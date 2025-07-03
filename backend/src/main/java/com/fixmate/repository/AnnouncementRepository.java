package com.fixmate.repository;

import com.fixmate.model.Announcement;
import com.fixmate.model.enums.Audience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("SELECT a FROM Announcement a WHERE " +
            "(a.targetAudience = :audience OR a.targetAudience = com.fixmate.model.enums.Audience.ALL) " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
            "ORDER BY a.createdAt DESC")
    List<Announcement> findActiveByAudience(@Param("audience") Audience audience,
                                             @Param("now") LocalDateTime now);

    List<Announcement> findAllByOrderByCreatedAtDesc();
}
