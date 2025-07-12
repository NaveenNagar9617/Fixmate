package com.fixmate.service;

import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.model.Complaint;
import com.fixmate.model.MaintenanceSchedule;
import com.fixmate.model.User;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Priority;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.repository.MaintenanceScheduleRepository;
import com.fixmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @Service: Business logic file hai.
// @RequiredArgsConstructor: Dependencies ko constructor ke through inject karta hai.
// @Slf4j: Logs (history/debugging) print karne ke liye.
@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduleService {

    // Dependencies (Repositories) jo database se baat karti hain
    private final MaintenanceScheduleRepository scheduleRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    // Naya Maintenance Schedule create karne ke liye
    @Transactional
    public MaintenanceSchedule create(MaintenanceSchedule schedule, User admin) {
        schedule = scheduleRepository.save(schedule);
        log.info("Maintenance schedule created: {} by {}", schedule.getTitle(), admin.getEmail());
        return schedule;
    }

    // Saare schedules ko 'Next Due Date' ke basis par sort karke lane ke liye
    @Transactional(readOnly = true)
    public List<MaintenanceSchedule> getAll() {
        return scheduleRepository.findAllByOrderByNextDueAsc();
    }

    // ID se specific schedule dhoondhne ke liye
    @Transactional(readOnly = true)
    public MaintenanceSchedule getById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceSchedule", id.toString()));
    }

    // Purane schedule ko update karne ke liye
    @Transactional
    public MaintenanceSchedule update(UUID id, MaintenanceSchedule updated, User admin) {
        MaintenanceSchedule existing = getById(id);
        existing.setTitle(updated.getTitle());
        existing.setCategory(updated.getCategory());
        existing.setBlock(updated.getBlock());
        existing.setRecurrenceDays(updated.getRecurrenceDays());
        existing.setNextDue(updated.getNextDue());
        existing.setAssignedStaff(updated.getAssignedStaff());
        existing.setIsActive(updated.getIsActive());
        existing = scheduleRepository.save(existing);
        log.info("Maintenance schedule {} updated by {}", id, admin.getEmail());
        return existing;
    }

    // Schedule delete karne ke liye
    @Transactional
    public void delete(UUID id, User admin) {
        MaintenanceSchedule schedule = getById(id);
        scheduleRepository.delete(schedule);
        log.info("Maintenance schedule {} deleted by {}", id, admin.getEmail());
    }

    // [THE ROBOT ENGINE]: Ye method roz subah 8 baje apne aap chalega (Cron job)
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processDueSchedules() {
        // Aaj ki date nikal lo
        LocalDate today = LocalDate.now();
        // Database se wo saare schedules nikal lo jo aaj ya usse pehle karne the (aur wo active hon)
        List<MaintenanceSchedule> dueSchedules = scheduleRepository
                .findByNextDueLessThanEqualAndIsActiveTrue(today);

        // Agar aaj koi kaam nahi hai, toh wapas jao
        if (dueSchedules.isEmpty()) {
            return;
        }

        log.info("Processing {} due maintenance schedules", dueSchedules.size());

        // Har ek schedule ke liye loop chalao
        for (MaintenanceSchedule schedule : dueSchedules) {
            try {
                // Complaint create karne ke liye ek 'User' (Admin) chahiye, so use dhoondho
                User systemAdmin = userRepository.findAllByRoleAndIsActiveTrue(
                        com.fixmate.model.enums.Role.ADMIN).stream().findFirst().orElse(null);

                // Agar admin nahi mila, toh error log karke agle schedule par badho
                if (systemAdmin == null) {
                    log.error("No admin user found for maintenance schedule auto-creation");
                    continue;
                }

                // [FACTORY]: Bina user ke interaction ke nayi complaint banao
                Complaint complaint = Complaint.builder()
                        .title("[Scheduled] " + schedule.getTitle())
                        .description("Auto-generated maintenance task from schedule: " + schedule.getTitle() +
                                ". Block: " + schedule.getBlock() + ", Category: " + schedule.getCategory())
                        .category(schedule.getCategory())
                        .priority(Priority.MEDIUM)
                        .status(ComplaintStatus.SUBMITTED) // Initial status
                        .locationBlock(schedule.getBlock())
                        .locationFloor(1)
                        .roomNumber("N/A")
                        .student(systemAdmin) // Admin ko reporter banaya
                        .reopenedCount(0)
                        .upvoteCount(0)
                        .build();

                // Agar schedule me pehle se staff assign hai, toh complaint ko direct assign kar do
                if (schedule.getAssignedStaff() != null) {
                    complaint.setAssignedStaff(schedule.getAssignedStaff());
                    complaint.setStatus(ComplaintStatus.ASSIGNED);
                    complaint.setSlaDeadline(java.time.LocalDateTime.now().plusHours(Priority.MEDIUM.getSlaHours()));
                }

                // Complaint ko database me save karo
                complaintRepository.save(complaint);

                // [RESET BUTTON]: Agli service kab hogi? Aaj ki date + recurrence days
                schedule.setNextDue(today.plusDays(schedule.getRecurrenceDays()));
                scheduleRepository.save(schedule);

                log.info("Maintenance complaint auto-created for schedule '{}', next due: {}",
                        schedule.getTitle(), schedule.getNextDue());
            } catch (Exception e) {
                // Agar loop me kahin error aaye, toh sirf ye schedule fail hoga, baaki safe rahenge (Fault Isolation)
                log.error("Failed to process maintenance schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }
}