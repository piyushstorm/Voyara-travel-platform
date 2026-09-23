package com.travelplatform.service;

import com.travelplatform.dto.traveller.SavedTravellerRequest;
import com.travelplatform.entity.AuthIdentity;
import com.travelplatform.entity.Role;
import com.travelplatform.entity.SavedTraveller;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.AuthIdentityRepository;
import com.travelplatform.repository.SavedTravellerRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.controller.UserProfileController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SavedTravellerRepository travellerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthIdentityRepository authIdentityRepository;

    @InjectMocks
    private UserProfileController controller;

    private User testUser;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setRole(Role.USER);
        auth = new TestingAuthenticationToken("test@example.com", null);
    }

    @Test
    void getProfile_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.empty());

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_phoneAuthenticatedUser_returnsVerifiedPhone() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        AuthIdentity phoneIdentity = new AuthIdentity(testUser, AuthIdentity.Provider.PHONE, "+919876543210");
        phoneIdentity.setPhoneNumber("+919876543210");
        phoneIdentity.setVerified(true);
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.of(phoneIdentity));

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertEquals("+919876543210", data.get("phone"));
        assertEquals("test@example.com", data.get("email"));
        assertEquals("Test User", data.get("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_emailOnlyUser_phoneIsNull() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.empty());

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNull(data.get("phone"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_googleOnlyUser_phoneIsNull() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.empty());

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNull(data.get("phone"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_userWithLinkedPhone_returnsLinkedPhone() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        AuthIdentity linkedPhoneIdentity = new AuthIdentity(testUser, AuthIdentity.Provider.PHONE, "+919812345678");
        linkedPhoneIdentity.setPhoneNumber("+919812345678");
        linkedPhoneIdentity.setVerified(true);
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.of(linkedPhoneIdentity));

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertEquals("+919812345678", data.get("phone"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_scopedToAuthenticatedUser_doesNotReturnOtherUsersPhone() {
        User otherUser = new User("Other User", "other@example.com", "encodedPassword");
        otherUser.setId(2L);
        AuthIdentity otherUserPhone = new AuthIdentity(otherUser, AuthIdentity.Provider.PHONE, "+919999999999");
        otherUserPhone.setPhoneNumber("+919999999999");
        otherUserPhone.setVerified(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.empty());

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNull(data.get("phone"));
        verify(authIdentityRepository, never()).findByProviderAndUser(AuthIdentity.Provider.PHONE, otherUser);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_unverifiedPhoneIdentity_returnsNull() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        AuthIdentity unverifiedPhone = new AuthIdentity(testUser, AuthIdentity.Provider.PHONE, "+919876543210");
        unverifiedPhone.setPhoneNumber("+919876543210");
        unverifiedPhone.setVerified(false);
        when(authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, testUser))
                .thenReturn(Optional.of(unverifiedPhone));

        var response = controller.getProfile(auth);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNull(data.get("phone"));
    }

    @Test
    void updateProfile_attemptPhoneUpdate_throwsBadRequest() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        var updates = new java.util.HashMap<String, String>();
        updates.put("phone", "+919999999999");

        assertThrows(BadRequestException.class, () -> controller.updateProfile(auth, updates));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        var updates = new java.util.HashMap<String, String>();
        updates.put("name", "Updated Name");

        var response = controller.updateProfile(auth, updates);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Updated Name", testUser.getName());
    }

    @Test
    void changePassword_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncoded");
        when(userRepository.save(any())).thenReturn(testUser);

        var body = new java.util.HashMap<String, String>();
        body.put("currentPassword", "oldPassword");
        body.put("newPassword", "newPassword123");

        var response = controller.changePassword(auth, body);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void changePassword_wrongPassword_throws() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        var body = new java.util.HashMap<String, String>();
        body.put("currentPassword", "wrongPassword");
        body.put("newPassword", "newPassword123");

        assertThrows(BadRequestException.class, () -> controller.changePassword(auth, body));
    }

    @Test
    void addTraveller_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(travellerRepository.countByUserId(1L)).thenReturn(0L);
        when(travellerRepository.save(any())).thenAnswer(inv -> {
            SavedTraveller t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        SavedTravellerRequest request = new SavedTravellerRequest();
        request.setTitle("Mr");
        request.setFirstName("John");
        request.setLastName("Doe");

        var response = controller.addTraveller(auth, request);
        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void addTraveller_maxLimit_throws() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(travellerRepository.countByUserId(1L)).thenReturn(10L);

        SavedTravellerRequest request = new SavedTravellerRequest();
        request.setTitle("Mr");
        request.setFirstName("John");
        request.setLastName("Doe");

        assertThrows(BadRequestException.class, () -> controller.addTraveller(auth, request));
    }

    @Test
    void deleteTraveller_notYours_throws() {
        User otherUser = new User("Other", "other@example.com", "pass");
        otherUser.setId(2L);

        SavedTraveller traveller = new SavedTraveller(otherUser, "Mr", "John", "Doe");
        traveller.setId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(travellerRepository.findById(1L)).thenReturn(Optional.of(traveller));

        assertThrows(BadRequestException.class, () -> controller.deleteTraveller(auth, 1L));
    }

    @Test
    void getTravellers_empty() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(travellerRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        var response = controller.getTravellers(auth);
        assertEquals(200, response.getStatusCode().value());
    }
}
