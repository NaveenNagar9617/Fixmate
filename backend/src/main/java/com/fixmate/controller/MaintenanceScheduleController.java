package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.model.MaintenanceSchedule;
import com.fixmate.model.User;
import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.repository.UserRepository;
import com.fixmate.security.UserPrincipal;
import com.fixmate.service.MaintenanceScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/maintenance-schedules")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService scheduleService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceSchedule>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceSchedule>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getById(id)));
    }
    // Map use karne ka fayda yeh hai ki yeh quick aur flexible hota hai. Par iska bada nuksan (drawback) yeh hai ki humein har field ko manually Type Cast karna padta hai (jaise (String), (Integer)) aur isme @Valid laga kar automatic validation nahi ho pati. Ek badhe enterprise project mein yahan Map ki jagah hamesha ek proper DTO class (jaise CreateMaintenanceRequest) banani chahiye. Ye code maine basic flexibility show karne ke liye likha tha.
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceSchedule>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .title((String) body.get("title"))
                .category(ComplaintCategory.valueOf((String) body.get("category")))
                .block((String) body.get("block"))
                .recurrenceDays((Integer) body.get("recurrenceDays"))
                .nextDue(body.get("nextDue") != null
                        ? LocalDate.parse((String) body.get("nextDue"))
                        : LocalDate.now().plusDays(((Integer) body.get("recurrenceDays"))))
                .isActive(body.get("isActive") != null ? (Boolean) body.get("isActive") : true)
                .build();

        if (body.get("assignedStaffId") != null) {
            UUID staffId = UUID.fromString((String) body.get("assignedStaffId"));
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId.toString()));
            schedule.setAssignedStaff(staff);
        }

        MaintenanceSchedule created = scheduleService.create(schedule, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Schedule created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceSchedule>> update(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        MaintenanceSchedule updated = MaintenanceSchedule.builder()
                .title((String) body.get("title"))
                .category(ComplaintCategory.valueOf((String) body.get("category")))
                .block((String) body.get("block"))
                .recurrenceDays((Integer) body.get("recurrenceDays"))
                .nextDue(LocalDate.parse((String) body.get("nextDue")))
                .isActive(body.get("isActive") != null ? (Boolean) body.get("isActive") : true)
                .build();

        if (body.get("assignedStaffId") != null) {
            UUID staffId = UUID.fromString((String) body.get("assignedStaffId"));
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId.toString()));
            updated.setAssignedStaff(staff);
        }

        MaintenanceSchedule result = scheduleService.update(id, updated, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result, "Schedule updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        scheduleService.delete(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Schedule deleted"));
    }
}
