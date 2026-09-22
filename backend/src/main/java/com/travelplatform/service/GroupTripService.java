package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Base64;

/**
 * Group Trip / Travel Companions — manages shared trips and invitations.
 *
 * Email delivery is async: inviteCompanion() saves the invitation synchronously
 * (instant HTTP response), then sendInvitationEmailAsync() fires on a background
 * thread so SMTP latency never blocks a Tomcat request thread.
 */
@Service
public class GroupTripService {

    private static final Logger logger = LoggerFactory.getLogger(GroupTripService.class);

    private final GroupTripRepository groupTripRepo;
    private final TravelCompanionRepository companionRepo;
    private final TripInvitationRepository invitationRepo;
    private final GroupTripBookingRepository groupTripBookingRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public GroupTripService(GroupTripRepository groupTripRepo,
                            TravelCompanionRepository companionRepo,
                            TripInvitationRepository invitationRepo,
                            GroupTripBookingRepository groupTripBookingRepo,
                            UserRepository userRepo,
                            NotificationService notificationService,
                            EmailService emailService,
                            @Value("${frontend.base.url:http://localhost:5173}") String frontendBaseUrl) {
        this.groupTripRepo = groupTripRepo;
        this.companionRepo = companionRepo;
        this.invitationRepo = invitationRepo;
        this.groupTripBookingRepo = groupTripBookingRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    // -------------------------------------------------------------------------
    // Trip CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public GroupTrip createGroupTrip(String name, Long ownerId, String description,
                                      LocalDate startDate, LocalDate endDate) {
        User owner = userRepo.findById(ownerId).orElseThrow();

        // Server-side date validation
        LocalDate today = LocalDate.now();
        if (startDate != null && startDate.isBefore(today)) {
            throw new RuntimeException("Trip dates cannot be in the past.");
        }
        if (endDate != null && endDate.isBefore(today)) {
            throw new RuntimeException("Trip dates cannot be in the past.");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new RuntimeException("End date must be on or after the start date.");
        }

        GroupTrip trip = new GroupTrip();
        trip.setName(name);
        trip.setOwner(owner);
        trip.setDescription(description);
        trip.setStartDate(startDate);
        trip.setEndDate(endDate);
        groupTripRepo.save(trip);

        // Add owner as companion with OWNER role
        TravelCompanion ownerCompanion = new TravelCompanion();
        ownerCompanion.setGroupTrip(trip);
        ownerCompanion.setUser(owner);
        ownerCompanion.setRole("OWNER");
        ownerCompanion.setStatus("ACCEPTED");
        ownerCompanion.setJoinedAt(LocalDateTime.now());
        companionRepo.save(ownerCompanion);

        return trip;
    }

    @Transactional(readOnly = true)
    public List<GroupTrip> getUserTrips(Long userId) {
        List<GroupTrip> owned = groupTripRepo.findByOwnerIdOrderByCreatedAtDesc(userId);
        List<GroupTrip> memberOf = groupTripRepo.findByCompanionsUserIdOrderByCreatedAtDesc(userId);
        Set<Long> seen = new HashSet<>();
        List<GroupTrip> result = new ArrayList<>();
        for (GroupTrip t : owned) { if (seen.add(t.getId())) result.add(t); }
        for (GroupTrip t : memberOf) { if (seen.add(t.getId())) result.add(t); }
        return result;
    }

    @Transactional(readOnly = true)
    public GroupTrip getGroupTrip(Long tripId) {
        return groupTripRepo.findById(tripId).orElseThrow();
    }

