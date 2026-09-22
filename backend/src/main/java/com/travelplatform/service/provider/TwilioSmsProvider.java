package com.travelplatform.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnExpression("!'${twilio.account-sid:}'.isEmpty()")
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsProvider.class);

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank();
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!isConfigured()) {
            logger.warn("Twilio credentials not configured. SMS not sent to {}", maskPhone(phoneNumber));
            return;
        }

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, String> body = new HashMap<>();
        body.put("To", phoneNumber);
        body.put("From", fromNumber);
        body.put("Body", message);

        // Convert to form-urlencoded
        StringBuilder formBody = new StringBuilder();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (formBody.length() > 0) formBody.append("&");
            formBody.append(entry.getKey()).append("=").append(entry.getValue());
        }

        HttpEntity<String> request = new HttpEntity<>(formBody.toString(), headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            logger.info("SMS sent via Twilio to {}", maskPhone(phoneNumber));
        } catch (Exception e) {
            logger.error("Failed to send SMS via Twilio to {}", maskPhone(phoneNumber), e);
            throw new RuntimeException("SMS delivery failed");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }
}
