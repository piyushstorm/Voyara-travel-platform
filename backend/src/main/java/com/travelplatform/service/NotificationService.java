package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized notification service.
 * Business services call notification methods; this service handles
 * in-app persistence, email delivery, and preference checking.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final EmailService emailService;
    private final UserRepository userRepo;
    private final WebSocketNotificationService wsNotificationService;

    public NotificationService(NotificationRepository notifRepo,
                               NotificationPreferenceRepository prefRepo,
                               EmailService emailService,
                               UserRepository userRepo,
                               WebSocketNotificationService wsNotificationService) {
        this.notifRepo = notifRepo;
        this.prefRepo = prefRepo;
        this.emailService = emailService;
        this.userRepo = userRepo;
        this.wsNotificationService = wsNotificationService;
    }

    // ─── Booking Notifications ──────────────────────────────

    @Transactional
    public void notifyBookingConfirmed(Booking booking) {
        if (booking == null || booking.getUser() == null) return;
        User user = booking.getUser();
        String title = "Booking Confirmed ✓";
        String message = "Your " + booking.getBookingType().toLowerCase() + " booking " + booking.getBookingReference() + " is confirmed."
            + (booking.getTotalAmount() != null ? " Amount: ₹" + booking.getTotalAmount().intValue() : "");

        createNotification(user, "BOOKING_CONFIRMED", title, message,
            "BOOKING", booking.getId(), booking.getBookingReference(), "BOTH");
        sendEmailIfNeeded(user, "booking-confirmation", "Booking Confirmed - " + booking.getBookingReference(),
            Map.of("userName", user.getName(), "bookingReference", booking.getBookingReference(),
                   "bookingType", booking.getBookingType(),
                   "amount", booking.getTotalAmount() != null ? "₹" + booking.getTotalAmount().intValue() : "N/A"));
    }

    @Transactional
    public void notifyBookingCancelled(Booking booking) {
        if (booking == null || booking.getUser() == null) return;
        User user = booking.getUser();
        String title = "Booking Cancelled";
        String message = "Your " + booking.getBookingType().toLowerCase() + " booking " + booking.getBookingReference() + " has been cancelled."
            + (booking.getRefundAmount() != null && booking.getRefundAmount().compareTo(BigDecimal.ZERO) > 0
                ? " Refund amount: ₹" + booking.getRefundAmount().intValue() : "");

        createNotification(user, "BOOKING_CANCELLED", title, message,
            "BOOKING", booking.getId(), booking.getBookingReference(), "BOTH");
        sendEmailIfNeeded(user, "cancellation-confirmation", "Booking Cancelled - " + booking.getBookingReference(),
            Map.of("userName", user.getName(), "bookingReference", booking.getBookingReference(),
                   "bookingType", booking.getBookingType(),
                   "refundAmount", booking.getRefundAmount() != null ? "₹" + booking.getRefundAmount().intValue() : "N/A",
                   "reason", booking.getCancellationReason() != null ? booking.getCancellationReason() : "Not specified"));
    }

    @Transactional
    public void notifyRefundProcessed(Booking booking, Refund refund) {
        if (booking == null || booking.getUser() == null || refund == null) return;
        User user = booking.getUser();
        String title = "Refund Processed ✓";
        String message = "Your refund of ₹" + refund.getRefundAmount().intValue() + " for booking " + booking.getBookingReference() + " has been processed.";

        createNotification(user, "REFUND_PROCESSED", title, message,
            "BOOKING", booking.getId(), booking.getBookingReference(), "BOTH");
        sendEmailIfNeeded(user, "refund-confirmation", "Refund Processed - " + booking.getBookingReference(),
            Map.of("userName", user.getName(), "bookingReference", booking.getBookingReference(),
                   "refundAmount", "₹" + refund.getRefundAmount().intValue()));
    }

    // ─── Payment Notifications ──────────────────────────────

    @Transactional
    public void notifyPaymentSuccessful(Booking booking, String paymentId) {
        if (booking == null || booking.getUser() == null) return;
        User user = booking.getUser();
        String title = "Payment Successful";
        String message = "Payment of ₹" + booking.getTotalAmount().intValue() + " for booking " + booking.getBookingReference() + " was successful.";

        createNotification(user, "PAYMENT_SUCCESSFUL", title, message,
            "BOOKING", booking.getId(), booking.getBookingReference(), "IN_APP");
        sendEmailIfNeeded(user, "payment-successful", "Payment Confirmed - " + booking.getBookingReference(),
            Map.of("userName", user.getName(), "bookingReference", booking.getBookingReference(),
                   "amount", "₹" + booking.getTotalAmount().intValue()));
    }

    // ─── Flight Status Notifications ────────────────────────

    @Transactional
    public void notifyFlightStatusChanged(User user, String flightNumber, String origin, String destination,
                                           String newStatus, Long flightId) {
        if (user == null) return;
        String title = "Flight Status Update: " + newStatus;
        String message = "Flight " + flightNumber + " (" + origin + " → " + destination + ") status: " + newStatus;
        String key = "u:" + user.getId() + ":f:" + flightId + ":STATUS:" + newStatus;

        createNotification(user, "FLIGHT_STATUS_CHANGE", title, message,
            "FLIGHT", flightId, flightNumber, "IN_APP", key);
        sendEmailIfNeeded(user, "flight-status-update", "Flight Status Update - " + flightNumber,
            Map.of("userName", user.getName(), "flightNumber", flightNumber,
                   "origin", origin, "destination", destination, "status", newStatus));
    }

    @Transactional
    public void notifyFlightDelayed(User user, String flightNumber, String origin, String destination,
                                     String delayInfo, Long flightId) {
        notifyFlightDelayed(user, flightNumber, origin, destination, delayInfo, flightId, 0, null);
    }

    @Transactional
    public void notifyFlightDelayed(User user, String flightNumber, String origin, String destination,
                                     String delayInfo, Long flightId, int delayMinutes, LocalDateTime newDeparture) {
        if (user == null) return;
        String title = "Flight Delayed ⚠️";
        String message = "Flight " + flightNumber + " (" + origin + " → " + destination + ") is delayed. " + delayInfo;
        String timeStr = newDeparture != null ? newDeparture.toString() : "0";
        String key = "u:" + user.getId() + ":f:" + flightId + ":DELAY:" + delayMinutes + ":" + timeStr;

        createNotification(user, "FLIGHT_DELAY", title, message,
            "FLIGHT", flightId, flightNumber, "BOTH", key);
        sendEmailIfNeeded(user, "flight-delay", "Flight Delay - " + flightNumber,
            Map.of("userName", user.getName(), "flightNumber", flightNumber,
                   "origin", origin, "destination", destination, "delayInfo", delayInfo));
    }

    @Transactional
    public void notifyDepartureTimeChanged(User user, String flightNumber, String origin, String destination,
                                           LocalDateTime oldDep, LocalDateTime newDep, Long flightId) {
        if (user == null) return;
        String title = "Departure Time Updated ⏱️";
        String message = "Flight " + flightNumber + " departure changed from " +
                (oldDep != null ? oldDep.format(DateTimeFormatter.ofPattern("HH:mm")) : "scheduled") +
                " to " + (newDep != null ? newDep.format(DateTimeFormatter.ofPattern("HH:mm")) : "revised") + ".";
        String key = "u:" + user.getId() + ":f:" + flightId + ":DEP:" + (newDep != null ? newDep.toString() : "0");

        createNotification(user, "FLIGHT_DEPARTURE_CHANGE", title, message,
            "FLIGHT", flightId, flightNumber, "IN_APP", key);
    }

    @Transactional
    public void notifyFlightEtaChanged(User user, String flightNumber, String origin, String destination,
                                       LocalDateTime oldEta, LocalDateTime newEta, Long flightId) {
        if (user == null) return;
        String title = "Estimated Arrival Updated 🛬";
        String message = "Flight " + flightNumber + " ETA updated to " +
                (newEta != null ? newEta.format(DateTimeFormatter.ofPattern("HH:mm")) : "revised schedule") + ".";
        String key = "u:" + user.getId() + ":f:" + flightId + ":ETA:" + (newEta != null ? newEta.toString() : "0");

        createNotification(user, "FLIGHT_ETA_CHANGE", title, message,
            "FLIGHT", flightId, flightNumber, "IN_APP", key);
    }

    @Transactional
    public void notifyBoardingStarted(User user, String flightNumber, String origin, String destination,
                                      String gate, String terminal, Long flightId) {
        if (user == null) return;
        String gateInfo = gate != null ? " at Gate " + gate : "";
        if (terminal != null) gateInfo += " (Terminal " + terminal + ")";
        String title = "Boarding Started ✈️";
        String message = "Boarding has begun for flight " + flightNumber + " (" + origin + " → " + destination + ")" + gateInfo + ".";
        String key = "u:" + user.getId() + ":f:" + flightId + ":BOARDING:" + (gate != null ? gate : "TBD");

        createNotification(user, "FLIGHT_BOARDING", title, message,
            "FLIGHT", flightId, flightNumber, "BOTH", key);
    }

    // ─── Account/Security Notifications ─────────────────────

    @Transactional
    public void notifyWelcome(User user) {
        if (user == null) return;
        String title = "Welcome to Voyara! 🎉";
        String message = "Welcome " + user.getName() + "! Your account has been created. Start exploring amazing travel deals.";

        createNotification(user, "WELCOME", title, message,
            "ACCOUNT", user.getId(), null, "BOTH");
        sendEmailIfNeeded(user, "welcome", "Welcome to Voyara!",
            Map.of("userName", user.getName()));
    }

    @Transactional
    public void notifyPasswordReset(User user) {
        if (user == null) return;
        String title = "Password Reset Request";
        String message = "A password reset was requested for your account. If this wasn't you, please contact support immediately.";

        createNotification(user, "PASSWORD_RESET", title, message,
            "ACCOUNT", user.getId(), null, "IN_APP");
        // Security notification - always send email regardless of preferences
        emailService.sendEmail(user.getEmail(), "password-reset", "Password Reset Request",
            Map.of("userName", user.getName()));
    }

    @Transactional
    public void notifyAccountSecurity(User user, String event) {
        if (user == null) return;
        String title = "Account Security Alert";
        String message = "Security event on your account: " + event;

        createNotification(user, "ACCOUNT_SECURITY", title, message,
            "ACCOUNT", user.getId(), null, "IN_APP");
        // Security notifications always sent
        emailService.sendEmail(user.getEmail(), "account-security", "Security Alert",
            Map.of("userName", user.getName(), "event", event));
    }

    @Transactional
    public void notifyPriceAlert(User user, String entityType, String entityName, String alertMessage, Long entityId) {
        if (user == null) return;
        String title = "Price Alert 💰";
        createNotification(user, "PRICE_ALERT", title, alertMessage,
            entityType.toUpperCase(), entityId, entityName, "IN_APP");
        sendEmailIfNeeded(user, "price-alert", "Price Alert - " + entityName,
            Map.of("userName", user.getName(), "alertMessage", alertMessage));
    }

    // ─── Core Notification Methods ──────────────────────────

    @Transactional
    public Notification createNotification(User user, String type, String title, String message,
                                            String relatedEntityType, Long relatedEntityId,
                                            String relatedEntityRef, String channel) {
        return createNotification(user, type, title, message, relatedEntityType, relatedEntityId, relatedEntityRef, channel, null);
    }

    @Transactional
    public Notification createNotification(User user, String type, String title, String message,
                                            String relatedEntityType, Long relatedEntityId,
                                            String relatedEntityRef, String channel, String customIdempotencyKey) {
        if (user == null) return null;

        // Idempotency check: user-scoped and state-scoped
        String idempotencyKey = (customIdempotencyKey != null && !customIdempotencyKey.isBlank())
                ? customIdempotencyKey
                : "u:" + user.getId() + ":" + type + ":" + (relatedEntityId != null ? relatedEntityId : "0");

        if (notifRepo.existsByIdempotencyKey(idempotencyKey)) {
            logger.debug("Duplicate notification skipped: key={}", idempotencyKey);
            return null;
        }

        // Check preferences for in-app
        NotificationPreference prefs = getOrCreatePreferences(user.getId());
        if (!shouldNotify(prefs, type, "IN_APP")) {
            // Still create email-only notification if needed
            if (!shouldNotify(prefs, type, "EMAIL")) return null;
        }

        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setRelatedEntityType(relatedEntityType);
        notif.setRelatedEntityId(relatedEntityId);
        notif.setRelatedEntityRef(relatedEntityRef);
        notif.setChannel(channel);
        notif.setDeliveryStatus("SENT");
        notif.setIdempotencyKey(idempotencyKey);
        Notification saved = notifRepo.save(notif);

        // Push real-time WebSocket unread count update
        try {
            long unreadCount = notifRepo.countUnreadByUserId(user.getId());
            wsNotificationService.sendNotificationUpdate(user.getId(), unreadCount);
        } catch (Exception e) {
            logger.debug("Failed to push WebSocket notification update: {}", e.getMessage());
        }

        return saved;
    }

    // ─── Customer API Methods ───────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, String type, Boolean unreadOnly, int page, int size) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notifRepo.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, PageRequest.of(page, size));
        }
        if (type != null && !type.isBlank()) {
            return notifRepo.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(page, size));
        }
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notifRepo.countUnreadByUserId(userId);
    }

    @Transactional
    public boolean markAsRead(Long userId, Long notificationId) {
        Optional<Notification> notifOpt = notifRepo.findById(notificationId);
        if (notifOpt.isEmpty()) return false;
        Notification notif = notifOpt.get();
        if (!notif.getUser().getId().equals(userId)) return false;
        if (notif.isRead()) return true;
        notif.setRead(true);
        notif.setReadAt(LocalDateTime.now());
        notifRepo.save(notif);
        return true;
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notifRepo.markAllAsRead(userId);
    }

    // ─── Preferences ────────────────────────────────────────

    @Transactional(readOnly = true)
    public NotificationPreference getOrCreatePreferences(Long userId) {
        return prefRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId).orElseThrow();
            return prefRepo.save(new NotificationPreference(user));
        });
    }

    @Transactional
    public NotificationPreference updatePreferences(Long userId, NotificationPreference updates) {
        NotificationPreference prefs = getOrCreatePreferences(userId);
        prefs.setEmailBookingUpdates(updates.isEmailBookingUpdates());
        prefs.setEmailPaymentUpdates(updates.isEmailPaymentUpdates());
        prefs.setEmailCancellationRefund(updates.isEmailCancellationRefund());
        prefs.setEmailFlightUpdates(updates.isEmailFlightUpdates());
        prefs.setEmailPriceAlerts(updates.isEmailPriceAlerts());
        prefs.setEmailMarketing(updates.isEmailMarketing());
        prefs.setInAppBookingUpdates(updates.isInAppBookingUpdates());
        prefs.setInAppPaymentUpdates(updates.isInAppPaymentUpdates());
        prefs.setInAppFlightUpdates(updates.isInAppFlightUpdates());
        prefs.setInAppPriceAlerts(updates.isInAppPriceAlerts());
        // Security notifications always on - never allow disabling
        prefs.setAlwaysSendSecurityNotifications(true);
        return prefRepo.save(prefs);
    }

    // ─── Helpers ────────────────────────────────────────────

    private void sendEmailIfNeeded(User user, String template, String subject, Map<String, String> variables) {
        try {
            NotificationPreference prefs = getOrCreatePreferences(user.getId());
            if (!shouldNotify(prefs, template, "EMAIL")) {
                // Security notifications always sent
                if (!"password-reset".equals(template) && !"account-security".equals(template)) {
                    return;
                }
            }
            emailService.sendEmail(user.getEmail(), template, subject, variables);
        } catch (Exception e) {
            logger.warn("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private boolean shouldNotify(NotificationPreference prefs, String type, String channel) {
        boolean isSecurity = type.startsWith("ACCOUNT_SECURITY") || type.equals("PASSWORD_RESET");
        if (isSecurity) return true; // Always send security notifications

        boolean isInApp = "IN_APP".equals(channel);
        boolean isEmail = "EMAIL".equals(channel);

        return switch (type) {
            case "BOOKING_CONFIRMED", "BOOKING_CANCELLED" -> isInApp ? prefs.isInAppBookingUpdates() : prefs.isEmailBookingUpdates();
            case "PAYMENT_SUCCESSFUL" -> isInApp ? prefs.isInAppPaymentUpdates() : prefs.isEmailPaymentUpdates();
            case "REFUND_PROCESSED" -> prefs.isEmailCancellationRefund();
            case "FLIGHT_STATUS_CHANGE", "FLIGHT_DELAY", "FLIGHT_DEPARTURE_CHANGE", "FLIGHT_ETA_CHANGE", "FLIGHT_BOARDING", "FLIGHT_CANCELLED" -> isInApp ? prefs.isInAppFlightUpdates() : prefs.isEmailFlightUpdates();
            case "PRICE_ALERT" -> isInApp ? prefs.isInAppPriceAlerts() : prefs.isEmailPriceAlerts();
            default -> true;
        };
    }
}
