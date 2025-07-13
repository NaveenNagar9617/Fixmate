package com.fixmate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j   // log print krne ke liye use hota hai 
public class NotificationWebSocketController {

    @MessageMapping("/notifications/subscribe")
    @SendToUser("/queue/notifications")
    public String handleSubscription(Principal principal) {
        if (principal != null) {
            log.info("WebSocket subscription from user: {}", principal.getName());
            return "Subscribed to notifications";
        }
        return "Subscription acknowledged";
    }
}
