package com.travelplatform.dto.recommendation;

import java.util.ArrayList;
import java.util.List;

public class WhyRecommendationDto {
    private String title;
    private String badge;
    private List<RecommendationReasonDto> reasons = new ArrayList<>();

    public WhyRecommendationDto() {}

    public WhyRecommendationDto(String title, String badge, List<RecommendationReasonDto> reasons) {
        this.title = title;
        this.badge = badge;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public List<RecommendationReasonDto> getReasons() { return reasons; }
    public void setReasons(List<RecommendationReasonDto> reasons) { this.reasons = reasons; }
}
