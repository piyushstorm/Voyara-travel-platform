package com.travelplatform.service.provider;

import java.util.Map;

public interface EmailProvider {
    void sendEmail(String to, String subject, String htmlContent);
}
