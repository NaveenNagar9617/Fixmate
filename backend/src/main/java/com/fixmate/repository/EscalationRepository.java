package com.fixmate.repository;

import com.fixmate.model.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, UUID> {

    List<Escalation> findByComplaintIdOrderByEscalatedAtDesc(UUID complaintId);

    @Query("SELECT e FROM Escalation e WHERE e.escalatedTo.id = :userId AND e.acknowledged = false " +
            "ORDER BY e.escalatedAt DESC")
    List<Escalation> findUnacknowledgedByEscalatedTo(@Param("userId") UUID userId);

    @Query("SELECT e FROM Escalation e WHERE e.escalatedTo.id = :userId ORDER BY e.escalatedAt DESC")
    List<Escalation> findAllByEscalatedTo(@Param("userId") UUID userId);
}
