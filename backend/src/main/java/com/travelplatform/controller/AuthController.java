package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.dto.auth.*;
import com.travelplatform.entity.AuthIdentity;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.AuthService;
import com.travelplatform.service.MultiAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "JWT authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final MultiAuthService multiAuthService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, MultiAuthService multiAuthService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.multiAuthService = multiAuthService;
        this.userRepository = userRepository;
    }

    // =================== EXISTING ENDPOINTS ===================

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Request a password reset token")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a reset link has been sent"));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email address")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Verification token is required"));
        }
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request);
        return ResponseEntity.ok(ApiResponse.success("If the account requires verification, an email has been sent."));
    }

    // =================== GOOGLE LOGIN ===================

    @PostMapping("/google")
    @Operation(summary = "Login or register with Google ID token")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = multiAuthService.googleLogin(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
    }

    @GetMapping("/google/configured")
    @Operation(summary = "Check if Google login is configured on the server")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> googleConfigured() {
        return ResponseEntity.ok(ApiResponse.success("Google config",
                Map.of("configured", multiAuthService.isGoogleConfigured())));
    }

    // =================== PHONE OTP ===================

    @PostMapping("/phone/send-otp")
    @Operation(summary = "Send OTP to phone number")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        multiAuthService.sendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your phone"));
    }

    @PostMapping("/phone/verify-otp")
    @Operation(summary = "Verify OTP and login/register with phone")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        LoginResponse response = multiAuthService.verifyOtp(request.getPhoneNumber(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("Phone verification successful", response));
    }

    // =================== ACCOUNT LINKING (authenticated) ===================

    @GetMapping("/identities")
    @Operation(summary = "Get linked authentication identities for current user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>>
            getIdentities(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<AuthIdentity> identities = multiAuthService.getUserIdentities(user);

        List<Map<String, Object>> result = identities.stream()
                .map(i -> {
                    var map = new java.util.HashMap<String, Object>();
                    map.put("id", i.getId());
                    map.put("provider", i.getProvider().name());
                    map.put("verified", i.isVerified());
                    if (i.getProviderEmail() != null) map.put("email", i.getProviderEmail());
                    if (i.getPhoneNumber() != null) {
                        String phone = i.getPhoneNumber();
                        if (phone.length() > 8) {
                            map.put("displayPhone", phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3));
                        } else {
                            map.put("displayPhone", phone);
                        }
                    }
                    if (i.getDisplayName() != null) map.put("displayName", i.getDisplayName());
                    map.put("createdAt", i.getCreatedAt());
                    return (Map<String, Object>) map;
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Identities retrieved", result));
    }

    @PostMapping("/identities/google")
    @Operation(summary = "Link Google identity to authenticated account")
    public ResponseEntity<ApiResponse<Void>>            linkGoogle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GoogleLoginRequest request) {
        if (userDetails == null) return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        multiAuthService.linkGoogleIdentity(user, request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success("Google account linked successfully"));
    }

    @PostMapping("/identities/phone/send-otp")
    @Operation(summary = "Send OTP for phone linking")
    public ResponseEntity<ApiResponse<Void>>            sendLinkingOtp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SendOtpRequest request) {
        if (userDetails == null) return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        multiAuthService.sendLinkingOtp(user, request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent"));
    }

    @PostMapping("/identities/phone")
    @Operation(summary = "Link phone identity to authenticated account")
    public ResponseEntity<ApiResponse<Void>>            linkPhone(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyOtpRequest request) {
        if (userDetails == null) return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        multiAuthService.linkPhoneIdentity(user, request.getPhoneNumber(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("Phone number linked successfully"));
    }

    @DeleteMapping("/identities/{provider}")
    @Operation(summary = "Unlink an authentication identity")
    public ResponseEntity<ApiResponse<Void>>            unlinkIdentity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String provider) {
        if (userDetails == null) return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        AuthIdentity.Provider providerEnum;
        try {
            providerEnum = AuthIdentity.Provider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid provider: " + provider));
        }
        multiAuthService.unlinkIdentity(user, providerEnum);
        return ResponseEntity.ok(ApiResponse.success(provider + " identity removed"));
    }
}
