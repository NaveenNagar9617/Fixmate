package com.fixmate.service;

import com.fixmate.model.Complaint;
import com.fixmate.model.StaffProfile;
import com.fixmate.model.enums.*;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final StaffProfileRepository staffProfileRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintTimelineService timelineService;
    private final NotificationService notificationService;

    private static final List<ComplaintStatus> ACTIVE_STATUSES = List.of(
            ComplaintStatus.ASSIGNED,
            ComplaintStatus.IN_PROGRESS
    );

    @Transactional
    public synchronized boolean autoAssign(Complaint complaint) {
        if (complaint == null) return false;

        StaffCategory specialistCategory = mapToStaffCategory(complaint.getCategory());
        List<StaffProfile> candidates = staffProfileRepository
                .findByCategoryAndIsOnDutyTrueAndUserIsActiveTrue(specialistCategory);

        // Fallback 1: On-duty General maintenance staff
        if (candidates.isEmpty() && specialistCategory != StaffCategory.GENERAL) {
            candidates = staffProfileRepository
                    .findByCategoryAndIsOnDutyTrueAndUserIsActiveTrue(StaffCategory.GENERAL);
        }

        // Fallback 2: Any on-duty active staff member
        if (candidates.isEmpty()) {
            candidates = staffProfileRepository.findByIsOnDutyTrueAndUserIsActiveTrue();
        }

        if (candidates.isEmpty()) {
            log.warn("No on-duty staff available for complaint {}. Category: {}",
                    complaint.getId(), complaint.getCategory());

            notificationService.notifyAdmins(
                    NotificationType.ESCALATION,
                    "Unassigned Complaint",
                    "No staff available for complaint: " + complaint.getTitle(),
                    complaint.getId()
            );
            return false;
        }

        // Select the optimal staff candidate using Weighted Burden Scoring
        StaffProfile selectedStaff = selectBestStaff(candidates);

        ComplaintStatus oldStatus = complaint.getStatus() != null ? complaint.getStatus() : ComplaintStatus.SUBMITTED;
        boolean isReopened = (oldStatus == ComplaintStatus.REOPENED);

        complaint.setAssignedStaff(selectedStaff.getUser());
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setSlaDeadline(LocalDateTime.now().plusHours(complaint.getPriority().getSlaHours()));

        selectedStaff.setLastAssignedAt(LocalDateTime.now());
        staffProfileRepository.save(selectedStaff);
        complaintRepository.save(complaint);

        String timelineAction = isReopened ? "Reassigned after reopening" : "Auto-assigned to " + selectedStaff.getUser().getName();
        String timelineNote = isReopened
                ? "Reassigned to " + selectedStaff.getUser().getName() + " (" + selectedStaff.getCategory() + " specialist)"
                : "Assigned to " + selectedStaff.getCategory() + " specialist (Burden score balanced)";

        timelineService.logEvent(
                complaint,
                complaint.getStudent(),
                timelineAction,
                oldStatus,
                ComplaintStatus.ASSIGNED,
                timelineNote
        );

        // Notify Assigned Staff
        notificationService.createAndSend(
                selectedStaff.getUser().getId(),
                NotificationType.COMPLAINT_ASSIGNED,
                "New Complaint Assigned",
                "You have been assigned: " + complaint.getTitle() + " (Priority: " + complaint.getPriority() + ")",
                complaint.getId()
        );

        // Notify Student
        notificationService.createAndSend(
                complaint.getStudent().getId(),
                NotificationType.COMPLAINT_ASSIGNED,
                "Complaint Assigned",
                "Your complaint \"" + complaint.getTitle() + "\" has been assigned to " +
                        selectedStaff.getUser().getName() + " (" + selectedStaff.getCategory().name()
                        .replace("_", " ").toLowerCase() + ")",
                complaint.getId()
        );

        log.info("Complaint {} auto-assigned to staff {} ({}) with priority {}",
                complaint.getId(), selectedStaff.getUser().getName(), selectedStaff.getCategory(), complaint.getPriority());

        return true;
    }

    public StaffProfile selectBestStaff(List<StaffProfile> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        return candidates.stream()
                .min(Comparator
                        .comparingDouble(this::calculateWeightedBurden)
                        .thenComparing(sp -> sp.getLastAssignedAt() != null ? sp.getLastAssignedAt() : LocalDateTime.MIN)
                        .thenComparing(Comparator.comparingDouble((StaffProfile sp) -> sp.getAvgRating() != null ? sp.getAvgRating() : 0.0).reversed())
                        .thenComparing(sp -> sp.getId().toString())
                )
                .orElse(candidates.get(0));
    }

    public double calculateWeightedBurden(StaffProfile staffProfile) {
        if (staffProfile == null || staffProfile.getUser() == null) return Double.MAX_VALUE;

        List<Complaint> activeComplaints = complaintRepository
                .findByAssignedStaffIdAndStatusIn(staffProfile.getUser().getId(), ACTIVE_STATUSES);

        double totalBurden = 0.0;
        for (Complaint c : activeComplaints) {
            Priority p = c.getPriority() != null ? c.getPriority() : Priority.LOW;
            totalBurden += switch (p) {
                case CRITICAL -> 3.0;
                case HIGH -> 2.0;
                case MEDIUM -> 1.0;
                case LOW -> 0.5;
            };
        }
        return totalBurden;
    }

    @Transactional
    public synchronized int dispatchPendingComplaints(StaffCategory category) {
        log.info("Dispatching pending complaints for category: {}", category);
        List<ComplaintCategory> mappedComplaintCategories = mapFromStaffCategory(category);

        List<Complaint> pending = new ArrayList<>();
        for (ComplaintCategory cc : mappedComplaintCategories) {
            pending.addAll(complaintRepository.findByStatusAndCategoryAndAssignedStaffIsNullOrderByCreatedAtAsc(
                    ComplaintStatus.SUBMITTED, cc));
        }

        int assignedCount = 0;
        for (Complaint c : pending) {
            if (autoAssign(c)) {
                assignedCount++;
            }
        }
        log.info("Dispatched {}/{} pending complaints for category {}", assignedCount, pending.size(), category);
        return assignedCount;
    }

    @Transactional
    public synchronized int dispatchAllPendingComplaints() {
        List<Complaint> pending = complaintRepository
                .findByStatusAndAssignedStaffIsNullOrderByCreatedAtAsc(ComplaintStatus.SUBMITTED);

        if (pending.isEmpty()) return 0;

        log.info("Found {} unassigned pending complaints. Attempting backlog auto-assignment...", pending.size());
        int assignedCount = 0;
        for (Complaint c : pending) {
            if (autoAssign(c)) {
                assignedCount++;
            }
        }
        log.info("Backlog auto-assignment complete: {}/{} complaints assigned", assignedCount, pending.size());
        return assignedCount;
    }

    public StaffCategory mapToStaffCategory(ComplaintCategory category) {
        if (category == null) return StaffCategory.GENERAL;
        return switch (category) {
            case ELECTRICAL -> StaffCategory.ELECTRICIAN;
            case PLUMBING -> StaffCategory.PLUMBER;
            case WIFI -> StaffCategory.IT_TECHNICIAN;
            case FURNITURE -> StaffCategory.CARPENTER;
            case CLEANING -> StaffCategory.CLEANER;
            case SECURITY -> StaffCategory.SECURITY;
            case PEST_CONTROL, OTHER -> StaffCategory.GENERAL;
        };
    }

    public List<ComplaintCategory> mapFromStaffCategory(StaffCategory staffCategory) {
        if (staffCategory == null) return List.of(ComplaintCategory.OTHER);
        return switch (staffCategory) {
            case ELECTRICIAN -> List.of(ComplaintCategory.ELECTRICAL);
            case PLUMBER -> List.of(ComplaintCategory.PLUMBING);
            case IT_TECHNICIAN -> List.of(ComplaintCategory.WIFI);
            case CARPENTER -> List.of(ComplaintCategory.FURNITURE);
            case CLEANER -> List.of(ComplaintCategory.CLEANING, ComplaintCategory.PEST_CONTROL);
            case SECURITY -> List.of(ComplaintCategory.SECURITY);
            case GENERAL -> List.of(ComplaintCategory.values());
        };
    }
}
