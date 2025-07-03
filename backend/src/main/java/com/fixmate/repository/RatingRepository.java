package com.fixmate.repository;

import com.fixmate.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByComplaintId(UUID complaintId);

    List<Rating> findByStudentId(UUID studentId);

    @Query("SELECT AVG(r.stars) FROM Rating r JOIN r.complaint c WHERE c.assignedStaff.id = :staffId")
    Optional<Double> avgRatingByStaffId(@Param("staffId") UUID staffId);

    @Query("SELECT r FROM Rating r JOIN r.complaint c WHERE c.assignedStaff.id = :staffId " +
            "ORDER BY r.createdAt DESC")
    List<Rating> findRatingsForStaff(@Param("staffId") UUID staffId);

    boolean existsByComplaintId(UUID complaintId);
}
