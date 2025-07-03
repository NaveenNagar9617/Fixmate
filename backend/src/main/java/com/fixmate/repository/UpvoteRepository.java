package com.fixmate.repository;

import com.fixmate.model.Upvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpvoteRepository extends JpaRepository<Upvote, UUID> {

    boolean existsByComplaintIdAndStudentId(UUID complaintId, UUID studentId);

    Optional<Upvote> findByComplaintIdAndStudentId(UUID complaintId, UUID studentId);

    long countByComplaintId(UUID complaintId);
}
