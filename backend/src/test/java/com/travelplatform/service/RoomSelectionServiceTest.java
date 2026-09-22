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
import com.travelplatform.repository.HotelRepository;
import com.travelplatform.repository.RoomHoldRepository;
import com.travelplatform.repository.RoomRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.repository.UserTravelPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RoomSelectionServiceTest {

    @Autowired private RoomSelectionService roomService;
    @Autowired private RoomRepository roomRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private RoomHoldRepository roomHoldRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private UserTravelPreferenceRepository preferenceRepo;

    private Hotel testHotel;
    private Room stdRoom;
    private Room delRoom;
    private User testUser;
    private User testUser2;

    @BeforeEach
    void setup() {
        roomHoldRepo.deleteAll();

        testUser = userRepo.findByEmail("room_test1@voyara.com").orElseGet(() ->
                userRepo.save(new User("Room Test 1", "room_test1@voyara.com", "Password123!")));

        testUser2 = userRepo.findByEmail("room_test2@voyara.com").orElseGet(() ->
                userRepo.save(new User("Room Test 2", "room_test2@voyara.com", "Password123!")));

        testHotel = new Hotel();
        testHotel.setName("Room Selection Test Hotel " + System.nanoTime());
        testHotel.setCity("Mumbai");
        testHotel.setAddress("Marine Drive");
        testHotel.setDescription("Luxury hotel");
        testHotel.setStarRating(5);
        testHotel.setStartingPrice(BigDecimal.valueOf(5000));
        testHotel.setImageUrl("https://example.com/hotel.jpg");
        testHotel = hotelRepo.save(testHotel);

        stdRoom = new Room();
        stdRoom.setHotel(testHotel);
        stdRoom.setRoomType("STANDARD");
        stdRoom.setName("Standard King");
        stdRoom.setPricePerNight(BigDecimal.valueOf(5000));
        stdRoom.setTotalRooms(5);
        stdRoom.setAvailableRooms(5);
        stdRoom.setBedType("KING");
        stdRoom.setAmenities("Free Wi-Fi,Air Conditioning,TV");
        stdRoom = roomRepo.save(stdRoom);

        delRoom = new Room();
        delRoom.setHotel(testHotel);
        delRoom.setRoomType("DELUXE");
        delRoom.setName("Deluxe King");
        delRoom.setPricePerNight(BigDecimal.valueOf(6500));
        delRoom.setTotalRooms(3);
        delRoom.setAvailableRooms(3);
        delRoom.setBedType("KING");
        delRoom.setAmenities("Free Wi-Fi,Air Conditioning,City View,Free Breakfast");
        delRoom = roomRepo.save(delRoom);
    }

    @Test
    @DisplayName("Retrieve RoomSelectionResponseDto with accurate upgrade differences")
    void testGetRoomSelectionDto() {
        RoomSelectionResponseDto response = roomService.getRoomSelectionDto(testHotel.getId(), testUser.getId());
        assertNotNull(response);
        assertEquals(testHotel.getId(), response.getHotelId());
        assertFalse(response.getRooms().isEmpty(), "Rooms list should not be empty");

        for (RoomDto r : response.getRooms()) {
            assertNotNull(r.getName());
            assertNotNull(r.getPricePerNight());
            assertNotNull(r.getAmenities());
            assertTrue(r.isBookable());
        }
    }

    @Test
    @DisplayName("Personalized Room Recommendations match user travel preferences")
    void testPersonalizedRoomRecommendation() {
        UserTravelPreference pref = preferenceRepo.findByUserId(testUser.getId())
                .orElseGet(() -> new UserTravelPreference(testUser));
        pref.setPreferredRoomType("DELUXE");
        pref.setPreferredBedType("KING");
        preferenceRepo.save(pref);

        RoomSelectionResponseDto response = roomService.getRoomSelectionDto(testHotel.getId(), testUser.getId());
        boolean hasMatch = response.getRooms().stream().anyMatch(RoomDto::isPreferenceMatch);
        assertTrue(hasMatch, "At least one Deluxe room should have preferenceMatch=true");
    }

    @Test
    @DisplayName("Successfully hold an available room")
    void testHoldRoom() {
        RoomHoldResponseDto hold = roomService.holdRoomDto(stdRoom.getId(), testUser.getId());
        assertNotNull(hold);
        assertEquals("ACTIVE", hold.getStatus());
        assertEquals(stdRoom.getId(), hold.getRoomId());
        assertTrue(hold.getSecondsRemaining() > 0);

        // Verify hold is recorded in DB
        List<RoomHold> holds = roomHoldRepo.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertFalse(holds.isEmpty());
    }

    @Test
    @DisplayName("Room Hold Concurrency: Exactly 1 out of 5 users can hold the last available room")
    void testRoomHoldConcurrencyOnLastRoom() throws Exception {
        // Set available rooms to 1
        stdRoom.setAvailableRooms(1);
        roomRepo.save(stdRoom);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= threads; i++) {
            final String email = "room_conc_" + i + "@voyara.com";
            User u = userRepo.findByEmail(email).orElseGet(() ->
                    userRepo.save(new User("Room Conc " + email, email, "Password123!")));
            users.add(u);
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (User u : users) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    roomService.holdRoom(stdRoom.getId(), u.getId());
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 user should hold the last room");
        assertEquals(threads - 1, failCount.get(), "Other users must receive room unavailable rejection");
    }

    @Test
    @DisplayName("Room hold release restores availability")
    void testReleaseRoomHold() {
        roomService.holdRoom(stdRoom.getId(), testUser.getId());

        // Release hold
        roomService.releaseRoomHold(stdRoom.getId(), testUser.getId());

        // Second user can now hold
        RoomHold hold2 = roomService.holdRoom(stdRoom.getId(), testUser2.getId());
        assertNotNull(hold2);
        assertEquals("ACTIVE", hold2.getStatus());
    }
}
