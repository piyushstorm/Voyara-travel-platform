package com.travelplatform.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Message Central VerifyNow SMS OTP provider.
 *
 * API docs: https://www.messagecentral.com/product/verify-now/api-india
 * Base URL: https://cpaas.messagecentral.com
 *
 * Flow:
 * 1. Generate auth token (cached ~24h)
 * 2. POST /verification/v3/send → returns verificationId
 * 3. GET  /verification/v3/validateOtp → returns verification status
 */
@Component
public class MessageCentralSmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(MessageCentralSmsProvider.class);
    private static final String BASE_URL = "https://cpaas.messagecentral.com";

    @Value("${messagecentral.customer-id:}")
    private String customerId;

    @Value("${messagecentral.email:}")
    private String email;

    @Value("${messagecentral.password:}")
    private String password;

    @Value("${messagecentral.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    // Auth token caching
    private volatile String cachedAuthToken;
    private volatile Instant tokenExpiry = Instant.MIN;

    public boolean isConfigured() {
        return enabled && customerId != null && !customerId.isBlank()
                && password != null && !password.isBlank();
    }

    // =================== AUTH TOKEN ===================

    /**
     * Get a valid auth token, refreshing if expired.
     * Token is valid for ~24 hours per Message Central docs.
     */
    private synchronized String getAuthToken() {
        // Refresh if expired or within 1 hour of expiry
        if (cachedAuthToken == null || Instant.now().plusSeconds(3600).isAfter(tokenExpiry)) {
            refreshAuthToken();
        }
        return cachedAuthToken;
    }

    private void refreshAuthToken() {
        String key = Base64.getEncoder().encodeToString(password.getBytes());

        String url = BASE_URL + "/auth/v1/authentication/token"
                + "?customerId=" + customerId
                + "&key=" + key
                + "&scope=NEW"
                + "&country=91"
                + "&email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedAuthToken = (String) response.getBody().get("token");
                tokenExpiry = Instant.now().plusSeconds(82800); // ~23 hours
                logger.info("Message Central auth token refreshed successfully");
            } else {
                logger.error("Message Central auth token refresh failed: HTTP {}", response.getStatusCode().value());
                throw new RuntimeException("Message Central authentication failed");
            }
        } catch (Exception e) {
            logger.error("Failed to refresh Message Central auth token: {}", e.getMessage());
            throw new RuntimeException("Message Central authentication failed: " + e.getMessage(), e);
        }
    }

    // =================== SEND OTP ===================

    /**
     * Send OTP via Message Central VerifyNow.
     * Returns the verificationId for later validation.
     */
    public String sendOtp(String phoneNumber) {
        if (!isConfigured()) {
            throw new RuntimeException("Message Central is not configured");
        }

        String authToken = getAuthToken();

        // Extract 10-digit number (remove +91 or 91 prefix)
        String mobileNumber = phoneNumber;
        if (mobileNumber.startsWith("+91")) {
            mobileNumber = mobileNumber.substring(3);
        } else if (mobileNumber.startsWith("91") && mobileNumber.length() == 12) {
            mobileNumber = mobileNumber.substring(2);
        }

        String url = BASE_URL + "/verification/v3/send"
                + "?countryCode=91"
                + "&mobileNumber=" + mobileNumber
                + "&flowType=SMS"
                + "&otpLength=6";

        HttpHeaders headers = new HttpHeaders();
        headers.set("authToken", authToken);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object dataObj = body.get("data");
                if (dataObj instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    Object vid = data.get("verificationId");
                    if (vid != null) {
                        String verificationId = String.valueOf(vid);
                        logger.info("Message Central OTP sent successfully, verificationId present: true");
                        return verificationId;
                    }
                }
            }

            logger.error("Message Central send OTP failed: HTTP {}", response.getStatusCode().value());
            throw new RuntimeException("OTP delivery failed");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send OTP via Message Central: {}", e.getMessage());
            throw new RuntimeException("OTP delivery failed: " + e.getMessage(), e);
        }
    }

    // =================== VALIDATE OTP ===================

    public enum ValidationStatus {
        SUCCESS,
        INVALID_OTP,
        EXPIRED_OTP,
        MAX_ATTEMPTS,
        ERROR
    }

    /**
     * Validate OTP via Message Central VerifyNow with detailed status.
     */
    public ValidationStatus validateOtpStatus(String verificationId, String code) {
        if (!isConfigured()) {
            throw new RuntimeException("Message Central is not configured");
        }

        String authToken = getAuthToken();

        String url = BASE_URL + "/verification/v3/validateOtp"
                + "?verificationId=" + verificationId
                + "&code=" + code;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authToken", authToken);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                int responseCode = body.containsKey("responseCode") && body.get("responseCode") != null ?
                        Integer.parseInt(String.valueOf(body.get("responseCode"))) : response.getStatusCode().value();
                String message = body.containsKey("message") && body.get("message") != null ?
                        String.valueOf(body.get("message")) : "";

                if (responseCode == 200) {
                    Object dataObj = body.get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        String status = (String) data.get("verificationStatus");
                        if ("VERIFICATION_COMPLETED".equals(status)) {
                            logger.info("Message Central OTP validation: SUCCESS");
                            return ValidationStatus.SUCCESS;
                        }
                    }
                } else if (responseCode == 702 || message.contains("WRONG_OTP")) {
                    logger.warn("Message Central OTP validation: INVALID_OTP (code {})", responseCode);
                    return ValidationStatus.INVALID_OTP;
                } else if (responseCode == 703 || message.contains("EXPIRED")) {
                    logger.warn("Message Central OTP validation: EXPIRED_OTP (code {})", responseCode);
                    return ValidationStatus.EXPIRED_OTP;
                } else if (responseCode == 704 || message.contains("MAX_ATTEMPTS") || message.contains("LIMIT")) {
                    logger.warn("Message Central OTP validation: MAX_ATTEMPTS (code {})", responseCode);
                    return ValidationStatus.MAX_ATTEMPTS;
                }
            }

            logger.error("Message Central validate OTP failed: HTTP {}", response.getStatusCode().value());
            return ValidationStatus.INVALID_OTP;
        } catch (Exception e) {
            logger.error("Failed to validate OTP via Message Central: {}", e.getMessage());
            return ValidationStatus.ERROR;
        }
    }

    /**
     * Validate OTP via Message Central VerifyNow.
     * Returns true if verification succeeded.
     */
    public boolean validateOtp(String verificationId, String code) {
        return validateOtpStatus(verificationId, code) == ValidationStatus.SUCCESS;
    }

    // =================== HELPERS ===================

    /**
     * Extract country code and mobile number for Message Central API.
     * Returns [countryCode, mobileNumber].
     */
    public static String[] extractCountryAndMobile(String phoneNumber) {
        if (phoneNumber == null) return new String[]{"91", ""};
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)\\.]", "");
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }
        return new String[]{"91", cleaned};
    }
}
