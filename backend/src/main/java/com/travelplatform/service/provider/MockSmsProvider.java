package com.travelplatform.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public void sendSms(String phoneNumber, String message) {
        // In development, log the SMS content to console
        // In production, configure MSG91 or Twilio instead
        logger.info("[MOCK SMS] to={}: {}", maskPhone(phoneNumber), message);
        logger.debug("[MOCK SMS CONTENT] {}", message);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }
}
