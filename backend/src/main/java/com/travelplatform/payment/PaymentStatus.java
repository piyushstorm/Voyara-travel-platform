package com.travelplatform.payment;

import java.math.BigDecimal;

public record PaymentStatus(
    String paymentId,
    String status,
    BigDecimal amount,
    String currency
) {}
