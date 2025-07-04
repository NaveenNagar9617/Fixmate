package com.fixmate.dto.user;

import com.fixmate.model.enums.Role;
import com.fixmate.model.enums.StaffCategory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String name,
        String email,
        Role role,
        String roomNumber,
        String block,
        String phone,
        String profilePhotoUrl,
        Boolean isActive,
        LocalDateTime createdAt,
        StaffCategory staffCategory,
        Boolean isOnDuty,
        LocalTime shiftStart,
        LocalTime shiftEnd,
        Double avgRating
) {}
