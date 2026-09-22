package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.service.*;
import com.travelplatform.repository.BookingRepository;
import com.travelplatform.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/voyara")
public class TravelController {

    private final TravelGuardianService guardianService;
    private final TripReadinessService readinessService;
    private final SmartTimelineService timelineService;
    private final ConnectionRiskService connectionRiskService;
    private final GroupTripService groupTripService;
    private final SmartExpenseService expenseService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public TravelController(TravelGuardianService guardianService,
                            TripReadinessService readinessService,
                            SmartTimelineService timelineService,
                            ConnectionRiskService connectionRiskService,
                            GroupTripService groupTripService,
                            SmartExpenseService expenseService,
                            UserRepository userRepository,
                            BookingRepository bookingRepository) {
        this.guardianService = guardianService;
        this.readinessService = readinessService;
        this.timelineService = timelineService;
        this.connectionRiskService = connectionRiskService;
        this.groupTripService = groupTripService;
        this.expenseService = expenseService;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    private User getUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    /**
     * IDOR protection: verify the authenticated user owns the booking.
     * Returns null if authorized, or a 403 ResponseEntity if not.
     */
    private ResponseEntity<Map<String, Object>> assertBookingOwner(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Booking not found"));
        }
        if (!booking.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
        }
        return null; // Authorized
    }

    // ════════════════════════════════════════════════════
    // TRAVEL GUARDIAN
    // ════════════════════════════════════════════════════

    @GetMapping("/guardian/alerts")
    public ResponseEntity<Map<String, Object>> getGuardianAlerts(
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        List<GuardianAlert> alerts = guardianService.getUserAlerts(user.getId());
        long unread = guardianService.getUnreadAlertCount(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", alerts, "unreadCount", unread));
    }

    @PostMapping("/guardian/analyze")
    public ResponseEntity<Map<String, Object>> analyzeTrips(
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        List<GuardianAlert> newAlerts = guardianService.analyzeAndGenerateAlerts(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "newAlerts", newAlerts.size(), "alerts", newAlerts));
    }

