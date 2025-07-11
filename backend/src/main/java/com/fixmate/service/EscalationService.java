package com.fixmate.service;

import com.fixmate.model.Complaint;
import com.fixmate.model.Escalation;
import com.fixmate.model.User;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.NotificationType;
import com.fixmate.model.enums.Priority;
import com.fixmate.model.enums.Role;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.repository.EscalationRepository;
import com.fixmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {

    private final EscalationRepository escalationRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintTimelineService timelineService;
    private final NotificationService notificationService;

    public User getEscalationTarget(Priority priority) {
        List<User> admins = userRepository.findAllByRoleAndIsActiveTrue(Role.ADMIN);
        if (admins.isEmpty()) {
            log.error("No active admin found for escalation!");
            return null;
        }
        return admins.get(0);
    }

    @Transactional
    public void escalate(Complaint complaint) {
        if (complaint.getStatus() == ComplaintStatus.ESCALATED) {
            return;
        }

        User target = getEscalationTarget(complaint.getPriority());
        if (target == null) {
            log.error("Cannot escalate complaint {}: no escalation target", complaint.getId());
            return;
        }

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaintRepository.save(complaint);

        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .escalatedTo(target)
                .reason("SLA breached: " + complaint.getPriority().getEscalationLabel() +
                        " - Deadline was " + complaint.getSlaDeadline())
                .escalatedAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
        escalationRepository.save(escalation);

        User systemUser = target;
        timelineService.logEvent(complaint, systemUser, "Auto-escalated: SLA breached",
                oldStatus, ComplaintStatus.ESCALATED,
                "SLA deadline exceeded. Priority: " + complaint.getPriority().name());

        notificationService.createAndSend(
                target.getId(),
                NotificationType.ESCALATION,
                "⚠️ Escalation: " + complaint.getTitle(),
                "SLA breached for " + complaint.getPriority().name() + " priority complaint. " +
                        "Block: " + complaint.getLocationBlock() + ", Room: " + complaint.getRoomNumber(),
                complaint.getId()
        );

        notificationService.createAndSend(
                complaint.getStudent().getId(),
                NotificationType.ESCALATION,
                "Complaint Escalated",
                "Your complaint \"" + complaint.getTitle() + "\" has been escalated due to SLA breach",
                complaint.getId()
        );

        if (complaint.getAssignedStaff() != null) {
            notificationService.createAndSend(
                    complaint.getAssignedStaff().getId(),
                    NotificationType.ESCALATION,
                    "Complaint Escalated",
                    "Complaint \"" + complaint.getTitle() + "\" assigned to you has been escalated",
                    complaint.getId()
            );
        }

        log.warn("Complaint {} escalated. Priority: {}, SLA deadline: {}",
                complaint.getId(), complaint.getPriority(), complaint.getSlaDeadline());
    }

    @Transactional
    public void acknowledgeEscalation(UUID escalationId, User admin) {
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new com.fixmate.exception.ResourceNotFoundException("Escalation", escalationId.toString()));

        escalation.setAcknowledged(true);
        escalation.setAcknowledgedAt(LocalDateTime.now());
        escalationRepository.save(escalation);

        log.info("Escalation {} acknowledged by admin {}", escalationId, admin.getEmail());
    }

    @Transactional(readOnly = true)
    public List<Escalation> getMyEscalations(UUID userId) {
        return escalationRepository.findAllByEscalatedTo(userId);
    }

    @Transactional(readOnly = true)
    public List<Escalation> getUnacknowledged(UUID userId) {
        return escalationRepository.findUnacknowledgedByEscalatedTo(userId);
    }

    @Transactional(readOnly = true)
    public List<Escalation> getEscalationsForComplaint(UUID complaintId, User requester) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new com.fixmate.exception.ResourceNotFoundException("Complaint", complaintId.toString()));

        if (requester.getRole() != Role.ADMIN) {
            if (requester.getRole() == Role.STUDENT &&
                    (complaint.getStudent() == null || !complaint.getStudent().getId().equals(requester.getId()))) {
                throw new com.fixmate.exception.UnauthorizedActionException("You do not have access to escalations for this complaint");
            }
            if (requester.getRole() == Role.STAFF &&
                    (complaint.getAssignedStaff() == null || !complaint.getAssignedStaff().getId().equals(requester.getId()))) {
                throw new com.fixmate.exception.UnauthorizedActionException("You are not assigned to this complaint");
            }
        }

        return escalationRepository.findByComplaintIdOrderByEscalatedAtDesc(complaintId);
    }
}
