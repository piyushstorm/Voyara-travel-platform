package com.travelplatform.dto.review;

import java.time.LocalDateTime;

public class ReviewReportDto {
    private Long id;
    private Long reviewId;
    private Long reporterUserId;
    private String reporterName;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public ReviewReportDto() {}

    public ReviewReportDto(Long id, String reporterName, String reason, String description, String status, LocalDateTime createdAt) {
        this.id = id;
        this.reporterName = reporterName;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public ReviewReportDto(Long id, Long reviewId, Long reporterUserId, String reporterName, String reason, String description, String status, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.reviewId = reviewId;
        this.reporterUserId = reporterUserId;
        this.reporterName = reporterName;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(Long reporterUserId) { this.reporterUserId = reporterUserId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
