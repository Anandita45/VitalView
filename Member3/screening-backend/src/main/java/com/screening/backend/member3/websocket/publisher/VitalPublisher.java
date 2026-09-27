package com.screening.backend.member3.websocket.publisher;

import com.screening.backend.member3.websocket.dto.VitalReadingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Publishes real-time vital readings to WebSocket clients.
 * Destination format: /topic/member3/session/{sessionId}/vitals
 */
@Component
public class VitalPublisher {

    private static final Logger log = LoggerFactory.getLogger(VitalPublisher.class);
    private static final String VITALS_DESTINATION_TEMPLATE = "/topic/member3/session/%s/vitals";

    private final SimpMessagingTemplate messagingTemplate;

    public VitalPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishVital(VitalReadingMessage message) {
        if (message == null || message.getSessionId() == null) {
            return;
        }
        String destination = String.format(VITALS_DESTINATION_TEMPLATE, message.getSessionId());
        log.trace("Publishing vital reading for session {} to {}", message.getSessionId(), destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void publishVital(UUID sessionId, Long timestamp, Double bpm,
                             Double respiration, List<Double> waveform, Double signalQuality) {
        VitalReadingMessage message = new VitalReadingMessage(
                sessionId, timestamp, bpm, respiration, waveform, signalQuality
        );
        publishVital(message);
    }
}
