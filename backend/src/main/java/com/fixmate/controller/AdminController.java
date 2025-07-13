package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.common.PageResponse;
import com.fixmate.dto.announcement.AnnouncementDto;
import com.fixmate.dto.announcement.CreateAnnouncementRequest;
import com.fixmate.dto.complaint.ComplaintFilterRequest;
import com.fixmate.dto.user.UserProfileDto;
import com.fixmate.mapper.UserMapper;
import com.fixmate.model.AuditLog;
import com.fixmate.model.StaffProfile;
import com.fixmate.model.User;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Role;
import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.repository.AuditLogRepository;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.repository.StaffProfileRepository;
import com.fixmate.repository.UserRepository;
import com.fixmate.security.UserPrincipal;
import com.fixmate.service.AnnouncementService;
import com.fixmate.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ComplaintRepository complaintRepository;
    private final AuditLogRepository auditLogRepository;
    private final AnnouncementService announcementService;
    private final ExportService exportService;
    private final UserMapper userMapper;
    private final com.fixmate.service.AssignmentService assignmentService;
    private final com.fixmate.service.AuthService authService;

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<UserProfileDto>> createStaff(
            @Valid @RequestBody com.fixmate.dto.user.CreateStaffRequest request) {
        UserProfileDto created = authService.createStaffUser(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Staff member created successfully"));
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllStaff() {
        List<User> staffUsers = userRepository.findAllByRoleAndIsActiveTrue(Role.STAFF);
        List<Map<String, Object>> result = staffUsers.stream().map(user -> {
            Map<String, Object> staffData = new HashMap<>();
            staffData.put("user", userMapper.toProfileDto(user));

            List<ComplaintStatus> activeStatuses = List.of(
                    ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS);
            long activeComplaints = complaintRepository.countByAssignedStaffIdAndStatusIn(
                    user.getId(), activeStatuses);
            staffData.put("activeComplaints", activeComplaints);

            long totalAssigned = complaintRepository.findByAssignedStaffId(
                    user.getId(), PageRequest.of(0, 1)).getTotalElements();
            staffData.put("totalAssigned", totalAssigned);

            return staffData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/staff/{id}/workload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStaffWorkload(@PathVariable UUID id) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id.toString()));

        Map<String, Object> workload = new HashMap<>();
        workload.put("user", userMapper.toProfileDto(staff));

        List<ComplaintStatus> activeStatuses = List.of(
                ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS);
        long active = complaintRepository.countByAssignedStaffIdAndStatusIn(id, activeStatuses);
        workload.put("activeComplaints", active);

        List<ComplaintStatus> resolvedStatuses = List.of(
                ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED);
        long resolved = complaintRepository.countByAssignedStaffIdAndStatusIn(id, resolvedStatuses);
        workload.put("resolvedComplaints", resolved);

        long total = complaintRepository.findByAssignedStaffId(id, PageRequest.of(0, 1)).getTotalElements();
        workload.put("totalAssigned", total);

        return ResponseEntity.ok(ApiResponse.success(workload));
    }

    @PutMapping("/staff/{id}/duty")
    public ResponseEntity<ApiResponse<Void>> toggleDuty(@PathVariable UUID id) {
        StaffProfile profile = staffProfileRepository.findByUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", id.toString()));
        profile.setIsOnDuty(!profile.getIsOnDuty());
        staffProfileRepository.save(profile);

        if (Boolean.TRUE.equals(profile.getIsOnDuty())) {
            assignmentService.dispatchPendingComplaints(profile.getCategory());
        }

        return ResponseEntity.ok(ApiResponse.success(null,
                "Staff duty status set to " + (profile.getIsOnDuty() ? "ON" : "OFF")));
    }

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getAllAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getAll()));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AnnouncementDto result = announcementService.create(request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result, "Announcement created"));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        announcementService.delete(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Announcement deleted"));
    }

    @GetMapping("/export/complaints")
    public ResponseEntity<byte[]> exportComplaints(@ModelAttribute ComplaintFilterRequest filter) {
        byte[] csvBytes = exportService.exportComplaintsCSV(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=complaints_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/audit-log")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> auditPage = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(auditPage)));
    }
}

// why used pagination not list Performance aur Memory optimization ke liye! Audit logs time ke sath hazaron-lakho mein badh sakte hain. Agar hum sara data ek sath List mein bhejenge, toh Database pe load aayega, RAM full ho sakti hai, aur Network bandwith bhi zyada use hogi jisse frontend slow ho jayega. Pagination se hum data ko chunks (tukdon) mein bhejte hain, jisse application fast rehti hai.