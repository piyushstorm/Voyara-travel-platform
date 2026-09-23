package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.dto.traveller.SavedTravellerRequest;
import com.travelplatform.entity.AuthIdentity;
import com.travelplatform.entity.SavedTraveller;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.AuthIdentityRepository;
import com.travelplatform.repository.SavedTravellerRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserRepository userRepository;
    private final SavedTravellerRepository travellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthIdentityRepository authIdentityRepository;

    public UserProfileController(UserRepository userRepository,
                                  SavedTravellerRepository travellerRepository,
                                  PasswordEncoder passwordEncoder,
                                  AuthIdentityRepository authIdentityRepository) {
        this.userRepository = userRepository;
        this.travellerRepository = travellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authIdentityRepository = authIdentityRepository;
    }

    // ---- Profile ----

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));

        String phone = authIdentityRepository.findByProviderAndUser(AuthIdentity.Provider.PHONE, user)
                .filter(AuthIdentity::isVerified)
                .map(AuthIdentity::getPhoneNumber)
                .filter(p -> p != null && !p.isBlank())
                .orElse(null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phone", phone);
        profile.put("role", user.getRole());
        profile.put("enabled", user.isEnabled());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());
        return ResponseEntity.ok(Map.of("success", true, "data", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(Authentication authentication,
                                                             @RequestBody Map<String, String> updates) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));

        if (updates.containsKey("phone")) {
            throw new BadRequestException("Phone number cannot be updated directly. Please use phone verification in Security settings.");
        }

        if (updates.containsKey("name")) {
            String name = updates.get("name");
            if (name == null || name.trim().length() < 2) {
                throw new BadRequestException("Name must be at least 2 characters");
            }
            user.setName(name.trim());
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated"));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(Authentication authentication,
                                                               @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            throw new BadRequestException("Current and new passwords are required");
        }
        if (newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
    }

    // ---- Saved Travellers ----

    @GetMapping("/travellers")
    public ResponseEntity<Map<String, Object>> getTravellers(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));
        List<SavedTraveller> travellers = travellerRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", travellers, "total", travellers.size()));
    }

    @PostMapping("/travellers")
    public ResponseEntity<Map<String, Object>> addTraveller(Authentication authentication,
                                                             @Valid @RequestBody SavedTravellerRequest request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));

        long count = travellerRepository.countByUserId(user.getId());
        if (count >= 10) {
            throw new BadRequestException("Maximum 10 saved travellers allowed");
        }

        SavedTraveller traveller = new SavedTraveller(user, request.getTitle(), request.getFirstName(), request.getLastName());
        traveller.setMiddleName(request.getMiddleName());
        traveller.setGender(request.getGender());
        traveller.setNationality(request.getNationality());
        traveller.setPassportNumber(request.getPassportNumber());
        traveller.setPassportCountry(request.getPassportCountry());
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
            traveller.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        }
        if (request.getPassportExpiry() != null && !request.getPassportExpiry().isEmpty()) {
            traveller.setPassportExpiry(LocalDate.parse(request.getPassportExpiry()));
        }

        travellerRepository.save(traveller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", traveller, "message", "Traveller saved"));
    }

    @PutMapping("/travellers/{id}")
    public ResponseEntity<Map<String, Object>> updateTraveller(Authentication authentication,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody SavedTravellerRequest request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));
        SavedTraveller traveller = travellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveller", "id", id));

        if (!traveller.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Not your traveller");
        }

        traveller.setTitle(request.getTitle());
        traveller.setFirstName(request.getFirstName());
        traveller.setMiddleName(request.getMiddleName());
        traveller.setLastName(request.getLastName());
        traveller.setGender(request.getGender());
        traveller.setNationality(request.getNationality());
        traveller.setPassportNumber(request.getPassportNumber());
        traveller.setPassportCountry(request.getPassportCountry());
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
            traveller.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        }
        if (request.getPassportExpiry() != null && !request.getPassportExpiry().isEmpty()) {
            traveller.setPassportExpiry(LocalDate.parse(request.getPassportExpiry()));
        }

        travellerRepository.save(traveller);
        return ResponseEntity.ok(Map.of("success", true, "data", traveller, "message", "Traveller updated"));
    }

    @DeleteMapping("/travellers/{id}")
    public ResponseEntity<Map<String, Object>> deleteTraveller(Authentication authentication,
                                                                @PathVariable Long id) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));
        SavedTraveller traveller = travellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveller", "id", id));

        if (!traveller.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Not your traveller");
        }

        travellerRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Traveller deleted"));
    }
}
