package com.travelplatform.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewReportRequestDto {

    @NotBlank(message = "Reason is required")
    private String reason; // SPAM, OFFENSIVE, HARASSMENT, FAKE_REVIEW, IRRELEVANT, INAPPROPRIATE_CONTENT, PERSONAL_INFORMATION, OTHER

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    public ReviewReportRequestDto() {}

    public ReviewReportRequestDto(String reason, String description) {
        this.reason = reason;
        this.description = description;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
