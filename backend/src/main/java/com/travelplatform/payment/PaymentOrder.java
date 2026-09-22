package com.travelplatform.payment;

import java.math.BigDecimal;

public record PaymentOrder(
    String orderId,
    BigDecimal amount,
    String currency,
    String receipt,
    String providerName
) {}
