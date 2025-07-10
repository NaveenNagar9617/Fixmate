package com.fixmate.service;

import com.fixmate.dto.auth.AuthResponse;
import com.fixmate.dto.auth.LoginRequest;
import com.fixmate.dto.auth.RegisterRequest;
import com.fixmate.dto.user.UserSummaryDto;
import com.fixmate.exception.DuplicateResourceException;
import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.mapper.UserMapper;
import com.fixmate.model.StaffProfile;
import com.fixmate.model.User;
import com.fixmate.model.enums.Role;
import com.fixmate.model.enums.StaffCategory;
import com.fixmate.repository.StaffProfileRepository;
import com.fixmate.repository.UserRepository;
import com.fixmate.security.JwtTokenProvider;
import com.fixmate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        storeRefreshToken(userPrincipal.getUser().getId().toString(), refreshToken);

        UserSummaryDto userDto = userMapper.toSummaryDto(userPrincipal.getUser());
        log.info("User logged in: {}", request.getEmail());
        return new AuthResponse(accessToken, refreshToken, userDto);
    }

    private static final java.util.Set<String> COMMON_PASSWORDS = java.util.Set.of(
            "password123", "1234567890", "123456789", "qwerty1234",
            "admin12345", "welcome123", "letmein123", "fixmate123", "iloveyou123"
    );

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 10) {
            throw new IllegalArgumentException("Password must be at least 10 characters long");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase().trim())) {
            throw new IllegalArgumentException("Password is too common. Please choose a more secure password");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswordStrength(request.getPassword());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Security Defense: Public self-registration is strictly forced to Role.STUDENT.
        // Administrative and Staff roles can only be created by authenticated administrators.
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .roomNumber(request.getRoomNumber())
                .block(request.getBlock())
                .phone(request.getPhone())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        storeRefreshToken(user.getId().toString(), refreshToken);

        UserSummaryDto userDto = userMapper.toSummaryDto(user);
        log.info("New student registered: {}", request.getEmail());
        return new AuthResponse(accessToken, refreshToken, userDto);
    }

    @Transactional
    public com.fixmate.dto.user.UserProfileDto createStaffUser(com.fixmate.dto.user.CreateStaffRequest request) {
        validatePasswordStrength(request.getPassword());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF)
                .phone(request.getPhone())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        StaffProfile staffProfile = StaffProfile.builder()
                .user(user)
                .category(request.getCategory())
                .isOnDuty(true)
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(17, 0))
                .avgRating(0.0)
                .build();
        staffProfileRepository.save(staffProfile);
        user.setStaffProfile(staffProfile);

        log.info("New staff provisioned by admin: {} ({})", request.getEmail(), request.getCategory());
        return userMapper.toProfileDto(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + user.getId().toString());
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh token not found or expired");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        storeRefreshToken(user.getId().toString(), newRefreshToken);

        UserSummaryDto userDto = userMapper.toSummaryDto(user);
        return new AuthResponse(newAccessToken, newRefreshToken, userDto);
    }

    public void logout(String userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("User logged out: {}", userId);
    }

    private void storeRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                REFRESH_TOKEN_TTL_DAYS,
                TimeUnit.DAYS
        );
    }
}
