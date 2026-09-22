package com.travelplatform.dto.review;

import java.time.LocalDateTime;

public class ReviewPhotoDto {
    private Long id;
    private String photoUrl;
    private String caption;
    private Long fileSize;
    private String contentType;
    private LocalDateTime createdAt;

    public ReviewPhotoDto() {}

    public ReviewPhotoDto(Long id, String photoUrl, String caption) {
        this.id = id;
        this.photoUrl = photoUrl;
        this.caption = caption;
    }

    public ReviewPhotoDto(Long id, String photoUrl, String caption, LocalDateTime createdAt) {
        this.id = id;
        this.photoUrl = photoUrl;
        this.caption = caption;
        this.createdAt = createdAt;
    }

    public ReviewPhotoDto(Long id, String photoUrl, String caption, Long fileSize, String contentType) {
        this.id = id;
        this.photoUrl = photoUrl;
        this.caption = caption;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
