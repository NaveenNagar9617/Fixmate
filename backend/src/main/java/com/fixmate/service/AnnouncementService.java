package com.fixmate.service;

import com.fixmate.dto.announcement.AnnouncementDto;
import com.fixmate.dto.announcement.CreateAnnouncementRequest;
import com.fixmate.dto.notification.NotificationDto;
import com.fixmate.exception.ResourceNotFoundException;

import com.fixmate.model.Announcement;
import com.fixmate.model.User;
import com.fixmate.model.enums.Audience;
import com.fixmate.model.enums.NotificationType;
import com.fixmate.model.enums.Role;
import com.fixmate.repository.AnnouncementRepository;
import com.fixmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    // create announcement 
    @Transactional
    public AnnouncementDto create(CreateAnnouncementRequest request, User admin) {
        Announcement announcement = Announcement.builder()
                .admin(admin)
                .title(request.getTitle())
                .content(request.getContent())
                .targetAudience(request.getTargetAudience())
                .expiresAt(request.getExpiresAt())
                .build();
        announcement = announcementRepository.save(announcement);
        log.info("Announcement created by {}: {}", admin.getEmail(), request.getTitle());

        // Broadcast to all connected clients via WebSocket topic /topic/announcements
        NotificationDto broadcastDto = new NotificationDto(
                announcement.getId(),
                NotificationType.ANNOUNCEMENT,
                "New Announcement: " + request.getTitle(),
                request.getContent(),
                false,
                null,
                LocalDateTime.now()
        );
        notificationService.broadcastAnnouncement(broadcastDto);

        return toDto(announcement);
    }

    // user ko uske  hisab se notification dikhe 
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getActiveForUser(User user) {
        Audience audience = switch (user.getRole()) {
            case STUDENT -> Audience.STUDENTS;
            case STAFF -> Audience.STAFF;
            case ADMIN -> Audience.ALL;
        };

        List<Announcement> announcements = announcementRepository.findActiveByAudience(
                audience, LocalDateTime.now());
        return announcements.stream().map(this::toDto).collect(Collectors.toList());
    }
    // sare complait ko lane ke liye use hota hai 
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }
    // delete krne ke liye ye use hote hai 
    @Transactional
    public void delete(UUID id, User admin) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id.toString()));
        announcementRepository.delete(announcement);
        log.info("Announcement {} deleted by {}", id, admin.getEmail());
    }

    private AnnouncementDto toDto(Announcement a) {
        return new AnnouncementDto(
                a.getId(),
                a.getAdmin().getName(),
                a.getTitle(),
                a.getContent(),
                a.getTargetAudience(),
                a.getExpiresAt(),
                a.getCreatedAt()
        );
    }
}
