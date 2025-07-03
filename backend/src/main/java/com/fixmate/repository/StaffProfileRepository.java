package com.fixmate.repository;

import com.fixmate.model.StaffProfile;
import com.fixmate.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {

    Optional<StaffProfile> findByUserId(UUID userId);

    List<StaffProfile> findByIsOnDutyTrue();

    List<StaffProfile> findByCategoryAndIsOnDutyTrueAndUserIsActiveTrue(StaffCategory category);

    List<StaffProfile> findByIsOnDutyTrueAndUserIsActiveTrue();

    List<StaffProfile> findByCategoryAndUserIsActiveTrue(StaffCategory category);

    List<StaffProfile> findByUserIsActiveTrue();
}