    // -------------------------------------------------------------------------
    // Invitations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TripInvitation getInvitationByToken(String token) {
        TripInvitation invitation = invitationRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid invitation"));
        invitation.getGroupTrip().getName();
        invitation.getInviterUser().getEmail();
        return invitation;
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Validates ownership, email, and duplicates, persists the invitation,
     * and executes synchronous delivery verification via EmailService.
     * Does NOT fake success if SMTP delivery fails.
     */
    @Transactional(noRollbackFor = RuntimeException.class)
    public TripInvitation inviteCompanion(Long tripId, Long inviterId, String inviteeEmail, String message) {
        GroupTrip trip = groupTripRepo.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));
        User inviter = userRepo.findById(inviterId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Group Trip ownership check (prevent IDOR)
        if (!trip.getOwner().getId().equals(inviterId)) {
            throw new RuntimeException("Only the trip owner can invite companions");
        }

        // 2. Email validation & normalization
        if (inviteeEmail == null || inviteeEmail.trim().isBlank()) {
            throw new RuntimeException("Enter an email address.");
        }
        String normalizedEmail = inviteeEmail.trim().toLowerCase(Locale.ROOT);
        if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("Enter a valid email address.");
        }

        // 3. Self-invite prevention
        if (inviter.getEmail() != null && inviter.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("You cannot invite yourself to your own trip");
        }

        // 4. Check if user is already an accepted companion on this trip
        boolean alreadyMember = companionRepo.findByGroupTripId(tripId).stream()
            .anyMatch(c -> c.getUser() != null && normalizedEmail.equalsIgnoreCase(c.getUser().getEmail()));
        if (alreadyMember) {
            throw new RuntimeException("User is already a member of this trip");
        }

        // 5. Check existing invitations for this email on this trip
        List<TripInvitation> existingInvites = invitationRepo.findByGroupTripIdAndInviteeEmail(tripId, normalizedEmail);
        for (TripInvitation inv : existingInvites) {
            if ("ACCEPTED".equals(inv.getStatus())) {
                throw new RuntimeException("User has already accepted an invitation to this trip");
            }
        }

        // Reuse an existing PENDING or FAILED invitation, or create a new one
        TripInvitation invitation = existingInvites.stream()
            .filter(i -> "PENDING".equals(i.getStatus()) || "FAILED".equals(i.getStatus()))
            .findFirst()
            .orElseGet(TripInvitation::new);

        invitation.setGroupTrip(trip);
        invitation.setInviterUser(inviter);
        invitation.setInviteeEmail(normalizedEmail);
        invitation.setToken(generateSecureToken());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setMessage(message);
        invitation.setStatus("PENDING");
        userRepo.findByEmail(normalizedEmail).ifPresent(invitation::setInviteeUser);
        TripInvitation saved = invitationRepo.save(invitation);

        // 6. Build template variables
        String acceptUrl  = frontendBaseUrl + "/group-invitations/" + saved.getToken();
        String declineUrl = frontendBaseUrl + "/group-invitations/" + saved.getToken() + "?action=decline";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String memberCount = String.valueOf(companionRepo.findByGroupTripId(tripId).size() + 1);

        Map<String, String> vars = new HashMap<>();
        vars.put("inviterName",     inviter.getName());
        vars.put("inviterEmail",    inviter.getEmail());
        vars.put("tripName",        trip.getName());
        vars.put("destination",     trip.getName());
        vars.put("startDate",       trip.getStartDate() != null ? trip.getStartDate().format(fmt) : "TBD");
        vars.put("endDate",         trip.getEndDate()   != null ? trip.getEndDate().format(fmt)   : "TBD");
        vars.put("memberCount",     memberCount);
        vars.put("acceptUrl",       acceptUrl);
        vars.put("declineUrl",      declineUrl);
        vars.put("expiryDate",      saved.getExpiresAt().format(fmt));
        vars.put("tripDescription", trip.getDescription() != null ? trip.getDescription() : "Plan your next getaway together.");

        String subject = "You're invited to join " + trip.getName() + " on Voyara";

        // 7. REAL DELIVERY VERIFICATION
        // Attempt email delivery. If SMTP fails, record status as FAILED, log diagnostic info safely,
        // and throw safe user-facing exception.
        try {
            emailService.sendGroupTripInvitationEmail(normalizedEmail, vars);
            saved.setStatus("SENT");
            saved.setSentAt(LocalDateTime.now());
            TripInvitation sent = invitationRepo.save(saved);
            logger.info("Invitation email successfully delivered to {} for trip {}", maskEmail(normalizedEmail), tripId);
            return sent;
        } catch (Exception e) {
            logger.error("SMTP delivery failed for invitation {} to {}: {}", saved.getId(), maskEmail(normalizedEmail), e.getMessage());
            saved.setStatus("FAILED");
            invitationRepo.save(saved);
            throw new RuntimeException("Unable to send invitation email. Please try again.");
        }
    }

    // -------------------------------------------------------------------------
    // Invitation acceptance / decline
    // -------------------------------------------------------------------------

    @Transactional
    public TravelCompanion acceptInvitation(String token, Long userId) {
        TripInvitation invitation = invitationRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid invitation"));

        User user = userRepo.findById(userId).orElseThrow();
        if (!user.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new RuntimeException("This invitation is for a different email address");
        }
        if (!"PENDING".equals(invitation.getStatus()) && !"SENT".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation already processed");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepo.save(invitation);
            throw new RuntimeException("Invitation expired");
        }

        if (companionRepo.existsByGroupTripIdAndUserId(invitation.getGroupTrip().getId(), userId)) {
            throw new RuntimeException("Already a member of this trip");
        }

        invitation.setStatus("ACCEPTED");
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setInviteeUser(user);
        invitationRepo.save(invitation);

        TravelCompanion companion = new TravelCompanion();
        companion.setGroupTrip(invitation.getGroupTrip());
        companion.setUser(user);
        companion.setRole("COMPANION");
        companion.setStatus("ACCEPTED");
        companion.setJoinedAt(LocalDateTime.now());
        return companionRepo.save(companion);
    }

    @Transactional
    public void declineInvitation(String token, Long userId) {
        TripInvitation invitation = invitationRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid invitation"));
        User user = userRepo.findById(userId).orElseThrow();
        if (!user.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new RuntimeException("This invitation is for a different email address");
        }
        if ("DECLINED".equals(invitation.getStatus()) || "ACCEPTED".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation already processed");
        }
        invitation.setStatus("DECLINED");
        invitation.setDeclinedAt(LocalDateTime.now());
        invitationRepo.save(invitation);
    }

    // -------------------------------------------------------------------------
    // Companions
    // -------------------------------------------------------------------------

    @Transactional
    public void removeCompanion(Long tripId, Long companionUserId, Long ownerId) {
        GroupTrip trip = groupTripRepo.findById(tripId).orElseThrow();
        if (!trip.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Only the owner can remove companions");
        }
        TravelCompanion companion = companionRepo.findByGroupTripIdAndUserId(tripId, companionUserId)
            .orElseThrow(() -> new RuntimeException("Companion not found"));
        if ("OWNER".equals(companion.getRole())) {
            throw new RuntimeException("Cannot remove the owner");
        }
        companionRepo.delete(companion);
    }

    @Transactional(readOnly = true)
    public List<TravelCompanion> getCompanions(Long tripId) {
        return companionRepo.findByGroupTripId(tripId);
    }

    // -------------------------------------------------------------------------
    // Trip bookings
    // -------------------------------------------------------------------------

    @Transactional
    public GroupTripBooking addBookingToTrip(Long tripId, Long bookingId, Long userId) {
        GroupTrip trip = groupTripRepo.findById(tripId).orElseThrow();
        User user = userRepo.findById(userId).orElseThrow();

        if (!companionRepo.existsByGroupTripIdAndUserId(tripId, userId)) {
            throw new RuntimeException("You are not a member of this trip");
        }

        GroupTripBooking existing = groupTripBookingRepo.findByGroupTripIdAndBookingId(tripId, bookingId).orElse(null);
        if (existing != null) {
            throw new RuntimeException("Booking already added to this trip");
        }

        GroupTripBooking gtb = new GroupTripBooking();
        gtb.setGroupTrip(trip);
        gtb.setBookingId(bookingId);
        gtb.setAddedByUserId(userId);
        return groupTripBookingRepo.save(gtb);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long tripId, Long userId) {
        GroupTrip trip = groupTripRepo.findById(tripId).orElseThrow();
        return trip.getOwner().getId().equals(userId);
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long tripId, Long userId) {
        return companionRepo.existsByGroupTripIdAndUserId(tripId, userId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at);
    }
}
