package com.fixmate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * Production-grade WebSocket Configuration for FixMate.
 * 
 * Configures:
 * 1. Dual STOMP endpoints: Native WebSocket (/ws) and SockJS fallback (/ws).
 * 2. Strict Cross-Origin (CORS) origin verification matching application configuration.
 * 3. Message broker destinations:
 *    - /topic: Broadcast destinations (Announcements, public updates)
 *    - /queue: Private user-targeted destinations (Complaints, SLA alerts)
 * 4. Channel Interceptor for handshake authentication and destination authorization.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory broker for broadcast (/topic) and user-private queues (/queue)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages sent from client -> server @MessageMapping handlers
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for private user messages: /user/{username}/queue/...
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // 1. Pure Native WebSocket endpoint (ws:// or wss://)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        // 2. SockJS Fallback endpoint (for browsers with restrictive proxies)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register authentication and destination authorization interceptor
        registration.interceptors(webSocketAuthInterceptor);
    }
}


