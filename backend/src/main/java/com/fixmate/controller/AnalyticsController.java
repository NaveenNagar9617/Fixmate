package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.dto.analytics.*;
import com.fixmate.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardStats()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryBreakdownDto>>> getCategoryBreakdown() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getCategoryBreakdown()));
    }

    @GetMapping("/blocks")
    public ResponseEntity<ApiResponse<List<BlockStatsDto>>> getBlockStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getBlockStats()));
    }

    @GetMapping("/staff-performance")
    public ResponseEntity<ApiResponse<List<StaffPerformanceDto>>> getStaffPerformance() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStaffPerformance()));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<TrendPointDto>>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDailyTrend(days)));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<Map<String, Map<Integer, Long>>>> getHeatmap() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getComplaintHeatmap()));
    }
}
