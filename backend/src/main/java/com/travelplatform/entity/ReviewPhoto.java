package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_photos")
public class ReviewPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String photoUrl;

    private String caption;

    private Long fileSize;

    private String contentType;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public ReviewPhoto() {}

    public ReviewPhoto(Review review, String photoUrl, String caption) {
        this.review = review;
        this.photoUrl = photoUrl;
        this.caption = caption;
    }

    public ReviewPhoto(Review review, String photoUrl, String caption, Long fileSize, String contentType) {
        this.review = review;
        this.photoUrl = photoUrl;
        this.caption = caption;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
