package com.travelplatform.service;

import com.travelplatform.dto.auth.*;
import com.travelplatform.entity.RefreshToken;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.EmailVerificationTokenRepository;
import com.travelplatform.repository.RefreshTokenRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setRole(Role.USER);
        testUser.setEmailVerified(true);
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:5173");
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        authService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getName().equals("Test User") &&
                user.getEmail().equals("test@example.com") &&
                user.getRole() == Role.USER
        ));
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.generateAccessToken(auth)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken("test@example.com")).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_validToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        RefreshToken storedToken = new RefreshToken("refresh-token", testUser, LocalDateTime.now().plusDays(7));
        storedToken.setId(1L);

        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));
        when(tokenProvider.generateAccessToken("test@example.com")).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken("test@example.com")).thenReturn("new-refresh");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.refreshAccessToken(request);

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertTrue(storedToken.isRevoked());
    }

    @Test
    void refreshAccessToken_invalidToken_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.refreshAccessToken(request));
    }

    @Test
    void logout_revokesToken() {
        RefreshToken storedToken = new RefreshToken("refresh-token", testUser, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));

        authService.logout("refresh-token");

        assertTrue(storedToken.isRevoked());
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void resetPassword_validToken() {
        // Store hashed token (simulating what requestPasswordReset does)
        testUser.setPasswordResetToken("$2a$10$hashedResetToken");
        testUser.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByPasswordResetTokenIsNotNull()).thenReturn(java.util.List.of(testUser));
        when(passwordEncoder.matches("reset-token-123", "$2a$10$hashedResetToken")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("newPassword123");

        authService.resetPassword(request);

        assertEquals("newEncodedPassword", testUser.getPassword());
        assertNull(testUser.getPasswordResetToken());
        assertNull(testUser.getPasswordResetTokenExpiry());
    }

    @Test
    void resetPassword_expiredToken_throws() {
        testUser.setPasswordResetToken("$2a$10$hashedExpiredToken");
        testUser.setPasswordResetTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userRepository.findByPasswordResetTokenIsNotNull()).thenReturn(java.util.List.of(testUser));
        when(passwordEncoder.matches("expired-token", "$2a$10$hashedExpiredToken")).thenReturn(true);

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPassword123");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
    }
}
