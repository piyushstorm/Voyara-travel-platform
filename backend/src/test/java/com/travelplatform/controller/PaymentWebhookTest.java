package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentWebhookTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User testUser;
    private Booking testBooking;
    private Payment testPayment;
    private String paymentId;
    private String userToken;

    @BeforeEach
    void setUp() {
        testUser = new User("Webhook Test", "webhook-test@test.com", passwordEncoder.encode("test123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);

        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        testBooking = new Booking();
        testBooking.setBookingReference("TP-WH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        testBooking.setUser(testUser);
        testBooking.setBookingType("FLIGHT");
        testBooking.setStatus("PENDING_PAYMENT");
        testBooking.setTotalAmount(new BigDecimal("5000.00"));
        testBooking.setOriginalAmount(new BigDecimal("5500.00"));
        testBooking.setDiscountAmount(new BigDecimal("500.00"));
        testBooking.setCouponCode("TEST10");
        testBooking = bookingRepository.save(testBooking);

        paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        testPayment = new Payment();
        testPayment.setPaymentId(paymentId);
        testPayment.setBooking(testBooking);
        testPayment.setAmount(new BigDecimal("5000.00"));
        testPayment.setCurrency("INR");
        testPayment.setPaymentMethod("RAZORPAY");
        testPayment.setStatus("PENDING");
        testPayment.setRazorpayOrderId("order_test_" + UUID.randomUUID().toString().substring(0, 8));
        testPayment = paymentRepository.save(testPayment);
    }

    // ─── Webhook signature tests ──────────────────────────────

    @Test
    void webhookReturns200ForInvalidSignatureWithMockGateway() throws Exception {
        // MockPaymentGateway does not verify signatures, so webhook accepts any signature
        // In production with RazorpayGateway, invalid signatures return 400
        String payload = "{\"event\":\"payment.captured\",\"id\":\"evt_test_1\"}";
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload)
                        .header("X-Razorpay-Signature", "invalid_signature"))
                .andExpect(status().isOk());
    }

    @Test
    void webhookAcceptsWithoutSignatureWithMockGateway() throws Exception {
        // MockPaymentGateway does not verify signatures
        String payload = "{\"event\":\"payment.captured\",\"id\":\"evt_test_2\"}";
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isOk());
    }

    // ─── Idempotency tests ────────────────────────────────────

    @Test
    void webhookEventIdempotencyTracked() {
        WebhookEvent event = new WebhookEvent("evt_idempotent_test", "payment.captured", "test event", "PROCESSED");
        webhookEventRepository.save(event);

        assert webhookEventRepository.existsByEventId("evt_idempotent_test");
        assert webhookEventRepository.findByEventId("evt_idempotent_test").isPresent();
        assert webhookEventRepository.findByEventId("evt_nonexistent").isEmpty();
    }

    @Test
    void webhookEventRepositoryHandlesMultipleEvents() {
        webhookEventRepository.save(new WebhookEvent("evt_100", "payment.captured", "test", "PROCESSED"));
        webhookEventRepository.save(new WebhookEvent("evt_101", "payment.failed", "test", "PROCESSED"));

        assert webhookEventRepository.existsByEventId("evt_100");
        assert webhookEventRepository.existsByEventId("evt_101");
        assert !webhookEventRepository.existsByEventId("evt_999");
    }

    // ─── Payment lookup tests ──────────────────────────────────

    @Test
    void paymentCanBeLookedUpByRazorpayOrderId() {
        Payment found = paymentRepository.findByRazorpayOrderId(testPayment.getRazorpayOrderId()).orElse(null);
        assert found != null;
        assert found.getPaymentId().equals(testPayment.getPaymentId());
        assert "PENDING".equals(found.getStatus());
    }

    @Test
    void paymentCanBeLookedUpByRazorpayPaymentId() {
        testPayment.setRazorpayPaymentId("pay_razorpay_test_123");
        paymentRepository.save(testPayment);

        Payment found = paymentRepository.findByRazorpayPaymentId("pay_razorpay_test_123").orElse(null);
        assert found != null;
        assert found.getPaymentId().equals(testPayment.getPaymentId());
    }

    @Test
    void paymentCanBeLookedUpByOrderIdOrPaymentId() {
        testPayment.setRazorpayPaymentId("pay_rzp_lookup_test");
        paymentRepository.save(testPayment);

        Payment found = paymentRepository.findByRazorpayOrderIdOrRazorpayPaymentId(
                "nonexistent_order", "pay_rzp_lookup_test").orElse(null);
        assert found != null;
        assert found.getRazorpayPaymentId().equals("pay_rzp_lookup_test");
    }

    @Test
    void paymentNotFoundForNonexistentRazorpayId() {
        assert paymentRepository.findByRazorpayOrderId("nonexistent").isEmpty();
        assert paymentRepository.findByRazorpayPaymentId("nonexistent").isEmpty();
    }

    // ─── Refund lookup tests ──────────────────────────────────

    @Test
    void refundCanBeLookedUpByRazorpayRefundId() {
        Refund refund = createRefund("rfnd_rzp_test_123", "PROCESSING");
        Refund found = refundRepository.findByRazorpayRefundId("rfnd_rzp_test_123").orElse(null);
        assert found != null;
        assert found.getRefundId().equals(refund.getRefundId());
    }

    @Test
    void refundNotFoundForNonexistentId() {
        assert refundRepository.findByRazorpayRefundId("nonexistent").isEmpty();
    }

    // ─── Entity field tests ───────────────────────────────────

    @Test
    void paymentEntityHasRazorpayFields() {
        testPayment.setRazorpayOrderId("order_123");
        testPayment.setRazorpayPaymentId("pay_456");
        paymentRepository.save(testPayment);

        Payment found = paymentRepository.findByPaymentId(paymentId).orElse(null);
        assert found != null;
        assert "order_123".equals(found.getRazorpayOrderId());
        assert "pay_456".equals(found.getRazorpayPaymentId());
    }

    @Test
    void refundEntityHasRazorpayRefundId() {
        createRefund("rfnd_rzp_test_789", "PENDING");
        Refund found = refundRepository.findByBookingId(testBooking.getId()).stream()
                .filter(r -> "rfnd_rzp_test_789".equals(r.getRazorpayRefundId()))
                .findFirst().orElse(null);
        assert found != null;
        assert "rfnd_rzp_test_789".equals(found.getRazorpayRefundId());
    }

    @Test
    void paymentEntityTimestampsWork() {
        assert testPayment.getCreatedAt() != null;
        testPayment.setStatus("PROCESSING");
        paymentRepository.save(testPayment);
        assert testPayment.getUpdatedAt() != null;
    }

    @Test
    void paymentStatusTransitionsWork() {
        assert "PENDING".equals(testPayment.getStatus());
        testPayment.setStatus("COMPLETED");
        paymentRepository.save(testPayment);
        Payment found = paymentRepository.findByPaymentId(paymentId).orElse(null);
        assert "COMPLETED".equals(found.getStatus());
    }

    @Test
    void paymentFailureRecordsReason() {
        testPayment.setStatus("FAILED");
        testPayment.setFailureReason("Insufficient funds");
        paymentRepository.save(testPayment);
        Payment found = paymentRepository.findByPaymentId(paymentId).orElse(null);
        assert "FAILED".equals(found.getStatus());
        assert "Insufficient funds".equals(found.getFailureReason());
    }

    // ─── Authenticated endpoint tests ──────────────────────────

    @Test
    void createOrderRequiresPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderRejectsZeroAmount() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEndpointRequiresBothFields() throws Exception {
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"order_test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEndpointProcessesValidPayment() throws Exception {
        // MockPaymentGateway always succeeds for valid inputs
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"order_ok\", \"paymentId\": \"pay_ok\", \"signature\": \"sig\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyEndpointFailsForInvalidPaymentId() throws Exception {
        // MockPaymentGateway returns failure when paymentId contains "fail"
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"order_fail\", \"paymentId\": \"pay_fail\", \"signature\": \"sig\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getPaymentStatusWorks() throws Exception {
        mockMvc.perform(get("/api/payments/status/" + paymentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotCreateOrder() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotVerifyPayment() throws Exception {
        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"x\", \"paymentId\": \"y\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── Helper methods ────────────────────────────────────────

    private Refund createRefund(String razorpayRefundId, String status) {
        Refund refund = new Refund();
        refund.setBooking(testBooking);
        refund.setRefundId("RF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        refund.setRefundAmount(new BigDecimal("2500.00"));
        refund.setOriginalAmount(new BigDecimal("5000.00"));
        refund.setRefundPercentage(new BigDecimal("50.00"));
        refund.setStatus(status);
        refund.setRazorpayRefundId(razorpayRefundId);
        return refundRepository.save(refund);
    }
}
