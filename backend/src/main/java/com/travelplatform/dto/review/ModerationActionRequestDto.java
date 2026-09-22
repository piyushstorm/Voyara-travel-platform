package com.travelplatform.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ModerationActionRequestDto {

    @NotBlank(message = "Action is required")
    private String action; // APPROVE, DISMISS_REPORTS, REMOVE, RESTORE

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public ModerationActionRequestDto() {}

    public ModerationActionRequestDto(String action, String reason) {
        this.action = action;
        this.reason = reason;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
