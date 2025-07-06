package com.fixmate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.dto.notification.WebSocketEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub Subscriber for WebSocket Cluster Relay.
 * 
 * Listens to the Redis topic "fixmate:ws:events". When any backend instance publishes
 * an event, this listener consumes it and dispatches it to locally connected
 * WebSocket client sessions via SimpMessagingTemplate.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketSubscriber implements MessageListener {

    public static final String WEBSOCKET_TOPIC = "fixmate:ws:events";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            WebSocketEventDto event = objectMapper.readValue(json, WebSocketEventDto.class);

            if (event.getTargetType() == WebSocketEventDto.TargetType.USER) {
                // Private user message: /user/{email}/queue/...
                messagingTemplate.convertAndSendToUser(
                        event.getTargetUserEmail(),
                        event.getDestination(),
                        event.getPayload()
                );
                log.debug("Redis WS Relay: Dispatched private message to user {} on {}",
                        event.getTargetUserEmail(), event.getDestination());
            } else if (event.getTargetType() == WebSocketEventDto.TargetType.BROADCAST) {
                // Public broadcast message: e.g. /topic/announcements
                messagingTemplate.convertAndSend(
                        event.getDestination(),
                        event.getPayload()
                );
                log.debug("Redis WS Relay: Dispatched broadcast message to {}", event.getDestination());
            }
        } catch (Exception e) {
            log.error("Failed to process Redis WebSocket relay message: {}", e.getMessage(), e);
        }
    }
}
