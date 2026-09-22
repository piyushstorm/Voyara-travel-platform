package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancellationServiceTest {

    @Mock private BookingRepository bookingRepo;
    @Mock private CancellationPolicyRepository policyRepo;
    @Mock private RefundRepository refundRepo;
    @Mock private FlightRepository flightRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private PaymentRepository paymentRepo;
    @Mock private com.travelplatform.payment.PaymentGateway paymentGateway;

    @InjectMocks
    private CancellationService cancellationService;

    private CancellationPolicy policy7Days;
    private CancellationPolicy policy3to7Days;
    private CancellationPolicy policy24to72Hours;
    private CancellationPolicy policyLess24Hours;

    @BeforeEach
    void setUp() {
        policy7Days = new CancellationPolicy("FLIGHT", "More than 7 days",
            168, 99999, BigDecimal.valueOf(100), BigDecimal.ZERO);
        policy3to7Days = new CancellationPolicy("FLIGHT", "3-7 days",
            72, 168, BigDecimal.valueOf(70), BigDecimal.valueOf(200));
        policy24to72Hours = new CancellationPolicy("FLIGHT", "24-72 hours",
            24, 72, BigDecimal.valueOf(50), BigDecimal.valueOf(500));
        policyLess24Hours = new CancellationPolicy("FLIGHT", "Less than 24 hours",
            0, 24, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private Booking createFlightBooking(int hoursBeforeDeparture) {
        Booking booking = new Booking();
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(BigDecimal.valueOf(10000));
        booking.setBookingType("FLIGHT");
        booking.setTravelDate(LocalDateTime.now().plusHours(hoursBeforeDeparture));
        return booking;
    }

    @Test
    @DisplayName("Full refund when cancelled more than 7 days before departure")
    void testFullRefund_Over7Days() {
        Booking booking = createFlightBooking(200); // ~8.3 days
        when(policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc("FLIGHT"))
            .thenReturn(List.of(policy7Days, policy3to7Days, policy24to72Hours, policyLess24Hours));

        BigDecimal refund = cancellationService.calculateRefundAmount(booking);

        // 100% of 10000 = 10000, minus 0 fee = 10000
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(refund));
    }

    @Test
    @DisplayName("70% refund when cancelled 3-7 days before departure")
    void testPartialRefund_3to7Days() {
        Booking booking = createFlightBooking(120); // 5 days
        when(policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc("FLIGHT"))
            .thenReturn(List.of(policy7Days, policy3to7Days, policy24to72Hours, policyLess24Hours));

        BigDecimal refund = cancellationService.calculateRefundAmount(booking);

        // 70% of 10000 = 7000, minus 200 fee = 6800
        assertEquals(0, BigDecimal.valueOf(6800).compareTo(refund));
    }

    @Test
    @DisplayName("50% refund when cancelled 24-72 hours before departure")
    void testPartialRefund_24to72Hours() {
        Booking booking = createFlightBooking(48); // 2 days
        when(policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc("FLIGHT"))
            .thenReturn(List.of(policy7Days, policy3to7Days, policy24to72Hours, policyLess24Hours));

        BigDecimal refund = cancellationService.calculateRefundAmount(booking);

        // 50% of 10000 = 5000, minus 500 fee = 4500
        assertEquals(0, BigDecimal.valueOf(4500).compareTo(refund));
    }

    @Test
    @DisplayName("No refund when cancelled less than 24 hours before departure")
    void testNoRefund_LessThan24Hours() {
        Booking booking = createFlightBooking(12); // 12 hours
        when(policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc("FLIGHT"))
            .thenReturn(List.of(policy7Days, policy3to7Days, policy24to72Hours, policyLess24Hours));

        BigDecimal refund = cancellationService.calculateRefundAmount(booking);

        assertEquals(0, BigDecimal.ZERO.compareTo(refund));
    }

    @Test
    @DisplayName("Refund never goes below zero even with high cancellation fee")
    void testRefundNeverNegative() {
        Booking booking = createFlightBooking(18); // 18 hours
        // Less than 24h policy gives 0% refund
        when(policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc("FLIGHT"))
            .thenReturn(List.of(policyLess24Hours));

        BigDecimal refund = cancellationService.calculateRefundAmount(booking);

        assertTrue(refund.compareTo(BigDecimal.ZERO) >= 0,
            "Refund should never be negative, got: " + refund);
    }

    @Test
    @DisplayName("Applicable policy name is returned correctly")
    void testGetApplicablePolicyName() {
        Booking booking = createFlightBooking(120); // 5 days
        when(policyRepo.findByEntityTypeOrderByMaxHoursBeforeDesc("FLIGHT"))
            .thenReturn(List.of(policy7Days, policy3to7Days, policy24to72Hours, policyLess24Hours));

        String policyName = cancellationService.getApplicablePolicyName(booking);

        assertTrue(policyName.contains("3-7 days"));
        assertTrue(policyName.contains("70%"));
    }
}
