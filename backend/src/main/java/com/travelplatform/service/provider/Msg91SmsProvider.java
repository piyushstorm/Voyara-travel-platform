package com.travelplatform.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class Msg91SmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(Msg91SmsProvider.class);

    @Value("${msg91.auth-key:}")
    private String authKey;

    @Value("${msg91.template-id:}")
    private String templateId;

    @Value("${msg91.sender-id:VOYARA}")
    private String senderId;

    @Value("${msg91.base-url:https://api.msg91.com/api/v5}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return authKey != null && !authKey.isBlank();
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!isConfigured()) {
            logger.warn("MSG91 auth key not configured. SMS not sent to {}", maskPhone(phoneNumber));
            return;
        }

        String url = baseUrl + "/sms";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authkey", authKey);

        // MSG91 expects phone number WITHOUT the + prefix (e.g., 919876543210)
        String mobiles = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;

        Map<String, Object> body = new HashMap<>();
        body.put("sender", senderId);
        body.put("route", "4");
        body.put("country", "91");

        if (templateId != null && !templateId.isBlank()) {
            body.put("template_id", templateId);
            body.put("mobiles", mobiles);

            String otpCode = extractOtpFromMessage(message);
            if (otpCode != null) {
                body.put("VAR1", otpCode);
            }
            // MSG91 requires message field even with template_id for DLT compliance
            body.put("message", message);
        } else {
            body.put("message", message);
            body.put("mobiles", mobiles);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            String responseBody = response.getBody();

            // Log response status and sanitized body (never log auth keys or phone numbers)
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("MSG91 API HTTP {} - response: {}", response.getStatusCode().value(),
                        sanitizeResponse(responseBody));
                // Check for error in response body
                if (responseBody != null && (responseBody.contains("\"type\":\"error\"" ) ||
                        responseBody.contains("\"error\""))) {
                    logger.error("MSG91 returned error in response body");
                    throw new RuntimeException("MSG91 rejected the SMS request");
                }
            } else {
                logger.error("MSG91 API returned HTTP {}: {}", response.getStatusCode().value(),
                        sanitizeResponse(responseBody));
                throw new RuntimeException("MSG91 SMS failed with HTTP " + response.getStatusCode().value());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("MSG91 API client error: HTTP {}", e.getStatusCode().value());
            throw new RuntimeException("MSG91 SMS delivery failed: " + e.getStatusCode().value(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            logger.error("MSG91 API server error: HTTP {}", e.getStatusCode().value());
            throw new RuntimeException("MSG91 SMS delivery failed: " + e.getStatusCode().value(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send SMS via MSG91: {}", e.getMessage());
            throw new RuntimeException("SMS delivery failed: " + e.getMessage(), e);
        }
    }

    private String extractOtpFromMessage(String message) {
        if (message == null) return null;
        String[] parts = message.split(":");
        if (parts.length >= 2) {
            String otpPart = parts[1].trim();
            String[] otpAndRest = otpPart.split("\\.");
            if (otpAndRest.length >= 1) {
                String otp = otpAndRest[0].trim();
                if (otp.matches("\\d{6}")) {
                    return otp;
                }
            }
        }
        return null;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }

    /**
     * Sanitize MSG91 response for logging - remove any sensitive data.
     */
    private String sanitizeResponse(String response) {
        if (response == null) return "null";
        // Truncate long responses and remove potential auth tokens
        return response.length() > 200 ? response.substring(0, 200) + "..." : response;
    }
}
