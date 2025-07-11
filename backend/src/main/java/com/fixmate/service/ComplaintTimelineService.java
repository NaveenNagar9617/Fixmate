package com.fixmate.service;

import com.fixmate.model.Complaint;
import com.fixmate.model.ComplaintTimeline;
import com.fixmate.model.User;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.repository.ComplaintTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
// used to record the history of the update that happens in the complaint 
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintTimelineService {

    private final ComplaintTimelineRepository timelineRepository;

    @Transactional
    public ComplaintTimeline logEvent(Complaint complaint, User actor, String action,
                                      ComplaintStatus oldStatus, ComplaintStatus newStatus, String note) {
        ComplaintTimeline entry = ComplaintTimeline.builder()
                .complaint(complaint)
                .actor(actor)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .build();

        ComplaintTimeline saved = timelineRepository.save(entry);
        log.debug("Timeline logged for complaint {}: {} ({} → {})",
                complaint.getId(), action,
                oldStatus != null ? oldStatus.name() : "N/A",
                newStatus != null ? newStatus.name() : "N/A");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ComplaintTimeline> getTimeline(UUID complaintId) {
        return timelineRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
    }
}
