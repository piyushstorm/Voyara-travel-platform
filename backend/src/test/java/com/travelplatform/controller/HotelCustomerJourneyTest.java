package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end hotel customer journey test.
 * Tests: search → details → room selection → booking → cancellation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HotelCustomerJourneyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User testUser;
    private String userToken;
    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testUser = new User("Hotel Test", "hotel-journey@test.com", passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        testHotel = new Hotel();
        testHotel.setName("Test Hotel Mumbai");
        testHotel.setCity("Mumbai");
        testHotel.setDescription("Premium test hotel");
        testHotel.setAddress("123 Marine Drive, Mumbai");
        testHotel.setStarRating(5);
        testHotel.setActive(true);
        testHotel = hotelRepository.save(testHotel);

        testRoom = new Room();
        testRoom.setHotel(testHotel);
        testRoom.setRoomType("DELUXE");
        testRoom.setName("Deluxe King Room");
        testRoom.setDescription("Spacious deluxe room");
        testRoom.setPricePerNight(new BigDecimal("8000.00"));
        testRoom.setBasePrice(new BigDecimal("8000.00"));
        testRoom.setMaxGuests(2);
        testRoom.setBedCount(1);
        testRoom.setBedType("King");
        testRoom.setTotalRooms(10);
        testRoom.setAvailableRooms(10);
        testRoom.setActive(true);
        testRoom = roomRepository.save(testRoom);
    }

    @Test
    void e2e_hotelCustomerJourney() throws Exception {
        // Step 1: Search hotels
        mockMvc.perform(get("/api/hotels/search")
                        .param("city", "Mumbai"))
                .andExpect(status().isOk());

        // Step 2: Get hotel details
        mockMvc.perform(get("/api/hotels/" + testHotel.getId()))
                .andExpect(status().isOk());

        // Step 3: Get rooms for hotel
        mockMvc.perform(get("/api/hotels/" + testHotel.getId() + "/rooms"))
                .andExpect(status().isOk());

        // Step 4: Book hotel room
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"HOTEL\"," +
                                "\"hotelId\":" + testHotel.getId() + "," +
                                "\"roomId\":" + testRoom.getId() + "," +
                                "\"numberOfNights\":2," +
                                "\"checkInDate\":\"" + java.time.LocalDate.now().plusDays(7) + "\"," +
                                "\"checkOutDate\":\"" + java.time.LocalDate.now().plusDays(9) + "\"," +
                                "\"paymentMethod\":\"CARD\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").exists())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalAmount").value(16000)); // 8000 × 2 nights

        // Step 5: Verify room availability decreased
        Room updatedRoom = roomRepository.findById(testRoom.getId()).orElse(null);
        assert updatedRoom != null;
        assert updatedRoom.getAvailableRooms() == 9;

        // Step 6: Verify in My Trips
        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Step 7: Cancel booking
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getUser().getEmail().equals("hotel-journey@test.com") && "CONFIRMED".equals(b.getStatus()))
                .findFirst().orElse(null);
        assert booking != null;

        mockMvc.perform(post("/api/bookings/" + booking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "Change of plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Step 8: Verify room restored
        Room restoredRoom = roomRepository.findById(testRoom.getId()).orElse(null);
        assert restoredRoom != null;
        assert restoredRoom.getAvailableRooms() == 10;
    }

    @Test
    void cannotBookUnavailableRoom() throws Exception {
        // Make room unavailable
        testRoom.setAvailableRooms(0);
        roomRepository.save(testRoom);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"HOTEL\"," +
                                "\"hotelId\":" + testHotel.getId() + "," +
                                "\"roomId\":" + testRoom.getId() + "," +
                                "\"numberOfNights\":1," +
                                "\"checkInDate\":\"" + java.time.LocalDate.now().plusDays(7) + "\"," +
                                "\"checkOutDate\":\"" + java.time.LocalDate.now().plusDays(8) + "\"," +
                                "\"paymentMethod\":\"CARD\"" +
                                "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hotelSearchByCity() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("city", "Mumbai"))
                .andExpect(status().isOk());
    }

    @Test
    void hotelSearchNoResults() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("city", "NonExistentCity"))
                .andExpect(status().isOk());
    }

    @Test
    void hotelDetailsNotFound() throws Exception {
        mockMvc.perform(get("/api/hotels/99999"))
                .andExpect(status().isNotFound());
    }
}
