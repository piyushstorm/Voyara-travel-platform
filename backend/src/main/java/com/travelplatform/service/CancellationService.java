package com.travelplatform.service;

import com.travelplatform.dto.refund.RefundPreviewResponse;
import com.travelplatform.entity.*;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.payment.PaymentGateway;
import com.travelplatform.payment.RefundResult;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Production-quality cancellation and refund engine for Voyara.
 * - Evaluates applicable cancellation policies (24h reservation rule, service-specific tiers)
 * - Deterministic BigDecimal refund calculation
 * - Idempotency and concurrency safety against double-cancellation
 * - Refund lifecycle tracking and status updates
 * - Predefined reason validation and administrative analytics
 */
@Service
public class CancellationService {

    private static final Logger logger = LoggerFactory.getLogger(CancellationService.class);
    private static final String DEFAULT_TIMELINE = "Expected within 5–7 business days";

    private final BookingRepository bookingRepo;
    private final CancellationPolicyRepository policyRepo;
    private final RefundRepository refundRepo;
    private final FlightRepository flightRepo;
    private final RoomRepository roomRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    public CancellationService(BookingRepository bookingRepo,
                                CancellationPolicyRepository policyRepo,
                                RefundRepository refundRepo,
                                FlightRepository flightRepo,
                                RoomRepository roomRepo,
                                PaymentRepository paymentRepo,
                                PaymentGateway paymentGateway,
                                NotificationService notificationService) {
        this.bookingRepo = bookingRepo;
        this.policyRepo = policyRepo;
        this.refundRepo = refundRepo;
        this.flightRepo = flightRepo;
        this.roomRepo = roomRepo;
        this.paymentRepo = paymentRepo;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
    }

