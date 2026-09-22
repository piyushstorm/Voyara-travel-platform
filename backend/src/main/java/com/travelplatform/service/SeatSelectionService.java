package com.travelplatform.service;

import com.travelplatform.dto.selection.SeatDto;
import com.travelplatform.dto.selection.SeatHoldResponseDto;
import com.travelplatform.dto.selection.SeatMapResponseDto;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Seat;
import com.travelplatform.entity.SeatHold;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserTravelPreference;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.exception.SeatConflictException;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.SeatHoldRepository;
import com.travelplatform.repository.SeatRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.repository.UserTravelPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SeatSelectionService {

    private static final Logger logger = LoggerFactory.getLogger(SeatSelectionService.class);

    private static final int HOLD_DURATION_MINUTES = 10;

    private final SeatRepository seatRepo;
    private final SeatHoldRepository holdRepo;
    private final UserRepository userRepo;
    private final FlightRepository flightRepo;
    private final UserTravelPreferenceRepository preferenceRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public SeatSelectionService(SeatRepository seatRepo,
                                SeatHoldRepository holdRepo,
                                UserRepository userRepo,
                                FlightRepository flightRepo,
                                UserTravelPreferenceRepository preferenceRepo,
                                @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.seatRepo = seatRepo;
        this.holdRepo = holdRepo;
        this.userRepo = userRepo;
        this.flightRepo = flightRepo;
        this.preferenceRepo = preferenceRepo;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Get seat map for a flight and cabin class with personalized recommendations and typed response.
     */
    @Transactional
    public SeatMapResponseDto getSeatMapDto(Long flightId, String cabinClass, Long userId) {
        Flight flight = flightRepo.findById(flightId).orElse(null);
        String flightNumber = flight != null ? flight.getFlightNumber() : "FL-" + flightId;

        List<Seat> seats = seatRepo.findByFlightIdAndCabinClassOrderByRowNumberAscColumnLetterAsc(flightId, cabinClass);
        if (seats.isEmpty() && flight != null) {
            seats = initializeSeatsForFlight(flight, cabinClass);
        }

        UserTravelPreference preference = userId != null
                ? preferenceRepo.findByUserId(userId).orElse(null)
                : null;

        List<SeatHold> userActiveHolds = userId != null
                ? holdRepo.findByUserIdAndStatus(userId, "ACTIVE")
                : List.of();
        Set<Long> userHoldSeatIds = new HashSet<>();
        for (SeatHold sh : userActiveHolds) {
            if (!sh.isExpired() && sh.getSeat() != null) {
                userHoldSeatIds.add(sh.getSeat().getId());
            }
        }

        List<SeatDto> seatDtos = new ArrayList<>();
        int availableCount = 0;
        int heldCount = 0;
        int bookedCount = 0;

        for (Seat seat : seats) {
            boolean isHeld = seat.isHeld();
            boolean isBooked = !seat.isAvailable();
            String status;

            if (userHoldSeatIds.contains(seat.getId())) {
                status = "SELECTED";
                heldCount++;
            } else if (isBooked) {
                status = "BOOKED";
                bookedCount++;
            } else if (isHeld) {
                status = "HELD";
                heldCount++;
            } else {
                status = "AVAILABLE";
                availableCount++;
            }

            // Check personalized preference match
            boolean match = false;
            String matchReason = null;

            if (preference != null && "AVAILABLE".equals(status)) {
                String posPref = preference.getPreferredSeatPosition(); // WINDOW, AISLE, MIDDLE, ANY
                String typePref = preference.getPreferredSeatType(); // STANDARD, PREMIUM, EXTRA_LEGROOM

                boolean posMatch = ("WINDOW".equalsIgnoreCase(posPref) && seat.isWindow()) ||
                        ("AISLE".equalsIgnoreCase(posPref) && seat.isAisle()) ||
                        ("MIDDLE".equalsIgnoreCase(posPref) && seat.isMiddle());

                boolean typeMatch = typePref != null && typePref.equalsIgnoreCase(seat.getSeatType());

                if (posMatch && typeMatch) {
                    match = true;
                    matchReason = "Matches your preference for " + posPref.toLowerCase() + " & " + typePref.toLowerCase() + " seat";
                } else if (posMatch) {
                    match = true;
                    matchReason = "Matches your preference: " + posPref.toLowerCase() + " seat";
                }
            }

            seatDtos.add(new SeatDto(
                    seat.getId(),
                    seat.getSeatNumber(),
                    seat.getRowNumber(),
                    seat.getColumnLetter(),
                    seat.isWindow(),
                    seat.isAisle(),
                    seat.isMiddle(),
                    seat.isExtraLegroom(),
                    seat.isEmergencyExit(),
                    seat.getSeatType(),
                    seat.getPrice(),
                    seat.getPremiumSurcharge(),
                    seat.getCurrency(),
                    status,
                    match,
                    matchReason
            ));
        }

        Map<String, Integer> summary = Map.of(
                "total", seats.size(),
                "available", availableCount,
                "held", heldCount,
                "booked", bookedCount
        );

        String prefSummary = preference != null
                ? "Preferred: " + preference.getPreferredSeatPosition() + " (" + preference.getPreferredSeatType() + ")"
                : null;

        return new SeatMapResponseDto(flightId, flightNumber, cabinClass, seatDtos, summary, prefSummary, new ArrayList<>(userHoldSeatIds));
    }

    /**
     * Backward-compatible map response.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSeatMap(Long flightId, String cabinClass) {
        SeatMapResponseDto dto = getSeatMapDto(flightId, cabinClass, null);
        List<Map<String, Object>> seatList = dto.getSeats().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("seatNumber", s.getSeatNumber());
            m.put("row", s.getRow());
            m.put("column", s.getColumn());
            m.put("window", s.isWindow());
            m.put("aisle", s.isAisle());
            m.put("middle", s.isMiddle());
            m.put("extraLegroom", s.isExtraLegroom());
            m.put("emergencyExit", s.isEmergencyExit());
            m.put("seatType", s.getSeatType());
            m.put("price", s.getPrice());
            m.put("premiumSurcharge", s.getPremiumSurcharge());
            m.put("totalPrice", s.getTotalPrice());
            m.put("currency", s.getCurrency());
            m.put("status", s.getStatus());
            m.put("cabinClass", cabinClass);
            m.put("preferenceMatch", s.isPreferenceMatch());
            m.put("matchReason", s.getMatchReason());
            return m;
        }).toList();

        return Map.of(
                "seats", seatList,
                "summary", dto.getSummary()
        );
    }

    /**
     * Hold a seat with PESSIMISTIC_WRITE lock and broadcast update event.
     */
    @Transactional
    public SeatHold holdSeat(Long seatId, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Seat seat = seatRepo.findByIdWithPessimisticLock(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));

        // Check if seat is already booked
        if (!seat.isAvailable()) {
            throw new SeatConflictException(seat.getId(), seat.getSeatNumber(), "Seat " + seat.getSeatNumber() + " is already booked");
        }

        // Check if seat is currently held by another user
        if (seat.isHeld() && !String.valueOf(userId).equals(seat.getHeldByUserId())) {
            throw new SeatConflictException(seat.getId(), seat.getSeatNumber(), "Seat " + seat.getSeatNumber() + " is currently held by another user");
        }

        // Clean up any expired or stale holds on this seat prior to new hold acquisition
        List<SeatHold> priorActiveHolds = holdRepo.findBySeatIdAndStatus(seatId, "ACTIVE");
        for (SeatHold sh : priorActiveHolds) {
            if (sh.isExpired() || !sh.getUser().getId().equals(userId)) {
                sh.setStatus("EXPIRED");
                holdRepo.save(sh);
            }
        }

        // Idempotent: check if user already has an active hold on this seat
        Optional<SeatHold> existingHoldOpt = holdRepo.findBySeatIdAndUserIdAndStatus(seatId, userId, "ACTIVE");
        if (existingHoldOpt.isPresent()) {
            SeatHold existingHold = existingHoldOpt.get();
            if (!existingHold.isExpired()) {
                // Refresh expiration time idempotently
                existingHold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_DURATION_MINUTES));
                seat.setHeldUntil(existingHold.getExpiresAt());
                seatRepo.save(seat);
                return holdRepo.save(existingHold);
            } else {
                existingHold.setStatus("RELEASED");
                holdRepo.save(existingHold);
            }
        }

        // Create new hold
        SeatHold hold = new SeatHold(seat, user, HOLD_DURATION_MINUTES);

        seat.setHeldByUserId(String.valueOf(userId));
        seat.setHeldUntil(hold.getExpiresAt());
        seatRepo.save(seat);

        hold = holdRepo.save(hold);

        logger.info("Seat {} held by user {} until {} (holdId={})",
                seat.getSeatNumber(), userId, hold.getExpiresAt(), hold.getId());

        broadcastSeatUpdate(seat.getFlight() != null ? seat.getFlight().getId() : null, seat.getId(), "HELD");

        return hold;
    }

    @Transactional
    public SeatHoldResponseDto holdSeatDto(Long seatId, Long userId) {
        SeatHold hold = holdSeat(seatId, userId);
        long remaining = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), hold.getExpiresAt()));
        Seat seat = hold.getSeat();
        BigDecimal price = seat.getPrice() != null ? seat.getPrice() : BigDecimal.ZERO;
        BigDecimal surcharge = seat.getPremiumSurcharge() != null ? seat.getPremiumSurcharge() : BigDecimal.ZERO;
        BigDecimal total = price.add(surcharge);
        return new SeatHoldResponseDto(
                hold.getId(),
                seat.getId(),
                seat.getSeatNumber(),
                seat.getCabinClass(),
                hold.getExpiresAt(),
                remaining,
                hold.getStatus(),
                price,
                surcharge,
                total
        );
    }

    /**
     * Release a seat hold (user cancels selection or hold expires).
     */
    @Transactional
    public void releaseHold(Long seatId, Long userId) {
        Seat seat = seatRepo.findByIdWithPessimisticLock(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));

        if (seat.getHeldByUserId() != null && seat.getHeldByUserId().equals(String.valueOf(userId))) {
            seat.setHeldByUserId(null);
            seat.setHeldUntil(null);
            seatRepo.save(seat);

            holdRepo.findBySeatIdAndUserIdAndStatus(seatId, userId, "ACTIVE")
                    .ifPresent(hold -> {
                        hold.setStatus("RELEASED");
                        holdRepo.save(hold);
                    });

            logger.info("Seat {} released by user {}", seat.getSeatNumber(), userId);
            broadcastSeatUpdate(seat.getFlight() != null ? seat.getFlight().getId() : null, seat.getId(), "AVAILABLE");
        }
    }

    /**
     * Confirm seat selection during booking — converts hold to booked.
     */
    @Transactional
    public Seat confirmSeat(Long seatId, Long userId) {
        Seat seat = seatRepo.findByIdWithPessimisticLock(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));

        if (!seat.isAvailable()) {
            throw new SeatConflictException(seat.getId(), seat.getSeatNumber(), "Seat " + seat.getSeatNumber() + " is no longer available");
        }

        if (seat.isHeld() && !userId.toString().equals(seat.getHeldByUserId())) {
            throw new SeatConflictException(seat.getId(), seat.getSeatNumber(), "Seat " + seat.getSeatNumber() + " is held by another user");
        }

        seat.setAvailable(false);
        seat.setHeldByUserId(null);
        seat.setHeldUntil(null);
        seatRepo.save(seat);

        holdRepo.findBySeatIdAndUserIdAndStatus(seatId, userId, "ACTIVE")
                .ifPresent(hold -> {
                    hold.setStatus("CONFIRMED");
                    holdRepo.save(hold);
                });

        logger.info("Seat {} confirmed for user {}", seat.getSeatNumber(), userId);
        broadcastSeatUpdate(seat.getFlight() != null ? seat.getFlight().getId() : null, seat.getId(), "BOOKED");
        return seat;
    }

    @Transactional
    public List<Seat> confirmSeats(List<Long> seatIds, Long userId) {
        List<Long> sortedIds = seatIds.stream().sorted().toList();
        return sortedIds.stream()
                .map(id -> confirmSeat(id, userId))
                .toList();
    }

    /**
     * Release all expired holds every 15 seconds.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void releaseExpiredHolds() {
        List<SeatHold> expiredHolds = holdRepo.findExpiredHolds();
        int released = 0;

        for (SeatHold hold : expiredHolds) {
            Seat seat = hold.getSeat();
            if (seat != null && seat.getHeldByUserId() != null) {
                seat.setHeldByUserId(null);
                seat.setHeldUntil(null);
                seatRepo.save(seat);
                broadcastSeatUpdate(seat.getFlight() != null ? seat.getFlight().getId() : null, seat.getId(), "AVAILABLE");
            }
            hold.setStatus("EXPIRED");
            holdRepo.save(hold);
            released++;
        }

        if (released > 0) {
            logger.info("Released {} expired seat holds", released);
        }
    }

    @Transactional(readOnly = true)
    public List<SeatHold> getUserActiveHolds(Long userId) {
        return holdRepo.findByUserIdAndStatus(userId, "ACTIVE");
    }

    @Transactional
    public List<Seat> initializeSeatsForFlight(Flight flight, String cabinClass) {
        // Double check within transaction to prevent duplicates
        List<Seat> existing = seatRepo.findByFlightIdAndCabinClassOrderByRowNumberAscColumnLetterAsc(flight.getId(), cabinClass);
        if (!existing.isEmpty()) {
            return existing;
        }

        List<Seat> seatBatch = new ArrayList<>(160);
        Random rng = new Random(flight.getId() * 31);
        String[] columns = {"A", "B", "C", "D", "E", "F"};

        // Rows 1-4: Business (wider layout A, C, D, F)
        for (int row = 1; row <= 4; row++) {
            for (String col : new String[]{"A", "C", "D", "F"}) {
                seatBatch.add(createSeatRecord(flight, "BUSINESS", row, col, rng));
            }
        }

        // Rows 5-7: Premium Economy (A, B, C, D, E, F)
        for (int row = 5; row <= 7; row++) {
            for (String col : columns) {
                seatBatch.add(createSeatRecord(flight, "PREMIUM_ECONOMY", row, col, rng));
            }
        }

        // Rows 8-28: Economy (A, B, C, D, E, F)
        for (int row = 8; row <= 28; row++) {
            for (String col : columns) {
                seatBatch.add(createSeatRecord(flight, "ECONOMY", row, col, rng));
            }
        }

        seatRepo.saveAll(seatBatch);
        logger.info("Auto-initialized {} seats for flight {} ({})", seatBatch.size(), flight.getId(), flight.getFlightNumber());

        return seatRepo.findByFlightIdAndCabinClassOrderByRowNumberAscColumnLetterAsc(flight.getId(), cabinClass);
    }

    private Seat createSeatRecord(Flight flight, String cabinClass, int row, String col, Random rng) {
        Seat seat = new Seat();
        seat.setFlight(flight);
        seat.setCabinClass(cabinClass);
        seat.setRowNumber(row);
        seat.setColumnLetter(col);
        seat.setSeatNumber(row + col);

        boolean isWin = "A".equals(col) || "F".equals(col);
        boolean isAisle = "C".equals(col) || "D".equals(col);
        boolean isMid = "B".equals(col) || "E".equals(col);
        seat.setWindow(isWin);
        seat.setAisle(isAisle);
        seat.setMiddle(isMid);

        boolean isExit = (row == 12 || row == 13);
        boolean isExtra = (row == 1 || row == 8 || row == 12 || row == 13);
        seat.setEmergencyExit(isExit);
        seat.setExtraLegroom(isExtra);

        int basePrice = switch (cabinClass) {
            case "ECONOMY" -> 350 + rng.nextInt(200);
            case "PREMIUM_ECONOMY" -> 900 + rng.nextInt(400);
            case "BUSINESS" -> 2500 + rng.nextInt(1000);
            case "FIRST" -> 6000 + rng.nextInt(2000);
            default -> 350;
        };
        if (seat.isWindow() || seat.isAisle()) basePrice += 200;
        seat.setPrice(BigDecimal.valueOf(basePrice));

        if (isExtra) {
            seat.setSeatType("EXTRA_LEGROOM");
            seat.setPremiumSurcharge(BigDecimal.valueOf(1200));
        } else if (isExit) {
            seat.setSeatType("EXIT_ROW");
            seat.setPremiumSurcharge(BigDecimal.valueOf(800));
        } else if (row <= 3) {
            seat.setSeatType("PREMIUM");
            seat.setPremiumSurcharge(BigDecimal.valueOf(600));
        } else {
            seat.setSeatType("STANDARD");
            seat.setPremiumSurcharge(BigDecimal.ZERO);
        }

        // Realistic deterministic availability: ~20% occupied
        boolean isOccupied = (rng.nextInt(9) <= 1);
        seat.setAvailable(!isOccupied);

        return seat;
    }

    private void broadcastSeatUpdate(Long flightId, Long seatId, String status) {
        if (messagingTemplate != null && flightId != null) {
            try {
                messagingTemplate.convertAndSend("/topic/seats/" + flightId, Map.of(
                        "flightId", flightId,
                        "seatId", seatId,
                        "status", status,
                        "timestamp", LocalDateTime.now().toString()
                ));
            } catch (Exception e) {
                logger.warn("Could not broadcast seat update over WebSocket: {}", e.getMessage());
            }
        }
    }
}
