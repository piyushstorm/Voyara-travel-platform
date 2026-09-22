package com.travelplatform.dto.review;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String targetType; // FLIGHT or HOTEL
    private Long targetId;
    private String targetName;
    private Long bookingId;
    private int rating;
    private String title;
    private String text;
    private int helpfulCount;
    private int notHelpfulCount;
    private String status;
    private boolean verifiedBooking;
    private int reportCount;
    private List<ReviewPhotoDto> photos = new ArrayList<>();
    private List<ReviewReplyDto> replies = new ArrayList<>();
    private Boolean userVotedHelpful; // true if current user voted HELPFUL, false if NOT_HELPFUL, null if no vote
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }
    public int getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(int notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isVerifiedBooking() { return verifiedBooking; }
    public void setVerifiedBooking(boolean verifiedBooking) { this.verifiedBooking = verifiedBooking; }
    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
    public List<ReviewPhotoDto> getPhotos() { return photos; }
    public void setPhotos(List<ReviewPhotoDto> photos) { this.photos = photos; }
    public List<ReviewReplyDto> getReplies() { return replies; }
    public void setReplies(List<ReviewReplyDto> replies) { this.replies = replies; }
    public Boolean getUserVotedHelpful() { return userVotedHelpful; }
    public void setUserVotedHelpful(Boolean userVotedHelpful) { this.userVotedHelpful = userVotedHelpful; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
