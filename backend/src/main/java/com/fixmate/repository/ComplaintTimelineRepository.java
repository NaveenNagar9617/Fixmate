package com.fixmate.repository;

import com.fixmate.model.ComplaintTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintTimelineRepository extends JpaRepository<ComplaintTimeline, UUID> {

    List<ComplaintTimeline> findByComplaintIdOrderByCreatedAtAsc(UUID complaintId);
}
