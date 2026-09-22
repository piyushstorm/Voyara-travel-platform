package com.travelplatform.service;

import com.travelplatform.dto.selection.RoomDto;
import com.travelplatform.dto.selection.RoomHoldResponseDto;
import com.travelplatform.dto.selection.RoomSelectionResponseDto;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.Room;
import com.travelplatform.entity.RoomHold;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserTravelPreference;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.HotelRepository;
import com.travelplatform.repository.RoomHoldRepository;
import com.travelplatform.repository.RoomRepository;
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
public class RoomSelectionService {

    private static final Logger logger = LoggerFactory.getLogger(RoomSelectionService.class);
    private static final int HOLD_DURATION_MINUTES = 10;

    private final RoomRepository roomRepo;
    private final HotelRepository hotelRepo;
    private final RoomHoldRepository roomHoldRepo;
    private final UserRepository userRepo;
    private final UserTravelPreferenceRepository preferenceRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomSelectionService(RoomRepository roomRepo,
                                HotelRepository hotelRepo,
                                RoomHoldRepository roomHoldRepo,
                                UserRepository userRepo,
                                UserTravelPreferenceRepository preferenceRepo,
                                @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.roomRepo = roomRepo;
        this.hotelRepo = hotelRepo;
        this.roomHoldRepo = roomHoldRepo;
        this.userRepo = userRepo;
        this.preferenceRepo = preferenceRepo;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Get all rooms for a hotel with verified galleries, upgrade deltas, and personalized recommendations.
     */
    @Transactional(readOnly = true)
    public RoomSelectionResponseDto getRoomSelectionDto(Long hotelId, Long userId) {
        Hotel hotel = hotelRepo.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));

        List<Room> rooms = roomRepo.findByHotelIdOrderByPricePerNightAsc(hotelId);

        UserTravelPreference preference = userId != null
                ? preferenceRepo.findByUserId(userId).orElse(null)
                : null;

        List<RoomHold> userActiveHolds = userId != null
                ? roomHoldRepo.findByUserIdAndStatus(userId, "ACTIVE")
                : List.of();

        Long userHeldRoomId = null;
        for (RoomHold rh : userActiveHolds) {
            if (!rh.isExpired() && rh.getRoom() != null && rh.getRoom().getHotel().getId().equals(hotelId)) {
                userHeldRoomId = rh.getRoom().getId();
                break;
            }
        }

        BigDecimal lowestPrice = rooms.stream()
                .filter(Room::isActive)
                .map(Room::getPricePerNight)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        List<RoomDto> roomDtos = new ArrayList<>();

        for (Room room : rooms) {
            if (!room.isActive()) continue;

            long activeHolds = roomHoldRepo.countActiveHoldsForRoom(room.getId(), now);
            int availableAfterHolds = Math.max(0, room.getAvailableRooms() - (int) activeHolds);
            boolean isBookable = availableAfterHolds > 0 || (userHeldRoomId != null && userHeldRoomId.equals(room.getId()));

            BigDecimal upgradeDiff = room.getPricePerNight().subtract(lowestPrice).max(BigDecimal.ZERO);

            boolean match = false;
            String matchReason = null;

            if (preference != null) {
                String prefType = preference.getPreferredRoomType();
                String prefBed = preference.getPreferredBedType();

                boolean typeMatch = prefType != null && prefType.equalsIgnoreCase(room.getRoomType());
                boolean bedMatch = prefBed != null && prefBed.equalsIgnoreCase(room.getBedType());

                if (typeMatch && bedMatch) {
                    match = true;
                    matchReason = "Recommended: Matches your preferred " + prefType.toLowerCase() + " & " + prefBed.toLowerCase() + " bed";
                } else if (typeMatch) {
                    match = true;
                    matchReason = "Recommended: Matches your preferred " + prefType.toLowerCase() + " room";
                } else if (bedMatch) {
                    match = true;
                    matchReason = "Matches your preferred " + prefBed.toLowerCase() + " bed";
                }
            }

            roomDtos.add(new RoomDto(
                    room.getId(),
                    room.getRoomType(),
                    room.getName(),
                    room.getDescription(),
                    room.getPricePerNight(),
                    room.getBasePrice(),
                    room.getMaxGuests(),
                    room.getBedCount(),
                    room.getBedType(),
                    room.getSizeSqm(),
                    room.getTotalRooms(),
                    room.getAvailableRooms(),
                    (int) activeHolds,
                    isBookable,
                    room.getImageUrl(),
                    room.getImagesList(),
                    room.getAmenitiesList(),
                    room.getCurrency(),
                    match,
                    matchReason,
                    upgradeDiff
            ));
        }

