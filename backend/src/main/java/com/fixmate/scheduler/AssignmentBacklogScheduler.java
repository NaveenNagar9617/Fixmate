package com.fixmate.scheduler;

import com.fixmate.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentBacklogScheduler {

    private final AssignmentService assignmentService;

    /**
     * Periodic safety-net scheduler to sweep unassigned complaints in SUBMITTED status
     * and assign them to eligible staff who are currently on duty.
     * Runs every 10 minutes (600,000 ms).
     */
    @Scheduled(fixedRate = 600000)
    public void sweepBacklogComplaints() {
        try {
            int dispatched = assignmentService.dispatchAllPendingComplaints();
            if (dispatched > 0) {
                log.info("Backlog scheduler dispatched {} pending complaints", dispatched);
            }
        } catch (Exception e) {
            log.error("Error running backlog auto-assignment sweep: {}", e.getMessage(), e);
        }
    }
}
