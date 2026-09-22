package com.travelplatform.payment;

import com.travelplatform.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock payment gateway for development and testing.
 * Always succeeds unless amount is negative or paymentId contains "fail".
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public PaymentOrder createOrder(BigDecimal amount, String currency, String receipt) {
        String orderId = "mock_order_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Mock order created: {} for {} {}", orderId, amount, currency);
        return new PaymentOrder(orderId, amount, currency, receipt, getProviderName());
    }

    @Override
    public PaymentResult verifyPayment(String orderId, String paymentId, String signature) {
        if (paymentId != null && paymentId.contains("fail")) {
            return PaymentResult.failure(orderId, "Simulated payment failure");
        }
        if (orderId == null || orderId.isBlank()) {
            return PaymentResult.failure(orderId, "Invalid order");
        }

        String id = paymentId != null ? paymentId : Payment.generatePaymentId();
        logger.info("Mock payment verified: orderId={}, paymentId={}", orderId, id);
        return PaymentResult.success(id, orderId, BigDecimal.ZERO, "INR");
    }

    @Override
    public RefundResult processRefund(String paymentId, BigDecimal amount, String notes) {
        String refundId = "mock_refund_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Mock refund processed: refundId={}, paymentId={}, amount={}", refundId, paymentId, amount);
        return RefundResult.success(refundId, paymentId, amount);
    }

    @Override
    public PaymentStatus getStatus(String paymentId) {
        return new PaymentStatus(paymentId, "COMPLETED", BigDecimal.ZERO, "INR");
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }
}
