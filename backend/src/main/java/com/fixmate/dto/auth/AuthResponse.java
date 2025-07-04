package com.fixmate.dto.auth;

import com.fixmate.dto.user.UserSummaryDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummaryDto user
) {}
