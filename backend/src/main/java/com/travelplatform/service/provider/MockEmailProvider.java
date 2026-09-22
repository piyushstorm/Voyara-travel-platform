package com.travelplatform.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("mockEmailProvider")
public class MockEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailProvider.class);

    @Override
    public void sendEmail(String to, String subject, String htmlContent) {
        logger.info("[MOCK EMAIL] to={}, subject={}", maskEmail(to), subject);
        logger.debug("[MOCK EMAIL CONTENT]\n{}", htmlContent);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
