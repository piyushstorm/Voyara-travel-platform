package com.travelplatform.service;

import com.travelplatform.entity.FlightStatus;
import com.travelplatform.entity.PriceHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Push flight status update to all subscribers of this flight */
    public void sendFlightStatusUpdate(FlightStatus status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", status.getId());
        payload.put("flightId", status.getFlight().getId());
        payload.put("flightNumber", status.getFlight().getFlightNumber());
        payload.put("status", status.getStatus());
        payload.put("delayMinutes", status.getDelayMinutes());
        payload.put("delayReason", status.getDelayReason());
        payload.put("scheduledDeparture", status.getScheduledDeparture());
        payload.put("estimatedDeparture", status.getEstimatedDeparture());
        payload.put("actualDeparture", status.getActualDeparture());
        payload.put("scheduledArrival", status.getScheduledArrival());
        payload.put("estimatedArrival", status.getEstimatedArrival());
        payload.put("actualArrival", status.getActualArrival());
        payload.put("boardingTime", status.getBoardingTime());
        payload.put("gate", status.getGate());
        payload.put("terminal", status.getTerminal());
        payload.put("message", message);
        payload.put("updatedAt", status.getUpdatedAt());
        payload.put("lastUpdated", status.getUpdatedAt());
        payload.put("originCode", status.getFlight().getOriginCode());
        payload.put("destinationCode", status.getFlight().getDestinationCode());
        payload.put("departureAirportCode", status.getFlight().getOriginCode());
        payload.put("arrivalAirportCode", status.getFlight().getDestinationCode());
        if (status.getFlight().getAirline() != null) {
            payload.put("airlineCode", status.getFlight().getAirline().getCode());
            payload.put("airlineName", status.getFlight().getAirline().getName());
        }

        String destination = "/topic/flight-status/" + status.getFlight().getId();
        messagingTemplate.convertAndSend(destination, payload);

        // Also send to dashboard topic for multi-flight tracking
        messagingTemplate.convertAndSend("/topic/flight-status/all", payload);

        logger.info("WebSocket flight status sent: {} → {}", status.getFlight().getFlightNumber(), status.getStatus());
    }

    /** Push price update to subscribers */
    public void sendPriceUpdate(String entityType, Long entityId, String cabinClass,
                                java.math.BigDecimal oldPrice, java.math.BigDecimal newPrice, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("entityType", entityType);
        payload.put("entityId", entityId);
        payload.put("cabinClass", cabinClass);
        payload.put("oldPrice", oldPrice);
        payload.put("newPrice", newPrice);
        payload.put("reason", reason);

        String destination = "/topic/price-updates/" + entityType + "/" + entityId;
        messagingTemplate.convertAndSend(destination, payload);

        logger.info("WebSocket price update sent: {} {} {} → {}", entityType, entityId, oldPrice, newPrice);
    }

    /** Push guardian alert to a specific user's topic */
    public void sendGuardianAlert(Long userId, Map<String, Object> alertPayload) {
        String destination = "/topic/guardian-alerts/" + userId;
        messagingTemplate.convertAndSend(destination, alertPayload);
        logger.info("WebSocket guardian alert sent to user {}: {}", userId, alertPayload.get("alertType"));
    }

    /** Push notification badge update (unread count) to a specific user */
    public void sendNotificationUpdate(Long userId, long unreadCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("unreadCount", unreadCount);
        payload.put("timestamp", java.time.LocalDateTime.now().toString());

        String destination = "/topic/notifications/" + userId;
        messagingTemplate.convertAndSend(destination, payload);
        logger.debug("WebSocket notification count sent to user {}: {}", userId, unreadCount);
    }
}
