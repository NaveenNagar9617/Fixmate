package com.fixmate.dto.user;

import com.fixmate.model.enums.Role;

import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String name,
        String email,
        Role role,
        String profilePhotoUrl
) {}
