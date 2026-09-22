package com.travelplatform.dto.review;

import java.time.LocalDateTime;

public class ReviewReplyDto {
    private Long id;
    private Long reviewId;
    private Long userId;
    private String userName;
    private String userRole;
    private String text;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewReplyDto() {}

    public ReviewReplyDto(Long id, Long reviewId, Long userId, String userName, String userRole, String text, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reviewId = reviewId;
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.text = text;
        this.status = "ACTIVE";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ReviewReplyDto(Long id, Long reviewId, Long userId, String userName, String text, String status, LocalDateTime createdAt) {
        this.id = id;
        this.reviewId = reviewId;
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
