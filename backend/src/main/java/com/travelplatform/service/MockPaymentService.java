package com.travelplatform.service;

import com.travelplatform.entity.Payment;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class MockPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentService.class);

    private final PaymentRepository paymentRepository;

    public MockPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment processPayment(BigDecimal amount, String paymentMethod, boolean simulateFailure) {
        String paymentId = Payment.generatePaymentId();

        Payment payment = new Payment(paymentId, amount, paymentMethod, "PENDING");
        payment = paymentRepository.save(payment);

        logger.info("Processing payment {}: amount={} method={}", paymentId, amount, paymentMethod);

        // Simulate payment processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (simulateFailure) {
            payment.setStatus("FAILED");
            payment.setFailureReason("Simulated payment failure for testing");
            payment = paymentRepository.save(payment);
            logger.warn("Payment {} failed (simulated)", paymentId);
            throw new BadRequestException("Payment failed: " + payment.getFailureReason());
        }

        // Simulate validation
        if (paymentMethod == null || paymentMethod.isBlank()) {
            payment.setStatus("FAILED");
            payment.setFailureReason("Invalid payment method");
            payment = paymentRepository.save(payment);
            throw new BadRequestException("Invalid payment method");
        }

        // Simulate amount validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            payment.setStatus("FAILED");
            payment.setFailureReason("Invalid amount");
            payment = paymentRepository.save(payment);
            throw new BadRequestException("Invalid payment amount");
        }

        // Success
        payment.setStatus("COMPLETED");
        payment = paymentRepository.save(payment);
        logger.info("Payment {} completed successfully", paymentId);
        return payment;
    }

    @Transactional
    public Payment refundPayment(Payment originalPayment, BigDecimal refundAmount) {
        String refundId = Payment.generatePaymentId();

        Payment refund = new Payment(refundId, refundAmount, originalPayment.getPaymentMethod(), "PROCESSING");
        refund = paymentRepository.save(refund);

        logger.info("Processing refund {} for original payment {}: amount={}",
                refundId, originalPayment.getPaymentId(), refundAmount);

        // Simulate refund processing
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        refund.setStatus("COMPLETED");
        refund = paymentRepository.save(refund);

        originalPayment.setStatus("REFUNDED");
        paymentRepository.save(originalPayment);

        logger.info("Refund {} completed", refundId);
        return refund;
    }
}
