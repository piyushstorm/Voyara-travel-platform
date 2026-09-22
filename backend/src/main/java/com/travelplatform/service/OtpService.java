package com.travelplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_KEY_PREFIX = "otp_attempts:";
    private static final String OTP_RESEND_KEY_PREFIX = "otp_resend:";
    private static final String OTP_LOCKED_KEY_PREFIX = "otp_locked:";
    private static final String VERIFICATION_ID_KEY_PREFIX = "vid:";

    @Value("${otp.length:6}")
    private int otpLength = 6;

    @Value("${otp.expiry-seconds:300}")
    private int otpExpirySeconds = 300;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts = 5;

    @Value("${otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds = 60;

    @Value("${otp.max-per-hour:5}")
    private int maxPerHour = 5;

    @Value("${otp.lock-duration-seconds:900}")
    private int lockDurationSeconds = 900;

    // Fallback in-memory storage when Redis is unavailable (tests)
    private final ConcurrentHashMap<String, String> memoryStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> memoryTtl = new ConcurrentHashMap<>();

    @Nullable
    private final StringRedisTemplate redisTemplate;

    private boolean redisAvailable = false;

    public OtpService(@Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            try {
                redisTemplate.hasKey("__probe__");
                redisAvailable = true;
                logger.info("OTP service using Redis");
            } catch (Exception e) {
                logger.warn("Redis not available, OTP service using in-memory fallback");
            }
        } else {
            logger.warn("No StringRedisTemplate provided, OTP service using in-memory fallback");
        }
    }

    // =================== PHONE NORMALIZATION ===================

    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)\\.]", "");

        // Handle international prefix 00
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }

        // Handle accidental duplicate +9191 or 9191 (e.g. +91918355846580)
        if (cleaned.startsWith("+9191") && cleaned.length() == 14) {
            cleaned = "+91" + cleaned.substring(4);
        } else if (cleaned.startsWith("9191") && cleaned.length() == 14) {
            cleaned = "+91" + cleaned.substring(4);
        }

        // Handle leading 0 (trunk prefix in India, e.g. 08355846580)
        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }

        // Handle +91 followed by leading 0, e.g. +9108355846580
        if (cleaned.startsWith("+910") && cleaned.length() == 14) {
            cleaned = "+91" + cleaned.substring(4);
        }

        // 10 digits without country code -> default to +91
        if (!cleaned.startsWith("+") && cleaned.length() == 10 && cleaned.matches("\\d{10}")) {
            cleaned = "+91" + cleaned;
        } else if (cleaned.startsWith("91") && cleaned.length() == 12 && cleaned.matches("\\d{12}")) {
            cleaned = "+" + cleaned;
        } else if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        if (!PHONE_PATTERN.matcher(cleaned).matches()) return null;
        return cleaned;
    }

    // =================== OTP GENERATION ===================

    public String generateOtp(String phoneNumber) {
        if (isLocked(phoneNumber)) {
            logger.warn("OTP generation blocked - phone {} is locked", maskPhone(phoneNumber));
            return null;
        }
        if (isRateLimited(phoneNumber)) {
            logger.warn("OTP generation blocked - rate limited for {}", maskPhone(phoneNumber));
            return null;
        }
        if (isResendCooldownActive(phoneNumber)) {
            logger.warn("OTP generation blocked - resend cooldown for {}", maskPhone(phoneNumber));
            return null;
        }

        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        String otpCode = otp.toString();

        if (redisAvailable) {
            redisTemplate.opsForValue().set(OTP_KEY_PREFIX + phoneNumber, otpCode, otpExpirySeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(OTP_ATTEMPTS_KEY_PREFIX + phoneNumber, "0", otpExpirySeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(OTP_RESEND_KEY_PREFIX + phoneNumber, "1", resendCooldownSeconds, TimeUnit.SECONDS);
        } else {
            long now = System.currentTimeMillis();
            memoryStore.put(OTP_KEY_PREFIX + phoneNumber, otpCode);
            memoryTtl.put(OTP_KEY_PREFIX + phoneNumber, now + otpExpirySeconds * 1000L);
            memoryStore.put(OTP_ATTEMPTS_KEY_PREFIX + phoneNumber, "0");
            memoryTtl.put(OTP_ATTEMPTS_KEY_PREFIX + phoneNumber, now + otpExpirySeconds * 1000L);
            memoryStore.put(OTP_RESEND_KEY_PREFIX + phoneNumber, "1");
            memoryTtl.put(OTP_RESEND_KEY_PREFIX + phoneNumber, now + resendCooldownSeconds * 1000L);
        }

        logger.info("OTP generated for {} (expires in {}s)", maskPhone(phoneNumber), otpExpirySeconds);
        return otpCode;
    }

    // =================== OTP VERIFICATION ===================

    public OtpResult verifyOtp(String phoneNumber, String otp) {
        if (isLocked(phoneNumber)) {
            return OtpResult.LOCKED;
        }

        String attemptsKey = OTP_ATTEMPTS_KEY_PREFIX + phoneNumber;
        int attempts = getCounter(attemptsKey);

        if (attempts >= maxAttempts) {
            lockPhone(phoneNumber);
            return OtpResult.LOCKED;
        }

        incrementCounter(attemptsKey);

        String storedOtp = getStored(OTP_KEY_PREFIX + phoneNumber);

        if (storedOtp == null) {
            logger.warn("OTP expired or not found for {}", maskPhone(phoneNumber));
            return OtpResult.EXPIRED;
        }

        if (!storedOtp.equals(otp)) {
            logger.warn("Invalid OTP for {} (attempt {}/{})", maskPhone(phoneNumber), attempts + 1, maxAttempts);
            if (attempts + 1 >= maxAttempts) {
                lockPhone(phoneNumber);
                return OtpResult.LOCKED;
            }
            return OtpResult.INVALID;
        }

        // OTP valid - consume it
        deleteKey(OTP_KEY_PREFIX + phoneNumber);
        deleteKey(attemptsKey);

        logger.info("OTP verified successfully for {}", maskPhone(phoneNumber));
        return OtpResult.SUCCESS;
    }

    // =================== STATUS CHECKS ===================

    public boolean isLocked(String phoneNumber) {
        return hasKey(OTP_LOCKED_KEY_PREFIX + phoneNumber);
    }

    public boolean isResendCooldownActive(String phoneNumber) {
        return hasKey(OTP_RESEND_KEY_PREFIX + phoneNumber);
    }

    public long getResendCooldownSeconds(String phoneNumber) {
        if (redisAvailable) {
            Long ttl = redisTemplate.getExpire(OTP_RESEND_KEY_PREFIX + phoneNumber, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        }
        Long expiry = memoryTtl.get(OTP_RESEND_KEY_PREFIX + phoneNumber);
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return remaining > 0 ? remaining : 0;
    }

    // =================== PRIVATE HELPERS ===================

    private boolean isRateLimited(String phoneNumber) {
        String rateKey = "otp_rate:" + phoneNumber;
        int count = getCounter(rateKey);
        if (count >= maxPerHour) return true;
        incrementCounter(rateKey);
        if (redisAvailable) {
            redisTemplate.expire(rateKey, 1, TimeUnit.HOURS);
        } else {
            memoryTtl.put(rateKey, System.currentTimeMillis() + 3600000L);
        }
        return false;
    }

    private void lockPhone(String phoneNumber) {
        if (redisAvailable) {
            redisTemplate.opsForValue().set(OTP_LOCKED_KEY_PREFIX + phoneNumber, "1", lockDurationSeconds, TimeUnit.SECONDS);
        } else {
            memoryStore.put(OTP_LOCKED_KEY_PREFIX + phoneNumber, "1");
            memoryTtl.put(OTP_LOCKED_KEY_PREFIX + phoneNumber, System.currentTimeMillis() + lockDurationSeconds * 1000L);
        }
        logger.warn("Phone {} locked for {}s", maskPhone(phoneNumber), lockDurationSeconds);
    }

    // =================== REDIS/MEMORY ABSTRACTION ===================

    private String getStored(String key) {
        if (redisAvailable) {
            return redisTemplate.opsForValue().get(key);
        }
        Long ttl = memoryTtl.get(key);
        if (ttl != null && System.currentTimeMillis() > ttl) {
            memoryStore.remove(key);
            memoryTtl.remove(key);
            return null;
        }
        return memoryStore.get(key);
    }

    private boolean hasKey(String key) {
        if (redisAvailable) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }
        Long ttl = memoryTtl.get(key);
        if (ttl != null && System.currentTimeMillis() > ttl) {
            memoryStore.remove(key);
            memoryTtl.remove(key);
            return false;
        }
        return memoryStore.containsKey(key);
    }

    private void deleteKey(String key) {
        if (redisAvailable) {
            redisTemplate.delete(key);
        } else {
            memoryStore.remove(key);
            memoryTtl.remove(key);
        }
    }

    private int getCounter(String key) {
        String val = getStored(key);
        return val != null ? Integer.parseInt(val) : 0;
    }

    private void incrementCounter(String key) {
        if (redisAvailable) {
            redisTemplate.opsForValue().increment(key);
        } else {
            int current = getCounter(key);
            memoryStore.put(key, String.valueOf(current + 1));
            if (!memoryTtl.containsKey(key)) {
                memoryTtl.put(key, System.currentTimeMillis() + otpExpirySeconds * 1000L);
            }
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }

    // =================== VERIFICATION ID (for Message Central) ===================

    public void storeVerificationId(String phoneNumber, String verificationId) {
        if (redisAvailable) {
            redisTemplate.opsForValue().set(VERIFICATION_ID_KEY_PREFIX + phoneNumber, verificationId, otpExpirySeconds, TimeUnit.SECONDS);
        } else {
            long now = System.currentTimeMillis();
            memoryStore.put(VERIFICATION_ID_KEY_PREFIX + phoneNumber, verificationId);
            memoryTtl.put(VERIFICATION_ID_KEY_PREFIX + phoneNumber, now + otpExpirySeconds * 1000L);
        }
    }

    public String getVerificationId(String phoneNumber) {
        return getStored(VERIFICATION_ID_KEY_PREFIX + phoneNumber);
    }

    public void clearVerificationId(String phoneNumber) {
        deleteKey(VERIFICATION_ID_KEY_PREFIX + phoneNumber);
    }

    // =================== ATTEMPT COUNTER (for Message Central) ===================

    public void incrementAttemptCounter(String phoneNumber) {
        String key = OTP_ATTEMPTS_KEY_PREFIX + phoneNumber;
        incrementCounter(key);
        int attempts = getCounter(key);
        if (attempts >= maxAttempts) {
            lockPhone(phoneNumber);
        }
    }

    public void clearAttemptCounter(String phoneNumber) {
        deleteKey(OTP_ATTEMPTS_KEY_PREFIX + phoneNumber);
    }

    public enum OtpResult {
        SUCCESS, INVALID, EXPIRED, LOCKED
    }
}
