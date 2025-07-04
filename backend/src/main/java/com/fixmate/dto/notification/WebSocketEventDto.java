package com.fixmate.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Universal Event DTO for Redis Pub/Sub WebSocket Message Relay.
 * 
 * When any backend node generates a real-time event (Notification, Board update, Announcement),
 * it wraps the event in this DTO and publishes it to the Redis channel.
 * All running backend nodes receive this event from Redis and deliver it to their
 * locally connected WebSocket client sessions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEventDto implements Serializable {

    public enum TargetType {
        USER,       // Private message to a specific user (routed via convertAndSendToUser)
        BROADCAST   // Public message to a topic (routed via convertAndSend to /topic/...)
    }

    private TargetType targetType;

    /**
     * Target user identifier (Email) when targetType is USER.
     */
    private String targetUserEmail;

    /**
     * STOMP destination path:
     * - For USER: e.g. "/queue/notifications"
     * - For BROADCAST: e.g. "/topic/announcements"
     */
    private String destination;

    /**
     * The actual notification payload.
     */
    private NotificationDto payload;
}
