package com.travelplatform.payment;

import java.math.BigDecimal;

public record RefundResult(
    boolean success,
    String refundId,
    String paymentId,
    BigDecimal amount,
    String status,
    String failureReason
) {
    public static RefundResult success(String refundId, String paymentId, BigDecimal amount) {
        return new RefundResult(true, refundId, paymentId, amount, "COMPLETED", null);
    }

    public static RefundResult failure(String paymentId, String reason) {
        return new RefundResult(false, null, paymentId, null, "FAILED", reason);
    }
}
