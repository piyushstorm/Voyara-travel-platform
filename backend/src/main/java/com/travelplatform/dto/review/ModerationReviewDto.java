package com.travelplatform.dto.review;

import java.util.ArrayList;
import java.util.List;

public class ModerationReviewDto extends ReviewResponseDto {

    private String userEmail;
    private String flagReason;
    private List<ReviewReportDto> reports = new ArrayList<>();
    private int totalReports;

    public ModerationReviewDto() {
        super();
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }

    public List<ReviewReportDto> getReports() { return reports; }
    public void setReports(List<ReviewReportDto> reports) { this.reports = reports; }

    public int getTotalReports() { return totalReports; }
    public void setTotalReports(int totalReports) { this.totalReports = totalReports; }
}
