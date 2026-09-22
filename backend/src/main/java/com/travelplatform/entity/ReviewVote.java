package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"review_id", "user_id"})
})
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** HELPFUL or NOT_HELPFUL */
    @Column(nullable = false)
    private String voteType;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public ReviewVote() {}

    public ReviewVote(Review review, User user, String voteType) {
        this.review = review;
        this.user = user;
        this.voteType = voteType;
    }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public User getUser() { return user; }
    public String getVoteType() { return voteType; }
    public void setVoteType(String voteType) { this.voteType = voteType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
