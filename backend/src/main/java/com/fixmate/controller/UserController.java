package com.fixmate.controller;

import com.fixmate.common.ApiResponse;
import com.fixmate.dto.user.UpdateProfileRequest;
import com.fixmate.dto.user.UserProfileDto;
import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.mapper.UserMapper;
import com.fixmate.model.User;
import com.fixmate.repository.UserRepository;
import com.fixmate.security.UserPrincipal;
import com.fixmate.service.FileStorageService;
import com.fixmate.util.ImageValidationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final ImageValidationUtil imageValidationUtil;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUser().getId().toString()));
        return ResponseEntity.ok(ApiResponse.success(buildResolvedProfileDto(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUser().getId().toString()));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRoomNumber() != null) {
            user.setRoomNumber(request.getRoomNumber());
        }
        if (request.getBlock() != null) {
            user.setBlock(request.getBlock());
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(buildResolvedProfileDto(user), "Profile updated"));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadProfilePhoto(
            @RequestPart("photo") MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Multi-layer defense in depth image validation before storage
        imageValidationUtil.validateImage(photo);

        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUser().getId().toString()));

        String oldObjectKey = user.getProfilePhotoUrl();
        String newObjectKey = fileStorageService.uploadFile(photo, "profiles/" + user.getId());

        user.setProfilePhotoUrl(newObjectKey);
        userRepository.save(user);

        if (oldObjectKey != null && !oldObjectKey.equals(newObjectKey)) {
            fileStorageService.deleteFile(oldObjectKey);
        }

        String presignedUrl = fileStorageService.getPresignedUrl(newObjectKey);
        return ResponseEntity.ok(ApiResponse.success(presignedUrl, "Profile photo uploaded"));
    }

    private UserProfileDto buildResolvedProfileDto(User user) {
        UserProfileDto baseDto = userMapper.toProfileDto(user);
        return new UserProfileDto(
                baseDto.id(),
                baseDto.name(),
                baseDto.email(),
                baseDto.role(),
                baseDto.roomNumber(),
                baseDto.block(),
                baseDto.phone(),
                fileStorageService.resolvePhotoUrl(baseDto.profilePhotoUrl()),
                baseDto.isActive(),
                baseDto.createdAt(),
                baseDto.staffCategory(),
                baseDto.isOnDuty(),
                baseDto.shiftStart(),
                baseDto.shiftEnd(),
                baseDto.avgRating()
        );
    }
}
