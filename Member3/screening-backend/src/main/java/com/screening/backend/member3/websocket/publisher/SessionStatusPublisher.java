package com.screening.backend.member3.websocket.publisher;

import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.websocket.dto.SessionStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes session status transitions to WebSocket clients.
 * Destination format: /topic/member3/session/{sessionId}/status
 */
@Component
public class SessionStatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(SessionStatusPublisher.class);
    private static final String STATUS_DESTINATION_TEMPLATE = "/topic/member3/session/%s/status";

    private final SimpMessagingTemplate messagingTemplate;

    public SessionStatusPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishStatus(UUID sessionId, SessionStatus status, Instant timestamp) {
        if (sessionId == null || status == null) {
            return;
        }
        String destination = String.format(STATUS_DESTINATION_TEMPLATE, sessionId);
        SessionStatusMessage message = new SessionStatusMessage(sessionId, status, timestamp);
        log.debug("Publishing status {} for session {} to {}", status, sessionId, destination);
        messagingTemplate.convertAndSend(destination, message);
    }
}
