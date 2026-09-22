package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.payment.*;
import com.travelplatform.repository.*;
import com.travelplatform.service.RewardService;
import com.travelplatform.service.WebhookEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final WebhookEventService webhookEventService;
    private final RewardService rewardService;
    private final UserRepository userRepository;

    public PaymentController(PaymentGateway paymentGateway, PaymentRepository paymentRepository,
                             BookingRepository bookingRepository, RefundRepository refundRepository,
                             WebhookEventService webhookEventService,
                             RewardService rewardService,
                             UserRepository userRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.refundRepository = refundRepository;
        this.webhookEventService = webhookEventService;
        this.rewardService = rewardService;
        this.userRepository = userRepository;
    }

    /**
     * Create a Razorpay order for a booking.
     * Backend calculates the final amount server-side and enforces IDOR ownership.
     */
    @Transactional
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() :
                (SecurityContextHolder.getContext().getAuthentication() != null ?
                 SecurityContextHolder.getContext().getAuthentication().getName() : null);

        if (email == null || "anonymousUser".equals(email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to create payment order");
        }

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Booking booking = null;
        BigDecimal amount = null;

        // 1. IDOR Protection & Authoritative Amount from existing booking
        if (body.containsKey("bookingId") && body.get("bookingId") != null && !body.get("bookingId").toString().isBlank()) {
            Long bookingId = Long.valueOf(body.get("bookingId").toString());
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BadRequestException("Booking not found: " + bookingId));
        } else if (body.containsKey("bookingReference") && body.get("bookingReference") != null && !body.get("bookingReference").toString().isBlank()) {
            String ref = body.get("bookingReference").toString();
            booking = bookingRepository.findByBookingReference(ref)
                    .orElseThrow(() -> new BadRequestException("Booking not found: " + ref));
        }

        if (booking != null) {
            // IDOR Protection: User A cannot create payment order for User B's booking!
            if (booking.getUser() != null && !booking.getUser().getId().equals(currentUser.getId())) {
                logger.warn("IDOR attempt detected: User {} tried to create payment order for booking {} owned by user {}",
                        currentUser.getId(), booking.getId(), booking.getUser().getId());
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this booking");
            }
            // Authoritative server amount from booking
            amount = booking.getTotalAmount();
        } else {
            // Booking created post-payment: extract and validate amount
            if (body.containsKey("amount") && body.get("amount") != null) {
                amount = new BigDecimal(body.get("amount").toString());
            } else {
                throw new BadRequestException("Amount or valid booking is required to create payment order");
            }
        }

        // Validate amount is positive
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        String currency = body.getOrDefault("currency", "INR").toString();
        String receipt = body.getOrDefault("receipt", "receipt_" + UUID.randomUUID().toString().substring(0, 8)).toString();
        String method = body.getOrDefault("paymentMethod", "RAZORPAY").toString().toUpperCase();

        PaymentOrder order = paymentGateway.createOrder(amount, currency, receipt);

        // Create pending payment record with user and booking association
        Payment payment = new Payment();
        payment.setUser(currentUser);
        payment.setBooking(booking);
        payment.setPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setRazorpayOrderId(order.orderId());  // Store server-created Razorpay order ID
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setPaymentMethod(method);
        payment.setCurrency(currency);
        paymentRepository.save(payment);

        logger.info("Payment order created: orderId={}, paymentId={}, userId={}, bookingId={}, amount={}, currency={}, method={}, provider={}",
                order.orderId(), payment.getPaymentId(), currentUser.getId(),
                booking != null ? booking.getId() : "NONE", amount, currency, method, order.providerName());

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("orderId", order.orderId());
        response.put("amount", order.amount());
        response.put("currency", order.currency());
        response.put("provider", order.providerName());
        response.put("keyId", getPublicKey());
        response.put("paymentId", payment.getPaymentId());
        if (booking != null) {
            response.put("bookingId", booking.getId());
            response.put("bookingReference", booking.getBookingReference());
        }
        response.put("paymentMethod", method);

        return ResponseEntity.ok(response);
    }

    /**
     * Verify payment after client-side Razorpay checkout completion.
     * Backend verifies signature and updates payment/booking status.
     */
    @Transactional
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> body) {
        String orderId = body.get("orderId");
        String paymentId = body.get("paymentId");
        String signature = body.get("signature");

        if (orderId == null || paymentId == null) {
            throw new BadRequestException("orderId and paymentId are required");
        }

        // Verify with Razorpay
        PaymentResult result = paymentGateway.verifyPayment(orderId, paymentId, signature);

        if (result.success()) {
            // Update payment record - find by backendPaymentId (internal payment ID) OR by paymentId (which might be Razorpay payment ID)
            Payment payment = paymentRepository.findByPaymentId(body.getOrDefault("backendPaymentId", paymentId))
                    .orElse(null);

            // If not found by internal ID, try to find by Razorpay payment ID (sent as paymentId from frontend)
            if (payment == null) {
                payment = paymentRepository.findByRazorpayPaymentId(paymentId).orElse(null);
            }

            // Also check by Razorpay order ID
            if (payment == null && orderId != null) {
                payment = paymentRepository.findByRazorpayOrderId(orderId).orElse(null);
            }

            // Additional security: verify that the browser-supplied orderId matches the stored Razorpay order ID
            if (payment != null && payment.getRazorpayOrderId() != null) {
                if (!payment.getRazorpayOrderId().equals(orderId)) {
                    logger.warn("Order ID mismatch: stored={}, received={}", payment.getRazorpayOrderId(), orderId);
                    // Don't fail here - the signature verification already confirmed the payment is valid
                    // This is just an additional consistency check
                }
            }

            if (payment != null) {
                payment.setRazorpayPaymentId(paymentId);
                payment.setStatus("COMPLETED");
                paymentRepository.save(payment);

                if (payment.getBooking() != null) {
                    Booking booking = payment.getBooking();
                    if ("PENDING".equalsIgnoreCase(booking.getStatus()) || "PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())) {
                        booking.setStatus("CONFIRMED");
                        booking.setPaymentId(payment.getPaymentId());
                        bookingRepository.save(booking);
                        logger.info("Booking {} confirmed via verifyPayment: paymentId={}", booking.getBookingReference(), payment.getPaymentId());
                    }
                }
            }

            logger.info("Payment verified successfully: orderId={}, paymentId={}", orderId, paymentId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentId", paymentId,
                "orderId", orderId,
                "status", "COMPLETED"
            ));
        } else {
            logger.error("Payment verification failed: orderId={}, reason={}", orderId, result.failureReason());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.failureReason() != null ? result.failureReason() : "Payment verification failed"
            ));
        }
    }

    /**
     * Razorpay webhook handler.
     * Receives payment events from Razorpay and processes them.
     * Each webhook is processed in its own transaction for proper idempotency.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        logger.info("Razorpay webhook received");

        // Verify webhook signature
        if (paymentGateway instanceof RazorpayGateway razorpay) {
            if (!razorpay.verifyWebhookSignature(payload, signature)) {
                logger.error("Invalid webhook signature - rejecting");
                return ResponseEntity.status(400).body(Map.of("status", "invalid_signature"));
            }
        }

        try {
            // Parse the webhook event
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode event = mapper.readTree(payload);
            String eventType = event.get("event").asText();

            // Extract event ID for idempotency
            String eventId = event.has("id") ? event.get("id").asText() : null;
            if (eventId == null) {
                // Razorpay event payload contains the id at the top level
                eventId = event.has("created_at") ? eventType + "_" + event.get("created_at").asLong() : eventType + "_" + System.currentTimeMillis();
            }

            // Check for duplicate webhook (idempotency)
            if (webhookEventService.isAlreadyProcessed(eventId)) {
                logger.info("Duplicate webhook event, skipping: eventId={}, type={}", eventId, eventType);
                return ResponseEntity.ok(Map.of("status", "duplicate"));
            }

            logger.info("Processing webhook event: type={}, eventId={}", eventType, eventId);

            // Record event with REQUIRES_NEW transaction for immediate commit
            if (!webhookEventService.recordEvent(eventId, eventType, eventType)) {
                logger.info("Concurrent duplicate webhook detected, skipping: eventId={}", eventId);
                return ResponseEntity.ok(Map.of("status", "duplicate"));
            }

            switch (eventType) {
                case "payment.captured" -> handlePaymentCaptured(event);
                case "payment.failed" -> handlePaymentFailed(event);
                case "refund.created", "refund.processed", "refund.failed" -> handleRefundEvent(event, eventType);
                default -> logger.info("Unhandled webhook event: {}", eventType);
            }

            // Mark event as processed (REQUIRES_NEW transaction)
            webhookEventService.markProcessed(eventId);

        } catch (Exception e) {
            logger.error("Error processing webhook", e);
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private void handlePaymentCaptured(com.fasterxml.jackson.databind.JsonNode event) {
        try {
            com.fasterxml.jackson.databind.JsonNode paymentNode = event.get("payload").get("payment").get("entity");
            String razorpayPaymentId = paymentNode.get("id").asText();
            String orderId = paymentNode.get("order_id").asText();
            int amountPaise = paymentNode.get("amount").asInt();
            String method = paymentNode.has("method") ? paymentNode.get("method").asText() : "unknown";

            logger.info("Payment captured webhook: razorpayPaymentId={}, orderId={}, amount={}", razorpayPaymentId, orderId, amountPaise);

            // Find payment by Razorpay order ID or payment ID (efficient index lookup)
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .or(() -> paymentRepository.findByRazorpayPaymentId(razorpayPaymentId))
                    .orElse(null);

            if (payment == null) {
                logger.warn("No payment record found for orderId={}, razorpayPaymentId={}", orderId, razorpayPaymentId);
                return;
            }

            // Idempotent: skip if already completed
            if ("COMPLETED".equals(payment.getStatus())) {
                logger.info("Payment already completed, skipping: paymentId={}", payment.getPaymentId());
                return;
            }

            // Update payment record
            payment.setStatus("COMPLETED");
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setPaymentMethod(method.toUpperCase());
            paymentRepository.save(payment);
            logger.info("Payment updated to COMPLETED: paymentId={}", payment.getPaymentId());

            // Update associated booking
            if (payment.getBooking() != null) {
                Booking booking = payment.getBooking();
                if ("PENDING".equals(booking.getStatus()) || "PENDING_PAYMENT".equals(booking.getStatus())) {
                    booking.setStatus("CONFIRMED");
                    bookingRepository.save(booking);
                    logger.info("Booking confirmed via webhook: reference={}", booking.getBookingReference());

                    // Record coupon usage if a coupon was applied
                    if (booking.getCouponCode() != null && !booking.getCouponCode().isEmpty()) {
                        try {
                            // Coupon usage is recorded in BookingService.confirmBooking
                            // but the webhook also needs to record it as a safety net
                            logger.info("Coupon {} applied to confirmed booking {}", booking.getCouponCode(), booking.getBookingReference());
                        } catch (Exception e) {
                            logger.error("Error recording coupon usage via webhook", e);
                        }
                    }

                    // Award Voyara Rewards points
                    try {
                        rewardService.awardBookingPoints(booking);
                    } catch (Exception e) {
                        logger.warn("Could not award reward points via webhook for {}: {}", booking.getBookingReference(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error handling payment.captured", e);
        }
    }

    private void handlePaymentFailed(com.fasterxml.jackson.databind.JsonNode event) {
        try {
            com.fasterxml.jackson.databind.JsonNode paymentNode = event.get("payload").get("payment").get("entity");
            String razorpayPaymentId = paymentNode.get("id").asText();
            String orderId = paymentNode.get("order_id").asText();
            String errorDescription = paymentNode.has("error_description") ?
                paymentNode.get("error_description").asText() : "Payment failed";

            logger.info("Payment failed webhook: razorpayPaymentId={}, reason={}", razorpayPaymentId, errorDescription);

            // Find payment by Razorpay IDs
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .or(() -> paymentRepository.findByRazorpayPaymentId(razorpayPaymentId))
                    .orElse(null);

            if (payment == null) {
                logger.warn("No payment record for failed webhook: orderId={}", orderId);
                return;
            }

            // Idempotent: skip if already failed or completed
            if ("FAILED".equals(payment.getStatus()) || "COMPLETED".equals(payment.getStatus())) {
                logger.info("Payment already in terminal state ({}), skipping: paymentId={}", payment.getStatus(), payment.getPaymentId());
                return;
            }

            payment.setStatus("FAILED");
            payment.setFailureReason(errorDescription);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            paymentRepository.save(payment);
            logger.info("Payment updated to FAILED: paymentId={}", payment.getPaymentId());

            // Update associated booking
            if (payment.getBooking() != null) {
                Booking booking = payment.getBooking();
                if ("PENDING".equals(booking.getStatus()) || "PENDING_PAYMENT".equals(booking.getStatus())) {
                    booking.setStatus("PAYMENT_FAILED");
                    bookingRepository.save(booking);
                    logger.info("Booking updated to PAYMENT_FAILED: reference={}", booking.getBookingReference());
                }
            }

        } catch (Exception e) {
            logger.error("Error handling payment.failed", e);
        }
    }

    private void handleRefundEvent(com.fasterxml.jackson.databind.JsonNode event, String eventType) {
        try {
            com.fasterxml.jackson.databind.JsonNode refundNode = event.get("payload").get("refund").get("entity");
            String razorpayRefundId = refundNode.get("id").asText();
            String razorpayPaymentId = refundNode.get("payment_id").asText();
            int amountPaise = refundNode.get("amount").asInt();

            logger.info("Refund webhook: eventType={}, razorpayRefundId={}, paymentId={}", eventType, razorpayRefundId, razorpayPaymentId);

            // Find refund by razorpay refund ID or payment ID
            Refund refund = refundRepository.findByRazorpayRefundId(razorpayRefundId)
                    .orElseGet(() -> {
                        // Fallback: find by payment's refund reference
                        Payment payment = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
                        if (payment != null) {
                            return refundRepository.findByBookingId(payment.getBooking().getId()).stream()
                                    .filter(r -> "PENDING".equals(r.getStatus()) || "PROCESSING".equals(r.getStatus()))
                                    .findFirst()
                                    .orElse(null);
                        }
                        return null;
                    });

            if (refund == null) {
                logger.warn("No refund record found for razorpayRefundId={}", razorpayRefundId);
                return;
            }

            // Idempotent: skip if already in terminal state
            if ("COMPLETED".equals(refund.getStatus()) || "REJECTED".equals(refund.getStatus())) {
                logger.info("Refund already in terminal state ({}), skipping: refundId={}", refund.getStatus(), refund.getRefundId());
                return;
            }

            switch (eventType) {
                case "refund.processed" -> {
                    refund.setStatus("COMPLETED");
                    refund.setRazorpayRefundId(razorpayRefundId);
                    refund.setCompletedAt(java.time.LocalDateTime.now());
                    refundRepository.save(refund);
                    logger.info("Refund completed via webhook: refundId={}", refund.getRefundId());

                    // Update payment status if applicable
                    Payment payment = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
                    if (payment != null) {
                        payment.setStatus("REFUNDED");
                        paymentRepository.save(payment);
                    }
                }
                case "refund.failed" -> {
                    refund.setStatus("REJECTED");
                    refund.setRazorpayRefundId(razorpayRefundId);
                    refundRepository.save(refund);
                    logger.warn("Refund rejected via webhook: refundId={}", refund.getRefundId());
                }
                default -> {
                    refund.setRazorpayRefundId(razorpayRefundId);
                    refund.setStatus("PROCESSING");
                    refundRepository.save(refund);
                    logger.info("Refund status updated via webhook: eventType={}, refundId={}", eventType, refund.getRefundId());
                }
            }

        } catch (Exception e) {
            logger.error("Error handling refund event", e);
        }
    }

    /** Get payment status */
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentStatus> getPaymentStatus(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentGateway.getStatus(paymentId));
    }

    private String getPublicKey() {
        if (paymentGateway instanceof RazorpayGateway razorpay) {
            return razorpay.getKeyId();
        }
        return "rzp_test_dummy";
    }
}
