package com.travelplatform.dto.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundPreviewResponse {

    private Long bookingId;
    private String bookingReference;
    private String bookingType;
    private BigDecimal originalAmount;
    private BigDecimal eligibleRefundPercentage;
    private BigDecimal refundAmount;
    private BigDecimal nonRefundableAmount;
    private BigDecimal cancellationFee;
    private String policyApplied;
    private String policyExplanation;
    private String expectedTimeline;
    private LocalDateTime calculatedAt;

    public RefundPreviewResponse() {}

    public RefundPreviewResponse(Long bookingId, String bookingReference, String bookingType,
                                 BigDecimal originalAmount, BigDecimal eligibleRefundPercentage,
                                 BigDecimal refundAmount, BigDecimal nonRefundableAmount,
                                 BigDecimal cancellationFee, String policyApplied,
                                 String policyExplanation, String expectedTimeline,
                                 LocalDateTime calculatedAt) {
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.bookingType = bookingType;
        this.originalAmount = originalAmount;
        this.eligibleRefundPercentage = eligibleRefundPercentage;
        this.refundAmount = refundAmount;
        this.nonRefundableAmount = nonRefundableAmount;
        this.cancellationFee = cancellationFee;
        this.policyApplied = policyApplied;
        this.policyExplanation = policyExplanation;
        this.expectedTimeline = expectedTimeline;
        this.calculatedAt = calculatedAt;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getEligibleRefundPercentage() { return eligibleRefundPercentage; }
    public void setEligibleRefundPercentage(BigDecimal eligibleRefundPercentage) { this.eligibleRefundPercentage = eligibleRefundPercentage; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getNonRefundableAmount() { return nonRefundableAmount; }
    public void setNonRefundableAmount(BigDecimal nonRefundableAmount) { this.nonRefundableAmount = nonRefundableAmount; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public String getPolicyApplied() { return policyApplied; }
    public void setPolicyApplied(String policyApplied) { this.policyApplied = policyApplied; }

    public String getPolicyExplanation() { return policyExplanation; }
    public void setPolicyExplanation(String policyExplanation) { this.policyExplanation = policyExplanation; }

    public String getExpectedTimeline() { return expectedTimeline; }
    public void setExpectedTimeline(String expectedTimeline) { this.expectedTimeline = expectedTimeline; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
