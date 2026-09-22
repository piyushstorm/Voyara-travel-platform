package com.travelplatform.payment;

import java.math.BigDecimal;

public record PaymentResult(
    boolean success,
    String paymentId,
    String orderId,
    BigDecimal amount,
    String currency,
    String status,
    String failureReason
) {
    public static PaymentResult success(String paymentId, String orderId, BigDecimal amount, String currency) {
        return new PaymentResult(true, paymentId, orderId, amount, currency, "COMPLETED", null);
    }

    public static PaymentResult failure(String orderId, String reason) {
        return new PaymentResult(false, null, orderId, null, null, "FAILED", reason);
    }
}
