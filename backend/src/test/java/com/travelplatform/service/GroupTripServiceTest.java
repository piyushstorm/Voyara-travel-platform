package com.travelplatform.service;

import com.travelplatform.entity.GroupTrip;
import com.travelplatform.entity.TravelCompanion;
import com.travelplatform.entity.TripInvitation;
import com.travelplatform.entity.User;
import com.travelplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupTripServiceTest {

    @Mock private GroupTripRepository groupTripRepo;
    @Mock private TravelCompanionRepository companionRepo;
    @Mock private TripInvitationRepository invitationRepo;
    @Mock private GroupTripBookingRepository groupTripBookingRepo;
    @Mock private UserRepository userRepo;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    private GroupTripService service;

    private User owner;
    private GroupTrip trip;

    @BeforeEach
    void setUp() {
        service = new GroupTripService(
                groupTripRepo,
                companionRepo,
                invitationRepo,
                groupTripBookingRepo,
                userRepo,
                notificationService,
                emailService,
                "http://localhost:5173"
        );

        owner = new User();
        owner.setId(1L);
        owner.setName("Owner User");
        owner.setEmail("owner@test.com");

        trip = new GroupTrip();
        org.springframework.test.util.ReflectionTestUtils.setField(trip, "id", 10L);
        trip.setName("Goa Getaway");
        trip.setOwner(owner);
        trip.setStartDate(LocalDate.now().plusDays(2));
        trip.setEndDate(LocalDate.now().plusDays(5));
    }

    // ─── DATE VALIDATION TESTS ───────────────────────────────────────────────

    @Test
    void createGroupTrip_pastStartDate_throwsException() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        LocalDate past = LocalDate.now().minusDays(1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.createGroupTrip("Test Trip", 1L, "Desc", past, LocalDate.now().plusDays(2))
        );
        assertEquals("Trip dates cannot be in the past.", ex.getMessage());
        verify(groupTripRepo, never()).save(any());
    }

    @Test
    void createGroupTrip_pastEndDate_throwsException() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        LocalDate past = LocalDate.now().minusDays(2);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.createGroupTrip("Test Trip", 1L, "Desc", null, past)
        );
        assertEquals("Trip dates cannot be in the past.", ex.getMessage());
        verify(groupTripRepo, never()).save(any());
    }

    @Test
    void createGroupTrip_endBeforeStart_throwsException() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(2);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.createGroupTrip("Test Trip", 1L, "Desc", start, end)
        );
        assertEquals("End date must be on or after the start date.", ex.getMessage());
        verify(groupTripRepo, never()).save(any());
    }

    @Test
    void createGroupTrip_todayStartDate_succeeds() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        LocalDate today = LocalDate.now();

        GroupTrip created = service.createGroupTrip("Today Trip", 1L, "Desc", today, today.plusDays(3));

        assertNotNull(created);
        assertEquals("Today Trip", created.getName());
        assertEquals(today, created.getStartDate());
        verify(groupTripRepo).save(any(GroupTrip.class));
        verify(companionRepo).save(any(TravelCompanion.class));
    }

    @Test
    void createGroupTrip_sameDayTrip_succeeds() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        LocalDate today = LocalDate.now();

        GroupTrip created = service.createGroupTrip("Day Trip", 1L, "Desc", today, today);

        assertNotNull(created);
        assertEquals(today, created.getStartDate());
        assertEquals(today, created.getEndDate());
        verify(groupTripRepo).save(any(GroupTrip.class));
    }

    // ─── INVITATION & EMAIL TESTS ────────────────────────────────────────────

    @Test
    void inviteCompanion_nonOwner_throwsException() {
        when(groupTripRepo.findById(10L)).thenReturn(Optional.of(trip));
        User nonOwner = new User();
        nonOwner.setId(2L);
        when(userRepo.findById(2L)).thenReturn(Optional.of(nonOwner));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.inviteCompanion(10L, 2L, "guest@test.com", "Join us!")
        );
        assertEquals("Only the trip owner can invite companions", ex.getMessage());
    }

    @Test
    void inviteCompanion_invalidEmail_throwsException() {
        when(groupTripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));

        RuntimeException ex1 = assertThrows(RuntimeException.class, () ->
                service.inviteCompanion(10L, 1L, "", "Join us!")
        );
        assertEquals("Enter an email address.", ex1.getMessage());

        RuntimeException ex2 = assertThrows(RuntimeException.class, () ->
                service.inviteCompanion(10L, 1L, "invalid-email", "Join us!")
        );
        assertEquals("Enter a valid email address.", ex2.getMessage());
    }

    @Test
    void inviteCompanion_selfInvite_throwsException() {
        when(groupTripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.inviteCompanion(10L, 1L, "owner@test.com", "Join!")
        );
        assertEquals("You cannot invite yourself to your own trip", ex.getMessage());
    }

    @Test
    void inviteCompanion_alreadyAcceptedMember_throwsException() {
        when(groupTripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));

        User companionUser = new User();
        companionUser.setEmail("member@test.com");
        TravelCompanion companion = new TravelCompanion();
        companion.setUser(companionUser);
        when(companionRepo.findByGroupTripId(10L)).thenReturn(List.of(companion));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.inviteCompanion(10L, 1L, "member@test.com", "Join!")
        );
        assertEquals("User is already a member of this trip", ex.getMessage());
    }

    @Test
    void inviteCompanion_smtpFailure_marksFailedAndThrowsSafeError() {
        when(groupTripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        when(companionRepo.findByGroupTripId(10L)).thenReturn(Collections.emptyList());
        when(invitationRepo.findByGroupTripIdAndInviteeEmail(10L, "friend@test.com")).thenReturn(Collections.emptyList());
        when(userRepo.findByEmail("friend@test.com")).thenReturn(Optional.empty());

        when(invitationRepo.save(any(TripInvitation.class))).thenAnswer(i -> {
            TripInvitation inv = i.getArgument(0);
            return inv;
        });

        // Simulate SMTP failure (e.g. Gmail 535 BadCredentials or connection failure)
        doThrow(new RuntimeException("Gmail SMTP error: 535 BadCredentials"))
                .when(emailService).sendGroupTripInvitationEmail(eq("friend@test.com"), anyMap());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.inviteCompanion(10L, 1L, "friend@test.com", "Join us!")
        );

        assertEquals("Unable to send invitation email. Please try again.", ex.getMessage());

        // Verify status was persisted as FAILED
        ArgumentCaptor<TripInvitation> captor = ArgumentCaptor.forClass(TripInvitation.class);
        verify(invitationRepo, atLeast(2)).save(captor.capture());
        TripInvitation lastSaved = captor.getValue();
        assertEquals("FAILED", lastSaved.getStatus());
        assertNull(lastSaved.getSentAt());
    }

    @Test
    void inviteCompanion_smtpSuccess_marksSentAndPopulatesSentAt() {
        when(groupTripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(1L)).thenReturn(Optional.of(owner));
        when(companionRepo.findByGroupTripId(10L)).thenReturn(Collections.emptyList());
        when(invitationRepo.findByGroupTripIdAndInviteeEmail(10L, "friend@test.com")).thenReturn(Collections.emptyList());
        when(userRepo.findByEmail("friend@test.com")).thenReturn(Optional.empty());

        when(invitationRepo.save(any(TripInvitation.class))).thenAnswer(i -> i.getArgument(0));

        // SMTP succeeds
        doNothing().when(emailService).sendGroupTripInvitationEmail(eq("friend@test.com"), anyMap());

        TripInvitation result = service.inviteCompanion(10L, 1L, "friend@test.com", "Join us!");

        assertNotNull(result);
        assertEquals("SENT", result.getStatus());
        assertNotNull(result.getSentAt());
        assertNotNull(result.getToken());
        assertTrue(result.getToken().length() >= 32);
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now()));

        verify(emailService).sendGroupTripInvitationEmail(eq("friend@test.com"), anyMap());
    }
}