        List<Map<String, Object>> upgrades = getUpgradeOptions(hotelId, "STANDARD");

        String prefSummary = preference != null
                ? "Preferred: " + preference.getPreferredRoomType() + " (" + preference.getPreferredBedType() + " Bed)"
                : null;

        return new RoomSelectionResponseDto(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                hotel.getStarRating(),
                roomDtos,
                upgrades,
                prefSummary,
                userHeldRoomId
        );
    }

    /**
     * Backward-compatible map response.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomSelectionData(Long hotelId) {
        RoomSelectionResponseDto dto = getRoomSelectionDto(hotelId, null);
        List<Map<String, Object>> roomData = dto.getRooms().stream().map(r -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", r.getId());
            data.put("roomType", r.getRoomType());
            data.put("name", r.getName());
            data.put("description", r.getDescription());
            data.put("pricePerNight", r.getPricePerNight());
            data.put("maxGuests", r.getMaxGuests());
            data.put("bedCount", r.getBedCount());
            data.put("bedType", r.getBedType());
            data.put("sizeSqm", r.getSizeSqm());
            data.put("totalRooms", r.getTotalRooms());
            data.put("availableRooms", r.getAvailableRooms());
            data.put("imageUrl", r.getImageUrl());
            data.put("images", r.getImages());
            data.put("amenities", r.getAmenities());
            data.put("bookable", r.isBookable());
            data.put("upgradePriceDiff", r.getUpgradePriceDiff());
            data.put("preferenceMatch", r.isPreferenceMatch());
            data.put("matchReason", r.getMatchReason());
            return data;
        }).toList();

        return Map.of(
                "hotelName", dto.getHotelName(),
                "city", dto.getCity(),
                "starRating", dto.getStarRating(),
                "rooms", roomData
        );
    }

    /**
     * Concurrency-safe hold on 1 room unit of a given room type with PESSIMISTIC_WRITE lock.
     */
    @Transactional
    public RoomHold holdRoom(Long roomId, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Room room = roomRepo.findByIdWithPessimisticLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        LocalDateTime now = LocalDateTime.now();
        long activeHolds = roomHoldRepo.countActiveHoldsForRoom(roomId, now);

        // Check if user already holds this room
        Optional<RoomHold> existingHoldOpt = roomHoldRepo.findByRoomIdAndUserIdAndStatus(roomId, userId, "ACTIVE");
        if (existingHoldOpt.isPresent()) {
            RoomHold existingHold = existingHoldOpt.get();
            if (!existingHold.isExpired()) {
                existingHold.setExpiresAt(now.plusMinutes(HOLD_DURATION_MINUTES));
                return roomHoldRepo.save(existingHold);
            } else {
                existingHold.setStatus("RELEASED");
                roomHoldRepo.save(existingHold);
            }
        }

        if (room.getAvailableRooms() <= (int) activeHolds) {
            throw new BadRequestException("Room type '" + room.getName() + "' is currently sold out or held by other guests");
        }

        // Release any existing room holds by this user in this hotel
        List<RoomHold> userHolds = roomHoldRepo.findByUserIdAndStatus(userId, "ACTIVE");
        for (RoomHold h : userHolds) {
            if (h.getRoom().getHotel().getId().equals(room.getHotel().getId())) {
                h.setStatus("RELEASED");
                roomHoldRepo.save(h);
            }
        }

        RoomHold hold = new RoomHold(room, user, HOLD_DURATION_MINUTES);
        hold = roomHoldRepo.save(hold);

        logger.info("Room {} held by user {} until {} (holdId={})",
                room.getName(), userId, hold.getExpiresAt(), hold.getId());

        broadcastRoomUpdate(room.getHotel().getId(), room.getId(), "HELD");

        return hold;
    }

    @Transactional
    public RoomHoldResponseDto holdRoomDto(Long roomId, Long userId) {
        RoomHold hold = holdRoom(roomId, userId);
        long remaining = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), hold.getExpiresAt()));
        return new RoomHoldResponseDto(
                hold.getId(),
                hold.getRoom().getId(),
                hold.getRoom().getName(),
                hold.getRoom().getRoomType(),
                hold.getExpiresAt(),
                remaining,
                hold.getStatus()
        );
    }

    /**
     * Release a room hold.
     */
    @Transactional
    public void releaseRoomHold(Long roomId, Long userId) {
        RoomHold hold = roomHoldRepo.findByRoomIdAndUserIdAndStatus(roomId, userId, "ACTIVE")
                .orElse(null);

        if (hold != null) {
            hold.setStatus("RELEASED");
            roomHoldRepo.save(hold);
            logger.info("Room hold {} released by user {}", hold.getId(), userId);
            broadcastRoomUpdate(hold.getRoom().getHotel().getId(), roomId, "AVAILABLE");
        }
    }

    /**
     * Confirm room hold during booking.
     */
    @Transactional
    public void confirmRoom(Long roomId, Long userId) {
        Room room = roomRepo.findByIdWithPessimisticLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        if (room.getAvailableRooms() < 1) {
            throw new BadRequestException("Room type '" + room.getName() + "' is sold out");
        }

        room.setAvailableRooms(room.getAvailableRooms() - 1);
        roomRepo.save(room);

        roomHoldRepo.findByRoomIdAndUserIdAndStatus(roomId, userId, "ACTIVE")
                .ifPresent(hold -> {
                    hold.setStatus("CONFIRMED");
                    roomHoldRepo.save(hold);
                });

        logger.info("Room {} confirmed and decremented for user {}", room.getName(), userId);
        broadcastRoomUpdate(room.getHotel().getId(), roomId, "CONFIRMED");
    }

    /**
     * Release expired room holds every 15 seconds.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void releaseExpiredRoomHolds() {
        List<RoomHold> expired = roomHoldRepo.findExpiredHolds(LocalDateTime.now());
        for (RoomHold hold : expired) {
            hold.setStatus("EXPIRED");
            roomHoldRepo.save(hold);
            broadcastRoomUpdate(hold.getRoom().getHotel().getId(), hold.getRoom().getId(), "AVAILABLE");
        }
        if (!expired.isEmpty()) {
            logger.info("Released {} expired room holds", expired.size());
        }
    }

    /**
     * Get upgrade options with exact benefits and price difference.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUpgradeOptions(Long hotelId, String currentRoomType) {
        List<Room> allRooms = roomRepo.findByHotelIdOrderByPricePerNightAsc(hotelId);

        String[] typeOrder = {"STANDARD", "DELUXE", "SUITE", "PRESIDENTIAL"};
        int currentIdx = -1;
        for (int i = 0; i < typeOrder.length; i++) {
            if (typeOrder[i].equalsIgnoreCase(currentRoomType)) {
                currentIdx = i;
                break;
            }
        }

        if (currentIdx < 0 || currentIdx >= typeOrder.length - 1) {
            return List.of();
        }

        final int minIdx = currentIdx;
        BigDecimal basePrice = allRooms.stream()
                .filter(r -> r.getRoomType().equalsIgnoreCase(currentRoomType))
                .map(Room::getPricePerNight)
                .findFirst()
                .orElse(allRooms.isEmpty() ? BigDecimal.ZERO : allRooms.get(0).getPricePerNight());

        return allRooms.stream()
                .filter(room -> {
                    for (int i = 0; i <= minIdx; i++) {
                        if (room.getRoomType().equalsIgnoreCase(typeOrder[i])) return false;
                    }
                    return room.getAvailableRooms() > 0 && room.isActive();
                })
                .map(room -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", room.getId());
                    map.put("roomType", room.getRoomType());
                    map.put("name", room.getName());
                    map.put("pricePerNight", room.getPricePerNight());
                    map.put("upgradePriceDiff", room.getPricePerNight().subtract(basePrice).max(BigDecimal.ZERO));
                    map.put("bedType", room.getBedType());
                    map.put("sizeSqm", room.getSizeSqm());
                    map.put("maxGuests", room.getMaxGuests());
                    map.put("amenities", room.getAmenitiesList());
                    map.put("imageUrl", room.getImageUrl());
                    return map;
                })
                .toList();
    }

    private void broadcastRoomUpdate(Long hotelId, Long roomId, String status) {
        if (messagingTemplate != null && hotelId != null) {
            try {
                messagingTemplate.convertAndSend("/topic/rooms/" + hotelId, Map.of(
                        "hotelId", hotelId,
                        "roomId", roomId,
                        "status", status,
                        "timestamp", LocalDateTime.now().toString()
                ));
            } catch (Exception e) {
                logger.warn("Could not broadcast room update over WebSocket: {}", e.getMessage());
            }
        }
    }
}
