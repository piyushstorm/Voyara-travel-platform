package com.travelplatform.payment;

import java.math.BigDecimal;

/**
 * Abstraction over payment providers (Razorpay, Mock, etc.).
 * Each implementation handles order creation, verification, and refunds
 * for its specific provider while keeping the booking flow provider-agnostic.
 */
public interface PaymentGateway {

    /** Create a payment order and return the order details for client-side handling */
    PaymentOrder createOrder(BigDecimal amount, String currency, String receipt);

    /** Verify a payment signature after client-side payment completion */
    PaymentResult verifyPayment(String orderId, String paymentId, String signature);

    /** Process a refund for a completed payment */
    RefundResult processRefund(String paymentId, BigDecimal amount, String notes);

    /** Check the current status of a payment */
    PaymentStatus getStatus(String paymentId);

    /** Returns the provider name for logging and identification */
    String getProviderName();
}
