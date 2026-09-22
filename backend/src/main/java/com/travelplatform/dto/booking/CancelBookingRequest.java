package com.travelplatform.dto.booking;

import com.travelplatform.entity.CancellationReason;
import jakarta.validation.constraints.Size;

public class CancelBookingRequest {

    private String reason;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;

    public CancelBookingRequest() {}

    public CancelBookingRequest(String reason, String comment) {
        this.reason = reason;
        this.comment = comment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Resolve reason to validated enum
     */
    public CancellationReason resolveReason() {
        return CancellationReason.fromString(reason);
    }
}