    @PostMapping("/guardian/alerts/{id}/read")
    public ResponseEntity<Map<String, Object>> markAlertRead(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        guardianService.markAsRead(id, user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/guardian/alerts/read-all")
    public ResponseEntity<Map<String, Object>> markAllAlertsRead(
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        guardianService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/guardian/alerts/{id}/dismiss")
    public ResponseEntity<Map<String, Object>> dismissAlert(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        guardianService.dismissAlert(id, user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ════════════════════════════════════════════════════
    // TRIP READINESS
    // ════════════════════════════════════════════════════

    @GetMapping("/readiness/{bookingId}")
    public ResponseEntity<Map<String, Object>> getReadiness(
            @PathVariable Long bookingId, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        ResponseEntity<Map<String, Object>> denied = assertBookingOwner(bookingId, user);
        if (denied != null) return denied;
        TripReadiness readiness = readinessService.getReadiness(bookingId);
        return ResponseEntity.ok(Map.of("success", true, "data", readiness));
    }

    @PostMapping("/readiness/{bookingId}/recalculate")
    public ResponseEntity<Map<String, Object>> recalculateReadiness(
            @PathVariable Long bookingId, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        ResponseEntity<Map<String, Object>> denied = assertBookingOwner(bookingId, user);
        if (denied != null) return denied;
        TripReadiness readiness = readinessService.calculateReadiness(bookingId);
        return ResponseEntity.ok(Map.of("success", true, "data", readiness));
    }

    // ════════════════════════════════════════════════════
    // SMART TIMELINE
    // ════════════════════════════════════════════════════

    @GetMapping("/timeline/{bookingId}")
    public ResponseEntity<Map<String, Object>> getTimeline(
            @PathVariable Long bookingId, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        ResponseEntity<Map<String, Object>> denied = assertBookingOwner(bookingId, user);
        if (denied != null) return denied;
        List<TripTimelineEvent> events = timelineService.getTimeline(bookingId);
        return ResponseEntity.ok(Map.of("success", true, "data", events));
    }

    @PostMapping("/timeline/{bookingId}/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateTimeline(
            @PathVariable Long bookingId, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        ResponseEntity<Map<String, Object>> denied = assertBookingOwner(bookingId, user);
        if (denied != null) return denied;
        List<TripTimelineEvent> events = timelineService.generateTimeline(bookingId);
        return ResponseEntity.ok(Map.of("success", true, "data", events));
    }

    @GetMapping("/timeline")
    public ResponseEntity<Map<String, Object>> getUserTimeline(
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        List<TripTimelineEvent> events = timelineService.getUserUpcomingTimeline(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", events));
    }

    // ════════════════════════════════════════════════════
    // CONNECTION RISK
    // ════════════════════════════════════════════════════

    @GetMapping("/connection-risk/{bookingId}")
    public ResponseEntity<Map<String, Object>> getConnectionRisk(
            @PathVariable Long bookingId, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        ResponseEntity<Map<String, Object>> denied = assertBookingOwner(bookingId, user);
        if (denied != null) return denied;
        Optional<ConnectionRisk> risk = connectionRiskService.getRiskForBooking(bookingId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("success", true);
        body.put("data", risk.orElse(null));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/connection-risk/calculate")
    public ResponseEntity<Map<String, Object>> calculateConnectionRisk(
            @RequestParam Long firstFlightId,
            @RequestParam Long secondFlightId,
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        ConnectionRisk risk = connectionRiskService.calculateRisk(user.getId(), firstFlightId, secondFlightId);
        return ResponseEntity.ok(Map.of("success", true, "data", risk));
    }

    // ════════════════════════════════════════════════════
    // GROUP TRIPS
    // ════════════════════════════════════════════════════

    @GetMapping("/group-trips")
    public ResponseEntity<Map<String, Object>> getMyGroupTrips(
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        List<GroupTrip> trips = groupTripService.getUserTrips(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", trips));
    }

    @PostMapping("/group-trips")
    public ResponseEntity<Map<String, Object>> createGroupTrip(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        GroupTrip trip = groupTripService.createGroupTrip(
            (String) body.get("name"),
            user.getId(),
            (String) body.get("description"),
            body.get("startDate") != null ? LocalDate.parse((String) body.get("startDate")) : null,
            body.get("endDate") != null ? LocalDate.parse((String) body.get("endDate")) : null
        );
        return ResponseEntity.ok(Map.of("success", true, "data", trip));
    }

    @GetMapping("/group-trips/{id}")
    public ResponseEntity<Map<String, Object>> getGroupTrip(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        if (!groupTripService.isMember(id, user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a member"));
        }
        GroupTrip trip = groupTripService.getGroupTrip(id);
        List<TravelCompanion> companions = groupTripService.getCompanions(id);
        return ResponseEntity.ok(Map.of("success", true, "data", trip, "companions", companions));
    }

    @PostMapping("/group-trips/{id}/invite")
    public ResponseEntity<Map<String, Object>> inviteCompanion(
            @PathVariable Long id, @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        TripInvitation invitation = groupTripService.inviteCompanion(
            id, user.getId(), body.get("email"), body.get("message"));
        return ResponseEntity.ok(Map.of("success", true, "data", invitation));
    }

    @GetMapping("/invitations/{token}")
    public ResponseEntity<Map<String, Object>> getInvitationByToken(@PathVariable String token) {
        TripInvitation invitation = groupTripService.getInvitationByToken(token);
        GroupTrip trip = invitation.getGroupTrip();
        User inviter = invitation.getInviterUser();
        Map<String, Object> tripData = new HashMap<>();
        tripData.put("id", trip.getId());
        tripData.put("name", trip.getName());
        tripData.put("description", trip.getDescription());
        tripData.put("startDate", trip.getStartDate());
        tripData.put("endDate", trip.getEndDate());

        Map<String, Object> inviterData = Map.of(
            "name", inviter.getName(),
            "email", inviter.getEmail()
        );
        Map<String, Object> invitationData = new HashMap<>();
        invitationData.put("status", invitation.getStatus());
        invitationData.put("inviteeEmail", invitation.getInviteeEmail());
        invitationData.put("message", invitation.getMessage());
        invitationData.put("expiresAt", invitation.getExpiresAt());
        invitationData.put("groupTrip", tripData);
        invitationData.put("inviterUser", inviterData);
        return ResponseEntity.ok(Map.of("success", true, "data", invitationData));
    }

    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @PathVariable String token, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        TravelCompanion companion = groupTripService.acceptInvitation(token, user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", companion));
    }

    @PostMapping("/invitations/{token}/decline")
    public ResponseEntity<Map<String, Object>> declineInvitation(
            @PathVariable String token, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        groupTripService.declineInvitation(token, user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/group-trips/{id}/bookings/{bookingId}")
    public ResponseEntity<Map<String, Object>> addBookingToTrip(
            @PathVariable Long id, @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        GroupTripBooking gtb = groupTripService.addBookingToTrip(id, bookingId, user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", gtb));
    }

    // ════════════════════════════════════════════════════
    // EXPENSES
    // ════════════════════════════════════════════════════

    @GetMapping("/group-trips/{id}/expenses")
    public ResponseEntity<Map<String, Object>> getExpenses(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        if (!groupTripService.isMember(id, user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a member"));
        }
        List<TripExpense> expenses = expenseService.getExpenses(id);
        Map<String, Object> summary = expenseService.getExpenseSummary(id);
        return ResponseEntity.ok(Map.of("success", true, "data", expenses, "summary", summary));
    }

    @PostMapping("/group-trips/{id}/expenses")
    public ResponseEntity<Map<String, Object>> addExpense(
            @PathVariable Long id, @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        if (!groupTripService.isMember(id, user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a member"));
        }
        List<Number> participantIds = (List<Number>) body.get("participantUserIds");
        List<Long> userIds = participantIds != null
            ? participantIds.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList())
            : null;

        List<BigDecimal> customAmounts = null;
        if (body.get("customAmounts") != null) {
            customAmounts = ((List<Number>) body.get("customAmounts")).stream()
                .map(n -> new BigDecimal(n.toString()))
                .collect(java.util.stream.Collectors.toList());
        }
        TripExpense expense = expenseService.addExpense(
            id, user.getId(),
            (String) body.get("description"),
            new BigDecimal(body.get("amount").toString()),
            (String) body.get("expenseType"),
            (String) body.getOrDefault("splitMode", "EQUAL"),
            userIds,
            body.get("totalPercentage") != null ? new BigDecimal(body.get("totalPercentage").toString()) : null,
            customAmounts
        );
        return ResponseEntity.ok(Map.of("success", true, "data", expense));
    }

    @GetMapping("/group-trips/{id}/settlements")
    public ResponseEntity<Map<String, Object>> getSettlements(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        if (!groupTripService.isMember(id, user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a member"));
        }
        List<TripSettlement> settlements = expenseService.calculateSettlements(id);
        return ResponseEntity.ok(Map.of("success", true, "data", settlements));
    }

    @PostMapping("/settlements/{id}/settle")
    public ResponseEntity<Map<String, Object>> markSettled(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = getUser(principal);
        TripSettlement settlement = expenseService.markSettled(id, user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", settlement));
    }
}
