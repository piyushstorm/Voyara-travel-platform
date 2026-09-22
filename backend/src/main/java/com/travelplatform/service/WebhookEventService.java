package com.travelplatform.service;

import com.travelplatform.entity.WebhookEvent;
import com.travelplatform.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WebhookEventService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventService.class);

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEventService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    /**
     * Check if a webhook event has already been processed.
     * Runs in its own read-only transaction to ensure committed data is visible.
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String eventId) {
        return webhookEventRepository.existsByEventId(eventId);
    }

    /**
     * Record a new webhook event with PROCESSING status.
     * Uses REQUIRES_NEW to commit immediately, ensuring duplicate detection
     * works across concurrent webhook deliveries.
     *
     * @return true if the event was recorded successfully, false if it already exists (duplicate)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordEvent(String eventId, String eventType, String payloadSummary) {
        try {
            webhookEventRepository.saveAndFlush(new WebhookEvent(eventId, eventType, payloadSummary, "PROCESSING"));
            logger.info("Webhook event recorded: eventId={}, type={}", eventId, eventType);
            return true;
        } catch (DataIntegrityViolationException e) {
            logger.info("Duplicate webhook event detected (concurrent): eventId={}", eventId);
            return false;
        }
    }

    /**
     * Mark a webhook event as successfully processed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(String eventId) {
        Optional<WebhookEvent> event = webhookEventRepository.findByEventId(eventId);
        event.ifPresent(we -> {
            we.setStatus("PROCESSED");
            webhookEventRepository.save(we);
        });
    }
}
