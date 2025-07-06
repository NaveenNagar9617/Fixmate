package com.fixmate.config;

import com.fixmate.security.JwtTokenProvider;
import com.fixmate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * Production-Grade WebSocket STOMP Channel Interceptor.
 *
 * Enforces:
 * 1. Mandatory JWT authentication on STOMP CONNECT.
 *    - Rejects invalid, missing, or expired tokens immediately with MessageDeliveryException.
 * 2. UserPrincipal binding to the STOMP session for user-scoped queue routing.
 * 3. Destination-level Authorization on STOMP SUBSCRIBE:
 *    - Blocks unauthenticated subscriptions.
 *    - Restricts admin-only broadcast destinations (/topic/admin/**).
 * 4. Sender authentication on STOMP SEND.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    /**
     * Authenticates the user on initial STOMP connection frame.
     * Throws MessageDeliveryException to immediately abort unauthenticated handshakes.
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket CONNECT rejected: Missing JWT token in STOMP headers");
            throw new MessageDeliveryException("Unauthorized: Missing JWT token in WebSocket headers");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket CONNECT rejected: Invalid or expired JWT token");
            throw new MessageDeliveryException("Unauthorized: Invalid or expired JWT token");
        }

        try {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtTokenProvider.isTokenValid(token, userDetails)) {
                log.warn("WebSocket CONNECT rejected: Token username does not match user details: {}", username);
                throw new MessageDeliveryException("Unauthorized: Token validation failed for user " + username);
            }

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                log.warn("WebSocket CONNECT rejected: User account is disabled or locked: {}", username);
                throw new MessageDeliveryException("Unauthorized: User account is inactive");
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
            accessor.setUser(auth);
            log.info("WebSocket STOMP CONNECT successfully authenticated for user: {}", username);
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("WebSocket CONNECT error during authentication: {}", e.getMessage());
            throw new MessageDeliveryException("Unauthorized: " + e.getMessage());
        }
    }

    /**
     * Authorizes subscription destinations based on the user's role and identity.
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();

        if (principal == null) {
            log.warn("WebSocket SUBSCRIBE rejected: User is not authenticated for destination: {}", destination);
            throw new MessageDeliveryException("Unauthorized: Authentication required to subscribe");
        }

        if (destination == null) {
            return;
        }

        // Restrict admin-only broadcast topics
        if (destination.startsWith("/topic/admin") || destination.startsWith("/topic/alerts/admin")) {
            if (!isAdminUser(principal)) {
                log.warn("WebSocket SUBSCRIBE forbidden: Non-admin user {} tried to subscribe to {}",
                        principal.getName(), destination);
                throw new MessageDeliveryException("Forbidden: Admin access required for destination " + destination);
            }
        }
    }

    /**
     * Verifies that the sender is authenticated before allowing message processing.
     */
    private void handleSend(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("WebSocket SEND rejected: Unauthenticated message sending to: {}", accessor.getDestination());
            throw new MessageDeliveryException("Unauthorized: Authentication required to send messages");
        }
    }

    /**
     * Extracts the JWT token from various standard STOMP header formats.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Check standard "Authorization: Bearer <token>"
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7).trim();
            }
            return authHeader.trim();
        }

        // 2. Check alternative "token" or "accessToken" headers
        String altToken = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(altToken)) {
            return altToken.trim();
        }

        String accessToken = accessor.getFirstNativeHeader("accessToken");
        if (StringUtils.hasText(accessToken)) {
            return accessToken.trim();
        }

        return null;
    }

    private boolean isAdminUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
            return authToken.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}

