package com.fixmate.service;

import com.fixmate.dto.analytics.*;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;
    private final RatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long total = complaintRepository.count();
        if (total == 0) {
            return new DashboardStatsDto(0, 0, 0, 0.0, 0.0, 0.0, 0);
        }

        List<ComplaintStatus> openStatuses = List.of(
                ComplaintStatus.SUBMITTED, ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS, ComplaintStatus.REOPENED
        );
        long openCount = complaintRepository.countByStatusIn(openStatuses);

        List<ComplaintStatus> closedStatuses = List.of(
                ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED
        );
        long closedCount = complaintRepository.countByStatusIn(closedStatuses);

        long escalatedCount = complaintRepository.countByStatus(ComplaintStatus.ESCALATED);

        double slaBreachRate = (double) escalatedCount / total * 100;

        List<Object[]> resolutionTimes = complaintRepository.avgResolutionTimeByCategory();
        double avgResolutionHours = 0.0;
        if (!resolutionTimes.isEmpty()) {
            avgResolutionHours = resolutionTimes.stream()
                    .filter(row -> row[1] != null)
                    .mapToDouble(row -> ((Number) row[1]).doubleValue())
                    .average()
                    .orElse(0.0);
        }

        long reopenedComplaints = complaintRepository.countByReopenedCountGreaterThan(0);
        double reopenRate = (double) reopenedComplaints / total * 100;

        return new DashboardStatsDto(
                openCount,
                closedCount,
                escalatedCount,
                Math.round(slaBreachRate * 100.0) / 100.0,
                Math.round(avgResolutionHours * 100.0) / 100.0,
                Math.round(reopenRate * 100.0) / 100.0,
                total
        );
    }
    // for pie chart 
    @Transactional(readOnly = true)
    public List<CategoryBreakdownDto> getCategoryBreakdown() {
        List<Object[]> results = complaintRepository.countByCategory();
        long total = complaintRepository.count();
        if (total == 0) return Collections.emptyList();

        return results.stream()
                .map(row -> new CategoryBreakdownDto(
                        row[0].toString(),
                        ((Number) row[1]).longValue(),
                        Math.round(((Number) row[1]).doubleValue() / total * 100 * 100.0) / 100.0
                ))
                .collect(Collectors.toList());
    }
    // for bar graph 
    @Transactional(readOnly = true)
    public List<BlockStatsDto> getBlockStats() {
        List<Object[]> results = complaintRepository.blockStats();
        return results.stream()
                .map(row -> new BlockStatsDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 100.0) / 100.0 : 0.0
                ))
                .collect(Collectors.toList());
    }
    // performance sheet 
    @Transactional(readOnly = true)
    public List<StaffPerformanceDto> getStaffPerformance() {
        List<Object[]> results = complaintRepository.staffPerformanceStats();
        return results.stream()
                .map(row -> {
                    UUID staffId = (UUID) row[0];
                    String name = (String) row[1];
                    long assigned = ((Number) row[2]).longValue();
                    long resolved = row[3] != null ? ((Number) row[3]).longValue() : 0;
                    double avgHours = row[4] != null ? Math.round(((Number) row[4]).doubleValue() * 100.0) / 100.0 : 0.0;
                    long reopenCount = row[5] != null ? ((Number) row[5]).longValue() : 0;
                    double avgRating = ratingRepository.avgRatingByStaffId(staffId).orElse(0.0);

                    return new StaffPerformanceDto(
                            staffId, name, assigned, resolved,
                            avgHours, reopenCount,
                            Math.round(avgRating * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());
    }
    // line chart 
    @Transactional(readOnly = true)
    public List<TrendPointDto> getDailyTrend(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<Object[]> results = complaintRepository.dailyComplaintTrend(from);

        return results.stream()
                .map(row -> {
                    LocalDate date;
                    if (row[0] instanceof java.sql.Date sqlDate) {
                        date = sqlDate.toLocalDate();
                    } else {
                        date = ((java.util.Date) row[0]).toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    }
                    return new TrendPointDto(date, ((Number) row[1]).longValue());
                })
                .collect(Collectors.toList());
    }
    // java.util.date thread safe nhi hai or mutable hai or java.util.localdata theard safe hai  or immutable  hai 
    
    // ye method heatmap prr data show krne ke liye use kare gae hai linkedhash map block ko order me rakhta hai or treemap floors ko 
    @Transactional(readOnly = true)
    public Map<String, Map<Integer, Long>> getComplaintHeatmap() {
        List<Object[]> results = complaintRepository.complaintHeatmap();
        Map<String, Map<Integer, Long>> heatmap = new LinkedHashMap<>();

        for (Object[] row : results) {
            String block = (String) row[0];
            int floor = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();

            heatmap.computeIfAbsent(block, k -> new TreeMap<>())
                    .put(floor, count);
        }

        return heatmap;
    }
}
