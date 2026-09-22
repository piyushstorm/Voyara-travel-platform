package com.travelplatform.controller;

import com.travelplatform.entity.Notification;
import com.travelplatform.entity.NotificationPreference;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /** Get all notifications for current user */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        Page<Notification> notifications = notificationService.getUserNotifications(userId, type, unreadOnly, page, size);
        return ResponseEntity.ok(Map.of(
            "content", notifications.getContent().stream().map(this::toDto).toList(),
            "totalElements", notifications.getTotalElements(),
            "totalPages", notifications.getTotalPages(),
            "currentPage", notifications.getNumber(),
            "unreadCount", notificationService.getUnreadCount(userId)
        ));
    }

    /** Get unread count */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    /** Mark a single notification as read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        boolean success = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(Map.of("success", success, "unreadCount", notificationService.getUnreadCount(userId)));
    }

    /** Mark all notifications as read */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("marked", count, "unreadCount", 0));
    }

    // ─── Preferences ────────────────────────────────────────

    /** Get notification preferences */
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreference> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(notificationService.getOrCreatePreferences(userId));
    }

    /** Update notification preferences */
    @PutMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody NotificationPreference updates) {
        Long userId = getUserId(userDetails);
        NotificationPreference saved = notificationService.updatePreferences(userId, updates);
        return ResponseEntity.ok(Map.of("message", "Preferences updated", "preferences", saved));
    }

    // ─── Helpers ────────────────────────────────────────────

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> toDto(Notification n) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", n.getId());
        dto.put("type", n.getType());
        dto.put("title", n.getTitle());
        dto.put("message", n.getMessage());
        dto.put("isRead", n.isRead());
        dto.put("readAt", n.getReadAt());
        dto.put("relatedEntityType", n.getRelatedEntityType());
        dto.put("relatedEntityId", n.getRelatedEntityId());
        dto.put("relatedEntityRef", n.getRelatedEntityRef());
        dto.put("createdAt", n.getCreatedAt());
        return dto;
    }
}
