package com.fixmate.service;

import com.fixmate.common.PageResponse;
import com.fixmate.dto.comment.CommentDto;
import com.fixmate.dto.comment.CreateCommentRequest;
import com.fixmate.dto.complaint.*;
import com.fixmate.exception.DuplicateResourceException;
import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.exception.UnauthorizedActionException;
import com.fixmate.mapper.CommentMapper;
import com.fixmate.mapper.ComplaintMapper;
import com.fixmate.model.*;
import com.fixmate.model.enums.*;
import com.fixmate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintPhotoRepository photoRepository;
    private final CommentRepository commentRepository;
    private final UpvoteRepository upvoteRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ComplaintMapper complaintMapper;
    private final CommentMapper commentMapper;
    private final ComplaintStateMachine stateMachine;
    private final FileStorageService fileStorageService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final AssignmentService assignmentService;
    private final ComplaintTimelineService timelineService;
    private final NotificationService notificationService;
    private final PriorityEvaluationService priorityEvaluationService;
    private final com.fixmate.util.ImageValidationUtil imageValidationUtil;

    public ComplaintDetailDto createComplaint(CreateComplaintRequest request, User student, MultipartFile photo) {
        // Validate photo upfront before persisting if present
        if (photo != null && !photo.isEmpty()) {
            imageValidationUtil.validateImage(photo);
        }

        // 1. Commit base complaint to PostgreSQL in an isolated transaction
        Complaint complaint = createComplaintEntity(request, student);

        // 2. Photo upload runs outside the complaint DB transaction
        if (photo != null && !photo.isEmpty()) {
            try {
                String objectKey = fileStorageService.uploadFile(photo, "complaints/" + complaint.getId());
                attachPhotoToComplaint(complaint.getId(), objectKey, PhotoType.BEFORE, student);
            } catch (Exception e) {
                log.error("Failed to upload photo for complaint {}. Complaint persisted successfully without photo.", complaint.getId(), e);
                logPhotoFailure(complaint.getId(), student, "Photo upload could not be completed. Complaint was created successfully.");
            }
        }

        complaint = complaintRepository.findById(complaint.getId()).orElse(complaint);
        return buildDetailDto(complaint, student);
    }

    @Transactional
    public Complaint createComplaintEntity(CreateComplaintRequest request, User student) {
        var evalResult = priorityEvaluationService.evaluate(
                request.getCategory(),
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getSafetyFlags(),
                request.getUrgencyLevel()
        );

        String prioritySource = evalResult.isEmergencyDetected()
                ? "SYSTEM_HAZARD"
                : (evalResult.getEvaluatedPriority() != evalResult.getRequestedPriority() ? "SYSTEM_GUARDRAIL" : "STUDENT");

        Complaint complaint = Complaint.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(evalResult.getEvaluatedPriority())
                .requestedPriority(evalResult.getRequestedPriority())
                .priorityReason(evalResult.getReason())
                .prioritySource(prioritySource)
                .priorityUpdatedAt(LocalDateTime.now())
                .status(ComplaintStatus.SUBMITTED)
                .locationBlock(request.getLocationBlock())
                .locationFloor(request.getLocationFloor())
                .roomNumber(request.getRoomNumber())
                .student(student)
                .reopenedCount(0)
                .upvoteCount(0)
                .build();

        complaint = complaintRepository.save(complaint);

        boolean grouped = duplicateDetectionService.checkAndGroup(complaint);

        if (!grouped) {
            assignmentService.autoAssign(complaint);
        }

        timelineService.logEvent(complaint, student, "Complaint submitted",
                null, ComplaintStatus.SUBMITTED,
                "New complaint created (Evaluated priority: " + evalResult.getEvaluatedPriority() +
                        (evalResult.getEvaluatedPriority() != evalResult.getRequestedPriority()
                                ? ", requested: " + evalResult.getRequestedPriority() : "") + ")");

        saveAuditLog(student, "CREATE", "COMPLAINT", complaint.getId(), null,
                "{\"title\":\"" + complaint.getTitle() + "\",\"priority\":\"" + complaint.getPriority() + "\"}");

        // Notify all admins about the new complaint
        NotificationType notifType = evalResult.isEmergencyDetected() ? NotificationType.ESCALATION : NotificationType.STATUS_CHANGED;
        String notifPrefix = evalResult.isEmergencyDetected() ? "🚨 CRITICAL EMERGENCY: " : "New Complaint Submitted: ";

        notificationService.notifyAdmins(
                notifType,
                notifPrefix + complaint.getTitle(),
                "Student " + student.getName() + " submitted: \"" + complaint.getTitle() +
                        "\" (Priority: " + complaint.getPriority() + ")",
                complaint.getId()
        );

        log.info("Complaint committed to DB: {} by student {} with evaluated priority {}",
                complaint.getId(), student.getEmail(), complaint.getPriority());

        return complaint;
    }

    @Transactional
    public void attachPhotoToComplaint(UUID complaintId, String objectKey, PhotoType photoType, User uploader) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId.toString()));

        ComplaintPhoto complaintPhoto = ComplaintPhoto.builder()
                .complaint(complaint)
                .photoUrl(objectKey)
                .photoType(photoType)
                .uploadedBy(uploader)
                .uploadedAt(LocalDateTime.now())
                .build();

        photoRepository.save(complaintPhoto);
        complaint.getPhotos().add(complaintPhoto);
    }

    @Transactional
    public void logPhotoFailure(UUID complaintId, User user, String userFriendlyMessage) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint != null) {
            timelineService.logEvent(complaint, user, "Photo upload skipped",
                    null, null, userFriendlyMessage);
        }
    }

    @Transactional(readOnly = true)
    public ComplaintDetailDto getComplaintById(UUID id, User requester) {
        Complaint complaint = findComplaintOrThrow(id);
        checkAccess(complaint, requester);
        return buildDetailDto(complaint, requester);
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintListDto> getComplaints(ComplaintFilterRequest filter, User requester) {
        String[] sortParts = filter.getSort().split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(sortDir, sortField));

        // Normalize search: add % wildcards so JPQL LIKE can use it directly
        // If blank, "%" matches all strings
        String search = filter.getSearch();
        if (search == null || search.isBlank()) {
            search = "%";
        } else {
            search = "%" + search.trim().toLowerCase() + "%";
        }

        Specification<Complaint> spec;
        
        if (requester.getRole() == Role.STUDENT) {
            spec = com.fixmate.repository.spec.ComplaintSpecification.withFilters(
                    requester.getId(), null, filter.getStatus(), filter.getCategory(), 
                    filter.getPriority(), null, null, null, null, null, filter.getSearch());
        } else if (requester.getRole() == Role.STAFF) {
            spec = com.fixmate.repository.spec.ComplaintSpecification.withFilters(
                    null, requester.getId(), filter.getStatus(), filter.getCategory(), 
                    filter.getPriority(), null, null, null, null, null, null);
        } else {
            spec = com.fixmate.repository.spec.ComplaintSpecification.withFilters(
                    null, null, filter.getStatus(), filter.getCategory(), 
                    filter.getPriority(), filter.getBlock(), filter.getFloor(), 
                    filter.getAssignedStaffId(), filter.getFromDate(), 
                    filter.getToDate(), filter.getSearch());
        }

        Page<Complaint> page = complaintRepository.findAll(spec, pageable);

        Page<ComplaintListDto> dtoPage = page.map(complaintMapper::toListDto);
        return PageResponse.from(page, dtoPage.getContent());
    }

    @Transactional
    public ComplaintDetailDto updateStatus(UUID id, UpdateStatusRequest request, User actor) {
        Complaint complaint = findComplaintOrThrow(id);
        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.getNewStatus();

        stateMachine.validate(oldStatus, newStatus);

        if (actor.getRole() == Role.STAFF) {
            if (complaint.getAssignedStaff() == null ||
                    !complaint.getAssignedStaff().getId().equals(actor.getId())) {
                throw new UnauthorizedActionException("Staff can only update their assigned complaints");
            }
        }

        if (actor.getRole() == Role.STUDENT) {
            if (!complaint.getStudent().getId().equals(actor.getId())) {
                throw new UnauthorizedActionException("Students can only update their own complaints");
            }
            if (newStatus != ComplaintStatus.CLOSED && newStatus != ComplaintStatus.REOPENED) {
                throw new UnauthorizedActionException("Students can only close or reopen complaints");
            }
        }

        if (newStatus == ComplaintStatus.RESOLVED) {
            boolean hasAfterPhoto = photoRepository.existsByComplaintIdAndPhotoType(id, PhotoType.AFTER);
            if (!hasAfterPhoto) {
                throw new IllegalArgumentException("After-photo is required before marking complaint as resolved");
            }
            complaint.setResolvedAt(LocalDateTime.now());
        }

        if (newStatus == ComplaintStatus.REOPENED) {
            complaint.setReopenedCount(complaint.getReopenedCount() + 1);
            complaint.setResolvedAt(null);
            complaint.setStatus(ComplaintStatus.REOPENED);
            complaintRepository.save(complaint);
            assignmentService.autoAssign(complaint);
            complaint = findComplaintOrThrow(id);
        } else {
            complaint.setStatus(newStatus);
            complaintRepository.save(complaint);
        }

        timelineService.logEvent(complaint, actor, "Status updated",
                oldStatus, newStatus, request.getNote());

        // Notify student if actor is not the student
        if (complaint.getStudent() != null && !complaint.getStudent().getId().equals(actor.getId())) {
            notificationService.createAndSend(
                    complaint.getStudent().getId(),
                    NotificationType.STATUS_CHANGED,
                    "Complaint Status Updated",
                    "Complaint \"" + complaint.getTitle() + "\" status changed to " + newStatus.name(),
                    complaint.getId()
            );
        }

        // Notify assigned staff if actor is not the assigned staff
        if (complaint.getAssignedStaff() != null && !complaint.getAssignedStaff().getId().equals(actor.getId())) {
            notificationService.createAndSend(
                    complaint.getAssignedStaff().getId(),
                    NotificationType.STATUS_CHANGED,
                    "Complaint Status Updated",
                    "Complaint \"" + complaint.getTitle() + "\" status changed to " + newStatus.name(),
                    complaint.getId()
            );
        }

        saveAuditLog(actor, "UPDATE_STATUS", "COMPLAINT", complaint.getId(),
                "{\"status\":\"" + oldStatus + "\"}",
                "{\"status\":\"" + newStatus + "\"}");

        log.info("Complaint {} status updated: {} → {} by {}", id, oldStatus, newStatus, actor.getEmail());
        return buildDetailDto(complaint, actor);
    }

    @Transactional
    public ComplaintDetailDto assignStaff(UUID id, AssignStaffRequest request, User admin) {
        Complaint complaint = findComplaintOrThrow(id);
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", request.getStaffId().toString()));

        if (staff.getRole() != Role.STAFF) {
            throw new IllegalArgumentException("User is not a staff member");
        }

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setAssignedStaff(staff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setSlaDeadline(LocalDateTime.now().plusHours(complaint.getPriority().getSlaHours()));
        complaintRepository.save(complaint);

        timelineService.logEvent(complaint, admin, "Staff manually assigned",
                oldStatus, ComplaintStatus.ASSIGNED, "Assigned to " + staff.getName());

        // 1. Notify the assigned staff
        notificationService.createAndSend(
                staff.getId(),
                NotificationType.COMPLAINT_ASSIGNED,
                "New Complaint Assigned",
                "You have been assigned: " + complaint.getTitle(),
                complaint.getId()
        );

        // 2. Notify the student so their view updates in real time
        if (complaint.getStudent() != null) {
            notificationService.createAndSend(
                    complaint.getStudent().getId(),
                    NotificationType.COMPLAINT_ASSIGNED,
                    "Staff Assigned",
                    "Your complaint \"" + complaint.getTitle() + "\" has been assigned to " + staff.getName(),
                    complaint.getId()
            );
        }

        saveAuditLog(admin, "ASSIGN_STAFF", "COMPLAINT", complaint.getId(), null,
                "{\"staffId\":\"" + staff.getId() + "\",\"staffName\":\"" + staff.getName() + "\"}");

        log.info("Complaint {} assigned to staff {} by admin {}", id, staff.getEmail(), admin.getEmail());
        return buildDetailDto(complaint, admin);
    }

    @Transactional
    public String uploadAfterPhoto(UUID id, MultipartFile file, User staff) {
        Complaint complaint = findComplaintOrThrow(id);

        if (complaint.getAssignedStaff() == null ||
                !complaint.getAssignedStaff().getId().equals(staff.getId())) {
            throw new UnauthorizedActionException("Only the assigned staff can upload after photos");
        }

        // 1. Multi-layer validation before storage operations
        imageValidationUtil.validateImage(file);

        // 2. Upload new photo to MinIO
        String newObjectKey = fileStorageService.uploadFile(file, "complaints/" + id + "/after");

        // 3. Update database reference
        var existingAfterPhotoOpt = photoRepository.findByComplaintIdAndPhotoType(id, PhotoType.AFTER);
        String oldObjectKeyToDelete = null;

        if (existingAfterPhotoOpt.isPresent()) {
            ComplaintPhoto existingPhoto = existingAfterPhotoOpt.get();
            oldObjectKeyToDelete = existingPhoto.getPhotoUrl();
            existingPhoto.setPhotoUrl(newObjectKey);
            existingPhoto.setUploadedAt(LocalDateTime.now());
            photoRepository.save(existingPhoto);
        } else {
            ComplaintPhoto photo = ComplaintPhoto.builder()
                    .complaint(complaint)
                    .photoUrl(newObjectKey)
                    .photoType(PhotoType.AFTER)
                    .uploadedBy(staff)
                    .uploadedAt(LocalDateTime.now())
                    .build();
            photoRepository.save(photo);
            complaint.getPhotos().add(photo);
        }

        timelineService.logEvent(complaint, staff, "After photo uploaded",
                null, null, "After-repair photo added");

        // 4. Best-effort deletion of old object (new object is already authoritative in DB)
        if (oldObjectKeyToDelete != null && !oldObjectKeyToDelete.equals(newObjectKey)) {
            fileStorageService.deleteFile(oldObjectKeyToDelete);
        }

        log.info("After photo uploaded for complaint {} by staff {}", id, staff.getEmail());
        return fileStorageService.getPresignedUrl(newObjectKey);
    }

    @Transactional
    public CommentDto addComment(UUID id, CreateCommentRequest request, User author) {
        Complaint complaint = findComplaintOrThrow(id);
        checkAccess(complaint, author);

        if (request.isInternal() && author.getRole() == Role.STUDENT) {
            throw new UnauthorizedActionException("Students cannot create internal comments");
        }

        Comment comment = Comment.builder()
                .complaint(complaint)
                .author(author)
                .content(request.getContent())
                .isInternal(request.isInternal())
                .build();
        comment = commentRepository.save(comment);

        UUID notifyUserId = null;
        if (author.getRole() == Role.STUDENT && complaint.getAssignedStaff() != null) {
            notifyUserId = complaint.getAssignedStaff().getId();
        } else if (author.getRole() != Role.STUDENT) {
            notifyUserId = complaint.getStudent().getId();
        }

        if (notifyUserId != null && !request.isInternal()) {
            notificationService.createAndSend(
                    notifyUserId,
                    NotificationType.NEW_COMMENT,
                    "New Comment",
                    author.getName() + " commented on \"" + complaint.getTitle() + "\"",
                    complaint.getId()
            );
        }

        return commentMapper.toDto(comment);
    }

    @Transactional
    public void upvoteComplaint(UUID id, User student) {
        Complaint complaint = findComplaintOrThrow(id);

        if (upvoteRepository.existsByComplaintIdAndStudentId(id, student.getId())) {
            throw new DuplicateResourceException("You have already upvoted this complaint");
        }

        Upvote upvote = Upvote.builder()
                .complaint(complaint)
                .student(student)
                .build();
        upvoteRepository.save(upvote);

        complaint.setUpvoteCount(complaint.getUpvoteCount() + 1);
        upgradePriorityIfNeeded(complaint);
        complaintRepository.save(complaint);

        log.info("Complaint {} upvoted by student {} (total: {})", id, student.getEmail(), complaint.getUpvoteCount());
    }

    @Transactional
    public void removeUpvote(UUID id, User student) {
        Upvote upvote = upvoteRepository.findByComplaintIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Upvote", id.toString()));

        Complaint complaint = findComplaintOrThrow(id);
        upvoteRepository.delete(upvote);
        complaint.setUpvoteCount(Math.max(0, complaint.getUpvoteCount() - 1));
        complaintRepository.save(complaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintDetailDto.TimelineDto> getTimeline(UUID id, User requester) {
        Complaint complaint = findComplaintOrThrow(id);
        checkAccess(complaint, requester);
        List<ComplaintTimeline> timeline = timelineService.getTimeline(id);
        return timeline.stream().map(complaintMapper::toTimelineDto).toList();
    }

    private Complaint findComplaintOrThrow(UUID id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id.toString()));
    }

    private void checkAccess(Complaint complaint, User requester) {
        if (requester.getRole() == Role.ADMIN) {
            return;
        }
        if (requester.getRole() == Role.STUDENT &&
                !complaint.getStudent().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException("You do not have access to this complaint");
        }
        if (requester.getRole() == Role.STAFF &&
                (complaint.getAssignedStaff() == null ||
                        !complaint.getAssignedStaff().getId().equals(requester.getId()))) {
            throw new UnauthorizedActionException("You are not assigned to this complaint");
        }
    }

    @Transactional
    public ComplaintDetailDto updatePriority(UUID id, UpdatePriorityRequest request, User actor) {
        Complaint complaint = findComplaintOrThrow(id);

        if (actor.getRole() != Role.ADMIN) {
            if (actor.getRole() != Role.STAFF ||
                    complaint.getAssignedStaff() == null ||
                    !complaint.getAssignedStaff().getId().equals(actor.getId())) {
                throw new UnauthorizedActionException("Only Admins or assigned Staff can modify complaint priority");
            }
        }

        if (complaint.getStatus() == ComplaintStatus.CLOSED || complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new IllegalArgumentException("Cannot change priority of a " + complaint.getStatus() + " complaint");
        }

        Priority oldPriority = complaint.getPriority();
        Priority newPriority = request.getPriority();

        if (oldPriority != newPriority) {
            complaint.setPriority(newPriority);
            complaint.setPriorityReason(request.getReason());
            complaint.setPrioritySource(actor.getRole().name());
            complaint.setPriorityUpdatedAt(LocalDateTime.now());
            complaint.setSlaDeadline(LocalDateTime.now().plusHours(newPriority.getSlaHours()));
            complaintRepository.save(complaint);

            timelineService.logEvent(
                    complaint,
                    actor,
                    "Priority changed",
                    null,
                    null,
                    "Priority updated from " + oldPriority + " to " + newPriority +
                            ". Reason: " + request.getReason() + " (SLA reset to " + newPriority.getSlaHours() + "h)"
            );

            if (complaint.getStudent() != null && !complaint.getStudent().getId().equals(actor.getId())) {
                notificationService.createAndSend(
                        complaint.getStudent().getId(),
                        NotificationType.STATUS_CHANGED,
                        "Priority Updated",
                        "Complaint \"" + complaint.getTitle() + "\" priority was updated to " + newPriority.name() +
                                " by " + actor.getName() + " (" + request.getReason() + ")",
                        complaint.getId()
                );
            }

            if (complaint.getAssignedStaff() != null && !complaint.getAssignedStaff().getId().equals(actor.getId())) {
                notificationService.createAndSend(
                        complaint.getAssignedStaff().getId(),
                        NotificationType.STATUS_CHANGED,
                        "Priority Updated",
                        "Complaint \"" + complaint.getTitle() + "\" priority was updated to " + newPriority.name() +
                                " by " + actor.getName() + " (" + request.getReason() + ")",
                        complaint.getId()
                );
            }

            saveAuditLog(actor, "UPDATE_PRIORITY", "COMPLAINT", complaint.getId(),
                    "{\"priority\":\"" + oldPriority + "\"}",
                    "{\"priority\":\"" + newPriority + "\",\"reason\":\"" + request.getReason() + "\"}");

            log.info("Complaint {} priority changed from {} to {} by {} ({})",
                    id, oldPriority, newPriority, actor.getEmail(), actor.getRole());
        }

        return buildDetailDto(complaint, actor);
    }

    private ComplaintDetailDto buildDetailDto(Complaint complaint, User requester) {
        ComplaintDetailDto baseDto = complaintMapper.toDetailDto(complaint);

        List<Comment> comments;
        if (requester.getRole() == Role.STUDENT) {
            comments = commentRepository.findByComplaintIdAndIsInternalFalseOrderByCreatedAtAsc(complaint.getId());
        } else {
            comments = commentRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId());
        }
        List<CommentDto> commentDtos = commentMapper.toDtoList(comments);

        boolean hasRating = ratingRepository.existsByComplaintId(complaint.getId());
        boolean userHasUpvoted = false;
        if (requester.getRole() == Role.STUDENT) {
            userHasUpvoted = upvoteRepository.existsByComplaintIdAndStudentId(complaint.getId(), requester.getId());
        }

        // Dynamically resolve stored object keys to active 1-hour presigned URLs for the authorized user
        List<ComplaintDetailDto.PhotoDto> resolvedPhotos = baseDto.photos() == null ? List.of() :
                baseDto.photos().stream()
                        .map(p -> new ComplaintDetailDto.PhotoDto(
                                p.id(),
                                fileStorageService.resolvePhotoUrl(p.photoUrl()),
                                p.photoType(),
                                p.uploadedByName(),
                                p.uploadedAt()
                        ))
                        .toList();

        return new ComplaintDetailDto(
                baseDto.id(),
                baseDto.title(),
                baseDto.description(),
                baseDto.category(),
                baseDto.priority(),
                baseDto.requestedPriority(),
                baseDto.priorityReason(),
                baseDto.prioritySource(),
                baseDto.priorityUpdatedAt(),
                baseDto.status(),
                baseDto.locationBlock(),
                baseDto.locationFloor(),
                baseDto.roomNumber(),
                baseDto.student(),
                baseDto.assignedStaff(),
                baseDto.parentComplaintId(),
                baseDto.slaDeadline(),
                baseDto.slaStatus(),
                baseDto.resolvedAt(),
                baseDto.reopenedCount(),
                baseDto.upvoteCount(),
                baseDto.createdAt(),
                baseDto.updatedAt(),
                resolvedPhotos,
                baseDto.timeline(),
                commentDtos,
                hasRating,
                userHasUpvoted
        );
    }

    private void upgradePriorityIfNeeded(Complaint complaint) {
        int upvoteCount = complaint.getUpvoteCount();
        if (upvoteCount >= 10 && complaint.getPriority() != Priority.CRITICAL) {
            complaint.setPriority(Priority.CRITICAL);
            complaint.setSlaDeadline(LocalDateTime.now().plusHours(Priority.CRITICAL.getSlaHours()));
        } else if (upvoteCount >= 5) {
            Priority upgraded = switch (complaint.getPriority()) {
                case LOW -> Priority.MEDIUM;
                case MEDIUM -> Priority.HIGH;
                case HIGH, CRITICAL -> complaint.getPriority();
            };
            if (upgraded != complaint.getPriority()) {
                complaint.setPriority(upgraded);
                complaint.setSlaDeadline(LocalDateTime.now().plusHours(upgraded.getSlaHours()));
            }
        }
    }

    private void saveAuditLog(User user, String action, String entityType, UUID entityId,
                               String oldValue, String newValue) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(auditLog);
    }
}
