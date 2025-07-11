package com.fixmate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.config.RedisWebSocketSubscriber;
import com.fixmate.dto.notification.NotificationDto;
import com.fixmate.dto.notification.WebSocketEventDto;
import com.fixmate.mapper.NotificationMapper;
import com.fixmate.model.Notification;
import com.fixmate.model.User;
import com.fixmate.model.enums.NotificationType;
import com.fixmate.model.enums.Role;
import com.fixmate.repository.NotificationRepository;
import com.fixmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createAndSend(UUID userId, NotificationType type, String title,
                               String message, UUID complaintId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot send notification: user {} not found", userId);
            return;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .complaintId(complaintId)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        NotificationDto dto = notificationMapper.toDto(notification);

        // 1. Deliver directly to locally connected WebSocket clients on this instance
        try {
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    dto
            );
            log.debug("Local WS notification sent to user {} ({})", userId, user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to dispatch local WS notification: {}", e.getMessage());
        }

        // 2. Publish to Redis Pub/Sub so all other instances in the cluster also deliver it
        try {
            WebSocketEventDto event = WebSocketEventDto.builder()
                    .targetType(WebSocketEventDto.TargetType.USER)
                    .targetUserEmail(user.getEmail())
                    .destination("/queue/notifications")
                    .payload(dto)
                    .build();

            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(RedisWebSocketSubscriber.WEBSOCKET_TOPIC, json);
            log.debug("Published notification event to Redis channel: {}", RedisWebSocketSubscriber.WEBSOCKET_TOPIC);
        } catch (Exception e) {
            log.warn("Failed to publish notification to Redis Pub/Sub: {}", e.getMessage());
        }
    }

    /**
     * Broadcasts a public announcement to all connected clients via /topic/announcements.
     */
    public void broadcastAnnouncement(NotificationDto dto) {
        // 1. Deliver locally
        try {
            messagingTemplate.convertAndSend("/topic/announcements", dto);
        } catch (Exception e) {
            log.warn("Failed to dispatch local announcement: {}", e.getMessage());
        }

        // 2. Relay via Redis Pub/Sub
        try {
            WebSocketEventDto event = WebSocketEventDto.builder()
                    .targetType(WebSocketEventDto.TargetType.BROADCAST)
                    .destination("/topic/announcements")
                    .payload(dto)
                    .build();
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(RedisWebSocketSubscriber.WEBSOCKET_TOPIC, json);
        } catch (Exception e) {
            log.warn("Failed to broadcast announcement to Redis: {}", e.getMessage());
        }
    }


    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message, UUID complaintId) {
        List<User> admins = userRepository.findAllByRoleAndIsActiveTrue(Role.ADMIN);
        for (User admin : admins) {
            createAndSend(admin.getId(), type, title, message, complaintId);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUser().getId().equals(userId)) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
