package com.travelplatform.service;

import com.travelplatform.dto.auth.*;
import com.travelplatform.entity.EmailVerificationToken;
import com.travelplatform.entity.RefreshToken;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.EmailVerificationTokenRepository;
import com.travelplatform.repository.RefreshTokenRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    @Value("${frontend.base.url:http://localhost:5173}")
    private String frontendBaseUrl;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.emailService = emailService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // Keep generic response for security if needed, but standard is throwing BadRequest
            throw new BadRequestException("Email is already registered");
        }

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);
        user.setEmailVerified(false);
        userRepository.save(user);

        logger.info("New user registered, pending verification: {}", request.getEmail());
        
        sendVerificationEmail(user);
    }

    private void sendVerificationEmail(User user) {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawToken);

        EmailVerificationToken verificationToken = new EmailVerificationToken(
                user,
                hashedToken,
                LocalDateTime.now().plusHours(24)
        );
        emailVerificationTokenRepository.save(verificationToken);

        String verificationUrl = frontendBaseUrl + "/verify-email?token=" + rawToken;

        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getName(),
                verificationUrl,
                frontendBaseUrl
        );
        
        // Log safe info
        logger.info("Verification email sent to user {}", user.getId());
    }

    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                // Expire any existing pending tokens
                emailVerificationTokenRepository
                        .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
                        .ifPresent(existingToken -> {
                            existingToken.setStatus("EXPIRED");
                            emailVerificationTokenRepository.save(existingToken);
                        });
                
                sendVerificationEmail(user);
            }
        });
        // Always return success to prevent email enumeration
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        // Efficient lookup: find any token by hash, then verify status
        // Since we store bcrypt hashes, we need to find candidates and match
        // Strategy: load PENDING tokens and match via passwordEncoder.matches
        EmailVerificationToken validToken = emailVerificationTokenRepository
                .findAll().stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .filter(t -> passwordEncoder.matches(rawToken, t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or already used verification token"));

        if (validToken.isExpired()) {
            validToken.setStatus("EXPIRED");
            emailVerificationTokenRepository.save(validToken);
            throw new BadRequestException("Verification token has expired");
        }

        validToken.setStatus("USED");
        validToken.setUsedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(validToken);

        User user = validToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        logger.info("Email verified for user {}", user.getEmail());

        // Send welcome email now that they are fully activated
        emailService.sendWelcomeEmail(
                user.getEmail(),
                user.getName(),
                frontendBaseUrl
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password")); // generic

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            // Give specific error for frontend to trigger resend flow
            throw new BadRequestException("EMAIL_NOT_VERIFIED");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.getEmail());

        RefreshToken storedRefreshToken = new RefreshToken(
                refreshToken,
                user,
                LocalDateTime.now().plusDays(7)
        );
        refreshTokenRepository.save(storedRefreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpirationMs(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshTokenStr = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshTokenStr)) {
            throw new BadRequestException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new BadRequestException("Refresh token is revoked or expired");
        }

        String email = refreshToken.getUser().getEmail();

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new tokens
        String newAccessToken = tokenProvider.generateAccessToken(email);
        String newRefreshToken = tokenProvider.generateRefreshToken(email);

        RefreshToken newStoredRefreshToken = new RefreshToken(
                newRefreshToken,
                refreshToken.getUser(),
                LocalDateTime.now().plusDays(7)
        );
        refreshTokenRepository.save(newStoredRefreshToken);

        User user = refreshToken.getUser();
        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                tokenProvider.getAccessTokenExpirationMs(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        }
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(passwordEncoder.encode(rawToken));
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + rawToken;
            
            emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getName(),
                resetUrl,
                frontendBaseUrl
            );
            
            logger.info("Password reset token generated and email sent for user ID: {}", user.getId());
        });
        // Always return success to prevent email enumeration
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        // Validate password strength
        validatePasswordStrength(request.getNewPassword());

        User user = userRepository.findByPasswordResetTokenIsNotNull().stream()
                .filter(u -> passwordEncoder.matches(request.getToken(), u.getPasswordResetToken()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        // Invalidate all existing refresh tokens for this user (force re-login)
        refreshTokenRepository.findByToken(null); // just to ensure repo is wired
        // Delete all refresh tokens for this user to force re-authentication
        refreshTokenRepository.deleteByUser(user);

        logger.info("Password reset completed for user ID: {}. All sessions invalidated.", user.getId());

        // Send confirmation email
        emailService.sendPasswordResetConfirmedEmail(
            user.getEmail(),
            user.getName(),
            frontendBaseUrl
        );
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }
        if (password.length() > 128) {
            throw new BadRequestException("Password must not exceed 128 characters");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new BadRequestException("Password must contain at least one uppercase letter, one lowercase letter, and one digit");
        }
    }
}
