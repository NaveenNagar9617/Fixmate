package com.fixmate.scheduler;

import com.fixmate.model.Complaint;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.NotificationType;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.service.EscalationService;
import com.fixmate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SLACheckScheduler {

    private final ComplaintRepository complaintRepository;
    private final EscalationService escalationService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void checkSLABreaches() {
        LocalDateTime now = LocalDateTime.now();

        List<ComplaintStatus> activeStatuses = List.of(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS
        );

        List<Complaint> breachedComplaints = complaintRepository.findBreachedComplaints(activeStatuses, now);

        if (!breachedComplaints.isEmpty()) {
            log.warn("Found {} SLA breaches to escalate", breachedComplaints.size());
        }

        int escalatedCount = 0;
        for (Complaint complaint : breachedComplaints) {
            try {
                escalationService.escalate(complaint);
                escalatedCount++;
            } catch (Exception e) {
                log.error("Failed to escalate complaint {}: {}", complaint.getId(), e.getMessage());
            }
        }

        if (escalatedCount > 0) {
            log.info("SLA check complete: {} complaints escalated", escalatedCount);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void checkUpcomingBreaches() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourFromNow = now.plusHours(1);

        List<ComplaintStatus> activeStatuses = List.of(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS
        );

        List<Complaint> upcomingBreaches = complaintRepository.findUpcomingBreaches(
                activeStatuses, now, oneHourFromNow);

        for (Complaint complaint : upcomingBreaches) {
            if (complaint.getAssignedStaff() != null) {
                notificationService.createAndSend(
                        complaint.getAssignedStaff().getId(),
                        NotificationType.SLA_WARNING,
                        "⏰ SLA Warning",
                        "Complaint \"" + complaint.getTitle() + "\" SLA deadline is approaching. " +
                                "Deadline: " + complaint.getSlaDeadline(),
                        complaint.getId()
                );
            }
        }

        if (!upcomingBreaches.isEmpty()) {
            log.info("SLA warning sent for {} complaints approaching deadline", upcomingBreaches.size());
        }
    }
}
