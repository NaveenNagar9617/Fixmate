package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.dto.rating.CreateRatingRequest;
import com.fixmate.dto.rating.RatingDto;
import com.fixmate.security.UserPrincipal;
import com.fixmate.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/api/v1/complaints/{id}/rating")
    public ResponseEntity<ApiResponse<RatingDto>> createRating(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRatingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RatingDto result = ratingService.createRating(id, request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Rating submitted"));
    }

    @GetMapping("/api/v1/staff/{id}/ratings")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getStaffRatings(@PathVariable UUID id) {
        List<RatingDto> result = ratingService.getRatingsForStaff(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
