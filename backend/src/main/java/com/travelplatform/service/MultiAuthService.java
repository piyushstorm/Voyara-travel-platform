package com.travelplatform.service;

import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.entity.AuthIdentity;
import com.travelplatform.entity.RefreshToken;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.AuthIdentityRepository;
import com.travelplatform.repository.RefreshTokenRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.security.JwtTokenProvider;
import com.travelplatform.service.GoogleAuthService.GoogleUserInfo;
import com.travelplatform.service.provider.MessageCentralSmsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MultiAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MultiAuthService.class);

    private final GoogleAuthService googleAuthService;
    private final OtpService otpService;
    private final MessageCentralSmsProvider messageCentralProvider;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    @Value("${frontend.base.url:http://localhost:5173}")
    private String frontendBaseUrl;

    public MultiAuthService(GoogleAuthService googleAuthService, OtpService otpService,
                            MessageCentralSmsProvider messageCentralProvider,
                            AuthIdentityRepository authIdentityRepository,
                            UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                            JwtTokenProvider tokenProvider, EmailService emailService) {
        this.googleAuthService = googleAuthService;
        this.otpService = otpService;
        this.messageCentralProvider = messageCentralProvider;
        this.authIdentityRepository = authIdentityRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.emailService = emailService;

        if (messageCentralProvider.isConfigured()) {
            logger.info("Message Central VerifyNow OTP provider = ENABLED");
        } else {
            logger.warn("Message Central VerifyNow OTP provider = DISABLED (credentials not configured)");
        }
    }

    // =================== GOOGLE LOGIN ===================

    public boolean isGoogleConfigured() {
        return googleAuthService.isConfigured();
    }

    @Transactional
    public LoginResponse googleLogin(String idTokenString) {
        GoogleUserInfo googleUser = googleAuthService.verifyIdToken(idTokenString);
        if (googleUser == null) {
            throw new BadRequestException("Google authentication failed. Please try again.");
        }

        var existingIdentity = authIdentityRepository
                .findByProviderAndProviderSubject(AuthIdentity.Provider.GOOGLE, googleUser.sub());

        if (existingIdentity.isPresent()) {
            User user = existingIdentity.get().getUser();
            if (!user.isEnabled()) {
                throw new BadRequestException("This account has been deactivated. Please contact support.");
            }
            logger.info("Google login: existing identity for user {}", user.getId());
            return issueTokens(user);
        }

        var existingUser = userRepository.findByEmail(googleUser.email());
        if (existingUser.isPresent()) {
            throw new BadRequestException("ACCOUNT_LINKING_REQUIRED|" + existingUser.get().getId());
        }

        User newUser = new User(
                googleUser.name() != null ? googleUser.name() : googleUser.email().split("@")[0],
                googleUser.email(),
                UUID.randomUUID().toString()
        );
        newUser.setEmailVerified(googleUser.emailVerified());
        newUser = userRepository.save(newUser);

        AuthIdentity identity = new AuthIdentity(newUser, AuthIdentity.Provider.GOOGLE, googleUser.sub());
        identity.setProviderEmail(googleUser.email());
        identity.setDisplayName(googleUser.name());
        identity.setVerified(googleUser.emailVerified());
        authIdentityRepository.save(identity);

        AuthIdentity localIdentity = new AuthIdentity(newUser, AuthIdentity.Provider.LOCAL, googleUser.email());
        localIdentity.setProviderEmail(googleUser.email());
        localIdentity.setVerified(googleUser.emailVerified());
        authIdentityRepository.save(localIdentity);

        logger.info("New user created via Google login: user={}, email={}", newUser.getId(), googleUser.email());

        try {
            emailService.sendEmail(
                newUser.getEmail(), "welcome", "Welcome to Voyara!",
                Map.of("name", newUser.getName(), "frontend_url", frontendBaseUrl)
            );
        } catch (Exception e) {
            logger.warn("Failed to send welcome email to new Google user", e);
        }

        return issueTokens(newUser);
    }

    // =================== PHONE OTP (Message Central VerifyNow) ===================

    /**
     * Send OTP via Message Central VerifyNow.
     * Message Central generates and delivers the OTP.
     * We store the verificationId for later validation.
     */
    public boolean sendOtp(String phoneNumber) {
        String normalizedPhone = otpService.normalizePhone(phoneNumber);
        if (normalizedPhone == null) {
            throw new BadRequestException("Enter a valid Indian mobile number (e.g., +919876543210).");
        }

        // Validate Indian mobile format (10 digits after +91)
        String mobileDigits = normalizedPhone.startsWith("+91") ? normalizedPhone.substring(3) : normalizedPhone;
        if (mobileDigits.length() != 10 || !mobileDigits.matches("\\d{10}")) {
            throw new BadRequestException("Enter a valid 10-digit Indian mobile number.");
        }

        if (otpService.isLocked(normalizedPhone)) {
            throw new BadRequestException("Too many failed attempts. Please try again later.");
        }

        if (otpService.isResendCooldownActive(normalizedPhone)) {
            long cooldown = otpService.getResendCooldownSeconds(normalizedPhone);
            throw new BadRequestException("Please wait " + cooldown + " seconds before requesting a new code.");
        }

        // Check if Message Central is configured
        if (!messageCentralProvider.isConfigured()) {
            throw new BadRequestException("OTP service is temporarily unavailable. Please try again later.");
        }

        // Send OTP via Message Central
        try {
            String verificationId = messageCentralProvider.sendOtp(normalizedPhone);
            if (verificationId == null || verificationId.isBlank()) {
                logger.error("Message Central returned null/empty verificationId");
                throw new BadRequestException("We couldn't send the OTP. Please try again.");
            }

            // Store verificationId in Redis for validation
            otpService.storeVerificationId(normalizedPhone, verificationId);

            // Set rate limiting keys
            otpService.generateOtp(normalizedPhone);

            logger.info("OTP sent via Message Central to {}", maskPhone(normalizedPhone));
            return true;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send OTP via Message Central to {}", maskPhone(normalizedPhone), e);
            throw new BadRequestException("We couldn't send the OTP. Please try again.");
        }
    }

    /**
     * Verify OTP via Message Central VerifyNow.
     * Uses the verificationId stored during sendOtp.
     */
    @Transactional
    public LoginResponse verifyOtp(String phoneNumber, String otp) {
        String normalizedPhone = otpService.normalizePhone(phoneNumber);
        if (normalizedPhone == null) {
            throw new BadRequestException("Enter a valid Indian mobile number.");
        }

        // Check rate limiting
        if (otpService.isLocked(normalizedPhone)) {
            throw new BadRequestException("Too many failed attempts. Please try again later.");
        }

        // Get stored verificationId
        String verificationId = otpService.getVerificationId(normalizedPhone);
        if (verificationId == null) {
            throw new BadRequestException("This OTP has expired. Please request a new OTP.");
        }

        // Validate via Message Central
        if (!messageCentralProvider.isConfigured()) {
            throw new BadRequestException("OTP service is temporarily unavailable. Please try again later.");
        }

        MessageCentralSmsProvider.ValidationStatus status;
        try {
            status = messageCentralProvider.validateOtpStatus(verificationId, otp);
        } catch (Exception e) {
            logger.error("Message Central validation error for {}", maskPhone(normalizedPhone), e);
            throw new BadRequestException("OTP verification failed. Please try again.");
        }

        if (status == MessageCentralSmsProvider.ValidationStatus.INVALID_OTP) {
            otpService.incrementAttemptCounter(normalizedPhone);
            throw new BadRequestException("Invalid verification code.");
        } else if (status == MessageCentralSmsProvider.ValidationStatus.EXPIRED_OTP) {
            otpService.clearVerificationId(normalizedPhone);
            throw new BadRequestException("This verification code has expired. Please request a new one.");
        } else if (status == MessageCentralSmsProvider.ValidationStatus.MAX_ATTEMPTS) {
            throw new BadRequestException("Too many attempts. Please wait before requesting another OTP.");
        } else if (status != MessageCentralSmsProvider.ValidationStatus.SUCCESS) {
            otpService.incrementAttemptCounter(normalizedPhone);
            throw new BadRequestException("Invalid verification code.");
        }

        // OTP verified - clear verification state
        otpService.clearVerificationId(normalizedPhone);
        otpService.clearAttemptCounter(normalizedPhone);

        logger.info("OTP verified successfully via Message Central for {}", maskPhone(normalizedPhone));

        // Find or create user by phone identity
        var existingIdentity = authIdentityRepository
                .findByProviderAndProviderSubject(AuthIdentity.Provider.PHONE, normalizedPhone);

        if (existingIdentity.isPresent()) {
            User user = existingIdentity.get().getUser();
            if (!user.isEnabled()) {
                throw new BadRequestException("This account has been deactivated. Please contact support.");
            }
            logger.info("Phone OTP login: existing identity for user {}", user.getId());
            return issueTokens(user);
        }

        // Check if phone is linked to another account
        var phoneIdentity = authIdentityRepository.findByPhoneNumber(normalizedPhone);
        if (phoneIdentity.isPresent()) {
            throw new BadRequestException("This phone number is already linked to another account.");
        }

        // Create new user with phone number
        User newUser = new User(
                "Traveler " + normalizedPhone.substring(normalizedPhone.length() - 4),
                "phone_" + normalizedPhone.hashCode() + "@voyara.phone",
                UUID.randomUUID().toString()
        );
        newUser.setEmailVerified(false);
        newUser = userRepository.save(newUser);

        AuthIdentity identity = new AuthIdentity(newUser, AuthIdentity.Provider.PHONE, normalizedPhone);
        identity.setPhoneNumber(normalizedPhone);
        identity.setVerified(true);
        authIdentityRepository.save(identity);

        logger.info("New user created via phone OTP: user={}, phone={}", newUser.getId(), maskPhone(normalizedPhone));

        return issueTokens(newUser);
    }

    // =================== ACCOUNT LINKING ===================

    @Transactional
    public void linkGoogleIdentity(User user, String idTokenString) {
        GoogleUserInfo googleUser = googleAuthService.verifyIdToken(idTokenString);
        if (googleUser == null) {
            throw new BadRequestException("Google authentication failed. Please try again.");
        }

        var existingIdentity = authIdentityRepository
                .findByProviderAndProviderSubject(AuthIdentity.Provider.GOOGLE, googleUser.sub());

        if (existingIdentity.isPresent()) {
            if (!existingIdentity.get().getUser().getId().equals(user.getId())) {
                throw new BadRequestException("This Google account is already linked to another Voyara account.");
            }
            throw new BadRequestException("This Google account is already linked to your account.");
        }

        AuthIdentity identity = new AuthIdentity(user, AuthIdentity.Provider.GOOGLE, googleUser.sub());
        identity.setProviderEmail(googleUser.email());
        identity.setDisplayName(googleUser.name());
        identity.setVerified(googleUser.emailVerified());
        authIdentityRepository.save(identity);

        logger.info("Google identity linked to user {}: sub={}", user.getId(), googleUser.sub());
    }

    public void sendLinkingOtp(User user, String phoneNumber) {
        String normalizedPhone = otpService.normalizePhone(phoneNumber);
        if (normalizedPhone == null) {
            throw new BadRequestException("Invalid phone number format.");
        }

        var existingPhone = authIdentityRepository.findByPhoneNumber(normalizedPhone);
        if (existingPhone.isPresent() && !existingPhone.get().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("This phone number is already linked to another account.");
        }

        var myPhone = authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, user);
        if (myPhone.isPresent() && normalizedPhone.equals(myPhone.get().getPhoneNumber())) {
            throw new BadRequestException("This phone number is already linked to your account.");
        }

        sendOtp(normalizedPhone);
    }

    @Transactional
    public void linkPhoneIdentity(User user, String phoneNumber, String otp) {
        String normalizedPhone = otpService.normalizePhone(phoneNumber);
        if (normalizedPhone == null) {
            throw new BadRequestException("Invalid phone number format.");
        }

        // Get stored verificationId
        String verificationId = otpService.getVerificationId(normalizedPhone);
        if (verificationId == null) {
            throw new BadRequestException("This OTP has expired. Please request a new OTP.");
        }

        // Validate via Message Central
        MessageCentralSmsProvider.ValidationStatus status;
        try {
            status = messageCentralProvider.validateOtpStatus(verificationId, otp);
        } catch (Exception e) {
            throw new BadRequestException("OTP verification failed. Please try again.");
        }

        if (status == MessageCentralSmsProvider.ValidationStatus.INVALID_OTP) {
            otpService.incrementAttemptCounter(normalizedPhone);
            throw new BadRequestException("Invalid verification code.");
        } else if (status == MessageCentralSmsProvider.ValidationStatus.EXPIRED_OTP) {
            otpService.clearVerificationId(normalizedPhone);
            throw new BadRequestException("This verification code has expired. Please request a new one.");
        } else if (status == MessageCentralSmsProvider.ValidationStatus.MAX_ATTEMPTS) {
            throw new BadRequestException("Too many attempts. Please wait before requesting another OTP.");
        } else if (status != MessageCentralSmsProvider.ValidationStatus.SUCCESS) {
            otpService.incrementAttemptCounter(normalizedPhone);
            throw new BadRequestException("Invalid verification code.");
        }

        otpService.clearVerificationId(normalizedPhone);
        otpService.clearAttemptCounter(normalizedPhone);

        var existingPhone = authIdentityRepository.findByPhoneNumber(normalizedPhone);
        if (existingPhone.isPresent() && !existingPhone.get().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("This phone number is already linked to another account.");
        }

        var myPhone = authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, user);
        if (myPhone.isPresent()) {
            myPhone.get().setPhoneNumber(normalizedPhone);
            myPhone.get().setProviderSubject(normalizedPhone);
            myPhone.get().setVerified(true);
            authIdentityRepository.save(myPhone.get());
        } else {
            AuthIdentity identity = new AuthIdentity(user, AuthIdentity.Provider.PHONE, normalizedPhone);
            identity.setPhoneNumber(normalizedPhone);
            identity.setVerified(true);
            authIdentityRepository.save(identity);
        }

        logger.info("Phone identity linked to user {}: phone={}", user.getId(), maskPhone(normalizedPhone));
    }

    @Transactional
    public void unlinkIdentity(User user, AuthIdentity.Provider provider) {
        List<AuthIdentity> identities = authIdentityRepository.findByUser(user);

        if (identities.size() <= 1) {
            throw new BadRequestException("Cannot remove your last authentication method. Add another method first.");
        }

        var target = identities.stream()
                .filter(i -> i.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No " + provider + " identity found."));

        authIdentityRepository.delete(target);
        logger.info("Identity {} unlinked from user {}", provider, user.getId());
    }

    public List<AuthIdentity> getUserIdentities(User user) {
        return authIdentityRepository.findByUser(user);
    }

    // =================== HELPER ===================

    private LoginResponse issueTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        String refreshTokenStr = tokenProvider.generateRefreshToken(user.getEmail());

        RefreshToken storedRefreshToken = new RefreshToken(
                refreshTokenStr, user, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(storedRefreshToken);

        return new LoginResponse(
                accessToken, refreshTokenStr,
                tokenProvider.getAccessTokenExpirationMs(),
                user.getEmail(), user.getName(), user.getRole());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }
}
