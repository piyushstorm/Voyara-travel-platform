package com.travelplatform.dto.review;

import java.util.HashMap;
import java.util.Map;

public class RatingSummaryDto {
    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> ratingDistribution = new HashMap<>();

    public RatingSummaryDto() {
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
    }

    public RatingSummaryDto(double averageRating, long totalReviews, Map<Integer, Long> distribution) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingDistribution = distribution != null ? distribution : new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            this.ratingDistribution.putIfAbsent(i, 0L);
        }
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }
    public long getReviewCount() { return totalReviews; }
    public void setReviewCount(long reviewCount) { this.totalReviews = reviewCount; }
    public Map<Integer, Long> getRatingDistribution() { return ratingDistribution; }
    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) { this.ratingDistribution = ratingDistribution; }
}
