package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.model.Escalation;
import com.fixmate.security.UserPrincipal;
import com.fixmate.service.EscalationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EscalationController {

    private final EscalationService escalationService;
    // no of complait escalated 
    @GetMapping("/escalations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Escalation>>> getMyEscalations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Escalation> result = escalationService.getMyEscalations(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
     // unseen to ackonwledged 
    @PatchMapping("/escalations/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acknowledgeEscalation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        escalationService.acknowledgeEscalation(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Escalation acknowledged"));
    }
    // history of complaint 
    @GetMapping("/complaints/{id}/escalations")
    public ResponseEntity<ApiResponse<List<Escalation>>> getComplaintEscalations(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Escalation> result = escalationService.getEscalationsForComplaint(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
