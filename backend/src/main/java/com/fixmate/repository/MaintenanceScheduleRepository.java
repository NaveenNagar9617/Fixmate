package com.fixmate.repository;

import com.fixmate.model.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, UUID> {

    List<MaintenanceSchedule> findByNextDueLessThanEqualAndIsActiveTrue(LocalDate date);

    List<MaintenanceSchedule> findAllByOrderByNextDueAsc();
}
