package com.fixmate.repository;

import com.fixmate.model.Complaint;
import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID>, JpaSpecificationExecutor<Complaint> {

    Page<Complaint> findByStudentId(UUID studentId, Pageable pageable);

    Page<Complaint> findByAssignedStaffId(UUID staffId, Pageable pageable);

    Page<Complaint> findByStatusIn(List<ComplaintStatus> statuses, Pageable pageable);

    List<Complaint> findByStatusAndCategoryAndAssignedStaffIsNullOrderByCreatedAtAsc(ComplaintStatus status, ComplaintCategory category);

    List<Complaint> findByStatusAndAssignedStaffIsNullOrderByCreatedAtAsc(ComplaintStatus status);

    List<Complaint> findByAssignedStaffIdAndStatusIn(UUID staffId, List<ComplaintStatus> statuses);

    List<Complaint> findByStatusAndSlaDeadlineBefore(ComplaintStatus status, LocalDateTime deadline);

    @Query("SELECT c FROM Complaint c WHERE c.status IN :statuses AND c.slaDeadline < :deadline")
    List<Complaint> findBreachedComplaints(@Param("statuses") List<ComplaintStatus> statuses,
                                           @Param("deadline") LocalDateTime deadline);

    @Query("SELECT c FROM Complaint c WHERE c.status IN :statuses " +
            "AND c.slaDeadline BETWEEN :from AND :to")
    List<Complaint> findUpcomingBreaches(@Param("statuses") List<ComplaintStatus> statuses,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query("SELECT c FROM Complaint c WHERE c.category = :category " +
            "AND c.locationBlock = :block AND c.locationFloor = :floor " +
            "AND c.roomNumber = :roomNumber " +
            "AND c.createdAt > :since AND c.status NOT IN " +
            "(com.fixmate.model.enums.ComplaintStatus.CLOSED, " +
            "com.fixmate.model.enums.ComplaintStatus.RESOLVED)")
    List<Complaint> findDuplicateCandidates(@Param("category") ComplaintCategory category,
                                            @Param("block") String block,
                                            @Param("floor") int floor,
                                            @Param("roomNumber") String roomNumber,
                                            @Param("since") LocalDateTime since);

    long countByStudentIdAndStatusIn(@Param("studentId") UUID studentId, 
                                     @Param("statuses") List<ComplaintStatus> statuses);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.assignedStaff.id = :staffId " +
            "AND c.status IN :statuses")
    long countByAssignedStaffIdAndStatusIn(@Param("staffId") UUID staffId,
                                           @Param("statuses") List<ComplaintStatus> statuses);

    // Analytics queries
    long countByStatus(ComplaintStatus status);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c.locationBlock, COUNT(c) FROM Complaint c GROUP BY c.locationBlock")
    List<Object[]> countByBlock();

    @Query("SELECT c.category, AVG((EXTRACT(EPOCH FROM c.resolvedAt) - EXTRACT(EPOCH FROM c.createdAt)) / 3600) " +
            "FROM Complaint c WHERE c.resolvedAt IS NOT NULL GROUP BY c.category")
    List<Object[]> avgResolutionTimeByCategory();

    @Query("SELECT c.locationBlock, COUNT(c), " +
            "AVG((EXTRACT(EPOCH FROM c.resolvedAt) - EXTRACT(EPOCH FROM c.createdAt)) / 3600) " +
            "FROM Complaint c GROUP BY c.locationBlock")
    List<Object[]> blockStats();

    @Query("SELECT CAST(c.createdAt AS date), COUNT(c) FROM Complaint c " +
            "WHERE c.createdAt >= :from GROUP BY CAST(c.createdAt AS date) " +
            "ORDER BY CAST(c.createdAt AS date)")
    List<Object[]> dailyComplaintTrend(@Param("from") LocalDateTime from);

    @Query("SELECT c.assignedStaff.id, c.assignedStaff.name, " +
            "COUNT(c), " +
            "SUM(CASE WHEN c.status = com.fixmate.model.enums.ComplaintStatus.RESOLVED " +
            "OR c.status = com.fixmate.model.enums.ComplaintStatus.CLOSED THEN 1 ELSE 0 END), " +
            "AVG(CASE WHEN c.resolvedAt IS NOT NULL THEN " +
            "(EXTRACT(EPOCH FROM c.resolvedAt) - EXTRACT(EPOCH FROM c.createdAt)) / 3600 ELSE NULL END), " +
            "SUM(c.reopenedCount) " +
            "FROM Complaint c WHERE c.assignedStaff IS NOT NULL " +
            "GROUP BY c.assignedStaff.id, c.assignedStaff.name")
    List<Object[]> staffPerformanceStats();

    @Query("SELECT c.locationBlock, c.locationFloor, COUNT(c) " +
            "FROM Complaint c GROUP BY c.locationBlock, c.locationFloor")
    List<Object[]> complaintHeatmap();

    long countByStatusIn(List<ComplaintStatus> statuses);

    long countByReopenedCountGreaterThan(int count);

    @Query("SELECT c FROM Complaint c WHERE c.locationBlock = :block " +
            "AND c.status NOT IN (com.fixmate.model.enums.ComplaintStatus.CLOSED, " +
            "com.fixmate.model.enums.ComplaintStatus.RESOLVED) " +
            "AND c.student.id != :studentId " +
            "ORDER BY c.createdAt DESC")
    List<Complaint> findNearbyIssues(@Param("block") String block,
                                     @Param("studentId") UUID studentId,
                                     Pageable pageable);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.student.id = :studentId AND c.status = :status")
    long countByStudentIdAndStatus(@Param("studentId") UUID studentId,
                                   @Param("status") ComplaintStatus status);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.student.id = :studentId " +
            "AND c.status = com.fixmate.model.enums.ComplaintStatus.CLOSED " +
            "AND NOT EXISTS (SELECT r FROM Rating r WHERE r.complaint = c)")
    long countPendingRatingsByStudentId(@Param("studentId") UUID studentId);
}
