package com.fixmate.repository;

import com.fixmate.model.User;
import com.fixmate.model.enums.Role;
import com.fixmate.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRoleAndIsActiveTrue(Role role);

    List<User> findAllByIsActiveTrue();

    @Query("SELECT u FROM User u JOIN StaffProfile sp ON sp.user = u " +
            "WHERE sp.category = :category AND u.isActive = true")
    List<User> findStaffByCategory(@Param("category") StaffCategory category);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("role") Role role);
}
