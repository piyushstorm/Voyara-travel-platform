package com.travelplatform.service;

import com.travelplatform.entity.Seat;
import com.travelplatform.entity.SeatHold;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.SeatHoldRepository;
import com.travelplatform.repository.SeatRepository;
import com.travelplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatSelectionServiceTest {

    @Mock private SeatRepository seatRepo;
    @Mock private SeatHoldRepository holdRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private SeatSelectionService seatService;

    private User testUser;
    private Seat availableSeat;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        availableSeat = new Seat();
        availableSeat.setSeatNumber("1A");
        availableSeat.setCabinClass("ECONOMY");
        availableSeat.setAvailable(true);
        availableSeat.setPrice(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("Successfully hold an available seat")
    void testHoldAvailableSeat() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSeat));
        when(holdRepo.findBySeatIdAndUserIdAndStatus(100L, 1L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(holdRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeatHold hold = seatService.holdSeat(100L, 1L);

        assertNotNull(hold);
        assertEquals("ACTIVE", hold.getStatus());
        assertEquals("1A", hold.getSeat().getSeatNumber());
        assertEquals("test@test.com", hold.getUser().getEmail());
        assertNotNull(hold.getExpiresAt());
        assertTrue(hold.getExpiresAt().isAfter(LocalDateTime.now()));

        // Verify seat was updated with hold info
        verify(seatRepo).save(availableSeat);
        assertEquals("1", availableSeat.getHeldByUserId());
        assertNotNull(availableSeat.getHeldUntil());
    }

    @Test
    @DisplayName("Cannot hold an already booked seat")
    void testCannotHoldBookedSeat() {
        Seat bookedSeat = new Seat();
        bookedSeat.setAvailable(false);
        bookedSeat.setSeatNumber("1A");

        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(bookedSeat));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> seatService.holdSeat(100L, 1L));
        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    @DisplayName("Cannot hold a seat held by another user")
    void testCannotHoldSeatHeldByOther() {
        Seat heldSeat = new Seat();
        heldSeat.setAvailable(true);
        heldSeat.setSeatNumber("1A");
        heldSeat.setHeldByUserId("99"); // Different user
        heldSeat.setHeldUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(heldSeat));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> seatService.holdSeat(100L, 1L));
        assertTrue(ex.getMessage().contains("held by another user"));
    }

    @Test
    @DisplayName("Can re-hold a seat if previous hold by same user expired")
    void testCanReholdAfterExpiry() {
        Seat expiredHoldSeat = new Seat();
        expiredHoldSeat.setAvailable(true);
        expiredHoldSeat.setSeatNumber("1A");
        expiredHoldSeat.setHeldByUserId("1"); // Same user
        expiredHoldSeat.setHeldUntil(LocalDateTime.now().minusMinutes(1)); // Expired

        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(expiredHoldSeat));
        when(holdRepo.findBySeatIdAndUserIdAndStatus(100L, 1L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(holdRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeatHold hold = seatService.holdSeat(100L, 1L);
        assertNotNull(hold);
        assertEquals("ACTIVE", hold.getStatus());
    }

    @Test
    @DisplayName("Release hold clears seat state")
    void testReleaseHold() {
        Seat heldSeat = new Seat();
        heldSeat.setAvailable(true);
        heldSeat.setSeatNumber("1A");
        heldSeat.setHeldByUserId("1");
        heldSeat.setHeldUntil(LocalDateTime.now().plusMinutes(5));

        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(heldSeat));
        SeatHold activeHold = new SeatHold();
        activeHold.setStatus("ACTIVE");
        when(holdRepo.findBySeatIdAndUserIdAndStatus(100L, 1L, "ACTIVE"))
                .thenReturn(Optional.of(activeHold));

        seatService.releaseHold(100L, 1L);

        assertNull(heldSeat.getHeldByUserId());
        assertNull(heldSeat.getHeldUntil());
        verify(seatRepo).save(heldSeat);
        assertEquals("RELEASED", activeHold.getStatus());
    }

    @Test
    @DisplayName("Confirm seat transitions hold to booked")
    void testConfirmSeat() {
        Seat heldSeat = new Seat();
        heldSeat.setAvailable(true);
        heldSeat.setSeatNumber("1A");
        heldSeat.setHeldByUserId("1");
        heldSeat.setHeldUntil(LocalDateTime.now().plusMinutes(5));

        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(heldSeat));
        SeatHold activeHold = new SeatHold();
        activeHold.setStatus("ACTIVE");
        when(holdRepo.findBySeatIdAndUserIdAndStatus(100L, 1L, "ACTIVE"))
                .thenReturn(Optional.of(activeHold));

        Seat confirmed = seatService.confirmSeat(100L, 1L);

        assertFalse(confirmed.isAvailable());
        assertNull(confirmed.getHeldByUserId());
        assertNull(confirmed.getHeldUntil());
        assertEquals("CONFIRMED", activeHold.getStatus());
    }

    @Test
    @DisplayName("Cannot confirm seat held by another user")
    void testCannotConfirmSeatHeldByOther() {
        Seat heldSeat = new Seat();
        heldSeat.setAvailable(true);
        heldSeat.setSeatNumber("1A");
        heldSeat.setHeldByUserId("99"); // Different user
        heldSeat.setHeldUntil(LocalDateTime.now().plusMinutes(5));

        when(seatRepo.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(heldSeat));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> seatService.confirmSeat(100L, 1L));
        assertTrue(ex.getMessage().contains("held by another user"));
    }

    @Test
    @DisplayName("Seat.isHeld() returns false for expired holds")
    void testExpiredHoldNotHeld() {
        Seat seat = new Seat();
        seat.setAvailable(true);
        seat.setHeldByUserId("1");
        seat.setHeldUntil(LocalDateTime.now().minusMinutes(1));

        assertFalse(seat.isHeld(), "Expired hold should not count as held");
        assertTrue(seat.isBookable(), "Seat with expired hold should be bookable");
    }

    @Test
    @DisplayName("Seat.isHeld() returns true for active holds")
    void testActiveHoldIsHeld() {
        Seat seat = new Seat();
        seat.setAvailable(true);
        seat.setHeldByUserId("1");
        seat.setHeldUntil(LocalDateTime.now().plusMinutes(10));

        assertTrue(seat.isHeld(), "Active hold should count as held");
        assertFalse(seat.isBookable(), "Seat with active hold should not be bookable");
    }
}
