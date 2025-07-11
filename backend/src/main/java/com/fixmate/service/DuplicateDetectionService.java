package com.fixmate.service;

import com.fixmate.model.Complaint;
import com.fixmate.model.enums.Priority;
import com.fixmate.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final ComplaintRepository complaintRepository;

    public boolean checkAndGroup(Complaint newComplaint) {
        LocalDateTime sixHoursAgo = LocalDateTime.now().minusHours(6);

        List<Complaint> candidates = complaintRepository.findDuplicateCandidates(
                newComplaint.getCategory(),
                newComplaint.getLocationBlock(),
                newComplaint.getLocationFloor(),
                newComplaint.getRoomNumber(),
                sixHoursAgo
        );

        // Exclude the newly saved complaint itself so it never becomes a duplicate of itself
        List<Complaint> otherCandidates = candidates.stream()
                .filter(c -> c.getId() != null && !c.getId().equals(newComplaint.getId()))
                .toList();

        if (otherCandidates.isEmpty()) {
            return false;
        }

        Complaint parent = findRootComplaint(otherCandidates);

        newComplaint.setParentComplaint(parent);
        newComplaint.setStatus(parent.getStatus());
        newComplaint.setAssignedStaff(parent.getAssignedStaff());
        newComplaint.setSlaDeadline(parent.getSlaDeadline());
        newComplaint.setPriorityReason("Grouped under parent complaint #" + parent.getId());
        complaintRepository.save(newComplaint);

        parent.setUpvoteCount(parent.getUpvoteCount() + 1);
        upgradePriorityIfNeeded(parent);
        complaintRepository.save(parent);

        log.info("Complaint {} grouped under parent: {} (total upvotes: {})",
                newComplaint.getId(), parent.getId(), parent.getUpvoteCount());
        return true;
    }

    private Complaint findRootComplaint(List<Complaint> candidates) {
        return candidates.stream()
                .filter(c -> c.getParentComplaint() == null)
                .findFirst()
                .orElse(candidates.get(0));
    }

    private void upgradePriorityIfNeeded(Complaint parent) {
        int upvoteCount = parent.getUpvoteCount();

        if (upvoteCount >= 10 && parent.getPriority() != Priority.CRITICAL) {
            log.info("Upgrading complaint {} priority to CRITICAL (upvotes: {})",
                    parent.getId(), upvoteCount);
            parent.setPriority(Priority.CRITICAL);
            parent.setSlaDeadline(LocalDateTime.now().plusHours(Priority.CRITICAL.getSlaHours()));
        } else if (upvoteCount >= 5) {
            Priority currentPriority = parent.getPriority();
            Priority upgraded = upgradeByOneLevel(currentPriority);
            if (upgraded != currentPriority) {
                log.info("Upgrading complaint {} priority from {} to {} (upvotes: {})",
                        parent.getId(), currentPriority, upgraded, upvoteCount);
                parent.setPriority(upgraded);
                parent.setSlaDeadline(LocalDateTime.now().plusHours(upgraded.getSlaHours()));
            }
        }
    }

    private Priority upgradeByOneLevel(Priority current) {
        return switch (current) {
            case LOW -> Priority.MEDIUM;
            case MEDIUM -> Priority.HIGH;
            case HIGH -> Priority.CRITICAL;
            case CRITICAL -> Priority.CRITICAL;
        };
    }
}