    /**
     * Preview authoritative refund calculation with full transparency
     */
    @Transactional(readOnly = true)
    public RefundPreviewResponse previewRefund(Booking booking) {
        BigDecimal originalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new RefundPreviewResponse(
                booking.getId(), booking.getBookingReference(), booking.getBookingType(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "Zero Amount Booking", "No payment made",
                DEFAULT_TIMELINE, LocalDateTime.now()
            );
        }

        // 1. Check 24-hour reservation rule (Cancellation within 24 hours of booking creation -> min 50% refund)
        boolean within24hOfReservation = booking.getCreatedAt() != null
                && !booking.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24));

        BigDecimal rule24hRefund = within24hOfReservation
                ? originalAmount.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. Evaluate service-specific travel time tiers
        LocalDateTime travelDateTime = getTravelDateTime(booking);
        long hoursUntilTravel = travelDateTime != null
                ? Math.max(0, ChronoUnit.HOURS.between(LocalDateTime.now(), travelDateTime))
                : 0;

        List<CancellationPolicy> policies = policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc(
                booking.getBookingType() != null ? booking.getBookingType() : "FLIGHT");

        BigDecimal travelPolicyRefund = BigDecimal.ZERO;
        BigDecimal travelPolicyFee = BigDecimal.ZERO;
        String matchedPolicyName = null;

        for (CancellationPolicy policy : policies) {
            if (hoursUntilTravel >= policy.getMinHoursBefore() &&
                hoursUntilTravel < (policy.getMaxHoursBefore() == 0 ? Long.MAX_VALUE : policy.getMaxHoursBefore())) {
                BigDecimal refundPct = policy.getRefundPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal grossRefund = originalAmount.multiply(refundPct);
                travelPolicyRefund = grossRefund.subtract(policy.getCancellationFee()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                travelPolicyFee = policy.getCancellationFee();
                matchedPolicyName = policy.getName() + " (" + policy.getRefundPercentage() + "% refund)";
                break;
            }
        }

        // 3. Determine best applicable policy (24h reservation rule vs travel tier)
        BigDecimal finalRefundAmount;
        BigDecimal finalFee;
        BigDecimal finalRefundPct;
        String policyApplied;
        String policyExplanation;

        if (within24hOfReservation && rule24hRefund.compareTo(travelPolicyRefund) > 0) {
            finalRefundAmount = rule24hRefund;
            finalFee = BigDecimal.ZERO;
            finalRefundPct = new BigDecimal("50.00");
            policyApplied = "24-Hour Reservation Guarantee (50% refund)";
            policyExplanation = "Booking cancelled within 24 hours of reservation. Eligible for 50% refund.";
        } else if (matchedPolicyName != null) {
            finalRefundAmount = travelPolicyRefund;
            finalFee = travelPolicyFee;
            finalRefundPct = originalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? finalRefundAmount.multiply(BigDecimal.valueOf(100)).divide(originalAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            policyApplied = matchedPolicyName;
            policyExplanation = "Standard " + booking.getBookingType() + " policy applied based on " + hoursUntilTravel + " hours before travel.";
        } else if (within24hOfReservation) {
            finalRefundAmount = rule24hRefund;
            finalFee = BigDecimal.ZERO;
            finalRefundPct = new BigDecimal("50.00");
            policyApplied = "24-Hour Reservation Guarantee (50% refund)";
            policyExplanation = "Booking cancelled within 24 hours of reservation. Eligible for 50% refund.";
        } else {
            finalRefundAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            finalFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            finalRefundPct = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            policyApplied = "No Refund Policy";
            policyExplanation = "Cancellation is outside the eligible refund window.";
        }

        BigDecimal nonRefundable = originalAmount.subtract(finalRefundAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new RefundPreviewResponse(
            booking.getId(),
            booking.getBookingReference(),
            booking.getBookingType(),
            originalAmount.setScale(2, RoundingMode.HALF_UP),
            finalRefundPct,
            finalRefundAmount,
            nonRefundable,
            finalFee.setScale(2, RoundingMode.HALF_UP),
            policyApplied,
            policyExplanation,
            DEFAULT_TIMELINE,
            LocalDateTime.now()
        );
    }

    /** Calculate refund amount based on cancellation policy (backward-compatible) */
    @Transactional(readOnly = true)
    public BigDecimal calculateRefundAmount(Booking booking) {
        return previewRefund(booking).getRefundAmount();
    }

    /** Get the applicable cancellation policy name for display (backward-compatible) */
    @Transactional(readOnly = true)
    public String getApplicablePolicyName(Booking booking) {
        RefundPreviewResponse preview = previewRefund(booking);
        return preview.getPolicyApplied();
    }

    /** Cancel a booking and process refund (overload without comment) */
    @Transactional
    public Refund cancelBooking(Booking booking, String reason) {
        return cancelBooking(booking, reason, null);
    }

    /** Cancel a booking with validated reason, optional comment, and concurrency safety */
    @Transactional
    public Refund cancelBooking(Booking booking, String reason, String comment) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Cancellation reason is required");
        }

        CancellationReason cancellationReason;
        try {
            cancellationReason = CancellationReason.fromString(reason);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid cancellation reason: " + reason);
        }

        // Thread-safe / idempotent check against double cancellation
        Object lock = booking.getId() != null ? booking.getId().toString().intern() : this;
        synchronized (lock) {
            if ("CANCELLED".equals(booking.getStatus())) {
                throw new BadRequestException("Booking is already cancelled");
            }
            if (!"CONFIRMED".equals(booking.getStatus()) &&
                !"PENDING".equals(booking.getStatus()) &&
                !"PENDING_PAYMENT".equals(booking.getStatus())) {
                throw new BadRequestException("Booking cannot be cancelled in current state: " + booking.getStatus());
            }

            List<Refund> existingRefunds = refundRepo.findByBookingId(booking.getId());
            if (!existingRefunds.isEmpty()) {
                logger.warn("Refund already exists for booking {}", booking.getBookingReference());
                return existingRefunds.get(0);
            }

            RefundPreviewResponse preview = previewRefund(booking);
            BigDecimal refundAmount = preview.getRefundAmount();
            BigDecimal originalAmount = preview.getOriginalAmount();
            BigDecimal refundPct = preview.getEligibleRefundPercentage();

            // Create refund record
            Refund refund = new Refund();
            refund.setBooking(booking);
            refund.setRefundId("REF-" + Booking.generateReference().replace("TP-", ""));
            refund.setRefundAmount(refundAmount);
            refund.setOriginalAmount(originalAmount);
            refund.setRefundPercentage(refundPct);
            refund.setCancellationReason(cancellationReason.getCode());
            refund.setCancellationComment(comment);
            refund.setCancellationPolicy(preview.getPolicyApplied());
            refund.setCurrency("INR");
            refund.setStatus("PENDING");
            // Set 2 minutes for demo scheduler processing; user UI shows expected 5-7 business days
            refund.setExpectedCompletionAt(LocalDateTime.now().plusMinutes(2));
            refund = refundRepo.save(refund);

            // Update booking
            booking.setStatus("CANCELLED");
            booking.setCancellationReason(cancellationReason.getCode());
            booking.setRefundAmount(refundAmount);
            bookingRepo.save(booking);

            // Restore capacity
            if ("FLIGHT".equalsIgnoreCase(booking.getBookingType()) && booking.getFlight() != null) {
                restoreFlightCapacity(booking);
            } else if ("HOTEL".equalsIgnoreCase(booking.getBookingType()) && booking.getRoom() != null) {
                restoreRoomCapacity(booking);
            }

            logger.info("Booking {} cancelled. Refund: {} ({}%) — {} [{}]",
                booking.getBookingReference(), refundAmount, refundPct, cancellationReason.getCode(), comment);

            return refund;
        }
    }

    /** Process refunds that are ready (called by scheduler) */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void processPendingRefunds() {
        List<Refund> pendingRefunds = refundRepo.findPendingRefunds();
        LocalDateTime now = LocalDateTime.now();

        for (Refund refund : pendingRefunds) {
            if ("PENDING".equals(refund.getStatus())) {
                // Move to PROCESSING
                refund.setStatus("PROCESSING");
                refund.setProcessedAt(now);
                refundRepo.save(refund);

                // Process via payment gateway
                if (refund.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
                    RefundResult result = paymentGateway.processRefund(
                        refund.getBooking().getPaymentId(),
                        refund.getRefundAmount(),
                        refund.getCancellationReason()
                    );

                    if (result.success()) {
                        refund.setExternalRefundId(result.refundId());
                        refund.setRazorpayRefundId(result.refundId());
                        logger.info("Refund {} processing initiated: externalId={}", refund.getRefundId(), result.refundId());
                    } else {
                        refund.setStatus("REJECTED");
                        refund.setRejectionReason(result.failureReason());
                        refund.setFailureReason(result.failureReason());
                        logger.error("Refund {} failed: {}", refund.getRefundId(), result.failureReason());
                    }
                } else {
                    // Zero refund — mark as completed immediately
                    refund.setStatus("COMPLETED");
                    refund.setCompletedAt(now);
                }
                refundRepo.save(refund);
            }

            if ("PROCESSING".equals(refund.getStatus()) && refund.getExpectedCompletionAt() != null
                && now.isAfter(refund.getExpectedCompletionAt())) {
                // Complete the refund
                refund.setStatus("COMPLETED");
                refund.setCompletedAt(now);
                refundRepo.save(refund);
                logger.info("Refund {} completed", refund.getRefundId());

                // Send refund processed notification
                try {
                    notificationService.notifyRefundProcessed(refund.getBooking(), refund);
                } catch (Exception e) {
                    logger.warn("Could not send refund notification: {}", e.getMessage());
                }
            }
        }
    }

    /** Get all refunds for a user */
    @Transactional(readOnly = true)
    public List<Refund> getUserRefunds(Long userId) {
        return refundRepo.findByUserId(userId);
    }

    /** Get refund for a booking */
    @Transactional(readOnly = true)
    public List<Refund> getBookingRefunds(Long bookingId) {
        return refundRepo.findByBookingId(bookingId);
    }

    /** Administrative cancellation reason analytics */
    @Transactional(readOnly = true)
    public Map<String, Object> getCancellationReasonAnalytics() {
        List<Booking> allCancelled = bookingRepo.findAll().stream()
            .filter(b -> "CANCELLED".equals(b.getStatus()))
            .toList();

        Map<String, Long> reasonCounts = new LinkedHashMap<>();
        for (CancellationReason r : CancellationReason.values()) {
            reasonCounts.put(r.getCode(), 0L);
        }
        for (Booking b : allCancelled) {
            String r = b.getCancellationReason();
            if (r != null && !r.isBlank()) {
                try {
                    CancellationReason cr = CancellationReason.fromString(r);
                    reasonCounts.merge(cr.getCode(), 1L, Long::sum);
                } catch (Exception e) {
                    reasonCounts.merge("OTHER", 1L, Long::sum);
                }
            }
        }

        List<Refund> allRefunds = refundRepo.findAll();
        BigDecimal totalRefunded = allRefunds.stream()
            .map(Refund::getRefundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long fullRefunds = allRefunds.stream()
            .filter(r -> r.getRefundPercentage().compareTo(new BigDecimal("99.99")) >= 0)
            .count();
        long noRefunds = allRefunds.stream()
            .filter(r -> r.getRefundAmount().compareTo(BigDecimal.ZERO) == 0)
            .count();
        long partialRefunds = allRefunds.size() - fullRefunds - noRefunds;

        Map<String, Long> statusCounts = new HashMap<>();
        for (Refund r : allRefunds) {
            statusCounts.merge(r.getStatus(), 1L, Long::sum);
        }

        return Map.of(
            "totalCancellations", allCancelled.size(),
            "totalRefunds", allRefunds.size(),
            "totalRefundedAmount", totalRefunded,
            "reasonDistribution", reasonCounts,
            "statusDistribution", statusCounts,
            "typeDistribution", Map.of(
                "FULL_REFUND", fullRefunds,
                "PARTIAL_REFUND", Math.max(0, partialRefunds),
                "NO_REFUND", noRefunds
            )
        );
    }

    private LocalDateTime getTravelDateTime(Booking booking) {
        if ("FLIGHT".equalsIgnoreCase(booking.getBookingType())) {
            return booking.getTravelDate();
        } else if ("HOTEL".equalsIgnoreCase(booking.getBookingType())) {
            return booking.getCheckInDate();
        }
        return booking.getTravelDate();
    }

    private void restoreFlightCapacity(Booking booking) {
        Flight flight = booking.getFlight();
        if (flight == null || booking.getCabinClass() == null) return;

        switch (booking.getCabinClass().toUpperCase()) {
            case "ECONOMY" -> flight.setBookedSeatsEconomy(
                Math.max(0, flight.getBookedSeatsEconomy() - booking.getPassengerCount()));
            case "PREMIUM_ECONOMY" -> flight.setBookedSeatsPremiumEconomy(
                Math.max(0, flight.getBookedSeatsPremiumEconomy() - booking.getPassengerCount()));
            case "BUSINESS" -> flight.setBookedSeatsBusiness(
                Math.max(0, flight.getBookedSeatsBusiness() - booking.getPassengerCount()));
            case "FIRST" -> flight.setBookedSeatsFirst(
                Math.max(0, flight.getBookedSeatsFirst() - booking.getPassengerCount()));
        }
        flightRepo.save(flight);
    }

    private void restoreRoomCapacity(Booking booking) {
        Room room = booking.getRoom();
        if (room != null) {
            room.setAvailableRooms(room.getAvailableRooms() + 1);
            roomRepo.save(room);
        }
    }
}
