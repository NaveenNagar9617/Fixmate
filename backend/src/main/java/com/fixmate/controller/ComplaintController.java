package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.common.PageResponse;
import com.fixmate.dto.comment.CommentDto;
import com.fixmate.dto.comment.CreateCommentRequest;
import com.fixmate.dto.complaint.*;
import com.fixmate.security.UserPrincipal;
import com.fixmate.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ComplaintDetailDto>> createComplaint(
            @Valid @RequestPart("complaint") CreateComplaintRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintDetailDto result = complaintService.createComplaint(request, principal.getUser(), photo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Complaint created successfully"));
    }
    // modal attribute kyuki agar muje koi filter ke sath query krne hai jese kewal electic ka data chahiey toh 
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ComplaintListDto>>> getComplaints(
            @ModelAttribute ComplaintFilterRequest filter,
            @AuthenticationPrincipal UserPrincipal principal) {
        PageResponse<ComplaintListDto> result = complaintService.getComplaints(filter, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintDetailDto>> getComplaintById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintDetailDto result = complaintService.getComplaintById(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    // PUT ka use tab karte hain jab poora ka poora data replace karna ho. Par yahan mujhe sirf complaint ka ek chota sa hissa (Status) update karna tha, baaki data (naam, photo) waise hi rakhna tha. Partial update ke liye hamesha PATCH use hota hai.
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ComplaintDetailDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintDetailDto result = complaintService.updateStatus(id, request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result, "Status updated successfully"));
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<ApiResponse<ComplaintDetailDto>> updatePriority(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriorityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintDetailDto result = complaintService.updatePriority(id, request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result, "Priority updated successfully"));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplaintDetailDto>> assignStaff(
            @PathVariable UUID id,
            @Valid @RequestBody AssignStaffRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintDetailDto result = complaintService.assignStaff(id, request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result, "Staff assigned successfully"));
    }

    @PostMapping(value = "/{id}/photo/after", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadAfterPhoto(
            @PathVariable UUID id,
            @RequestPart("photo") MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal principal) {
        String url = complaintService.uploadAfterPhoto(id, photo, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(url, "After photo uploaded"));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CommentDto result = complaintService.addComment(id, request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Comment added"));
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<ApiResponse<Void>> upvoteComplaint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        complaintService.upvoteComplaint(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Complaint upvoted"));
    }

    @DeleteMapping("/{id}/upvote")
    public ResponseEntity<ApiResponse<Void>> removeUpvote(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        complaintService.removeUpvote(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Upvote removed"));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<List<ComplaintDetailDto.TimelineDto>>> getTimeline(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ComplaintDetailDto.TimelineDto> result = complaintService.getTimeline(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
