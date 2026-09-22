package com.travelplatform.service;

import com.travelplatform.dto.booking.BookingRequest;
import com.travelplatform.dto.booking.BookingResponse;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.Room;
import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SeatAndRoomBookingPricingTest {

    @Autowired private BookingService bookingService;
    @Autowired private FlightRepository flightRepo;
    @Autowired private AirportRepository airportRepo;
    @Autowired private AirlineRepository airlineRepo;
    @Autowired private SeatRepository seatRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private RoomRepository roomRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RoomSelectionService roomService;
    @Autowired private SeatSelectionService seatService;

    private User testUser;
    private Flight testFlight;
    private Seat standardSeat;
    private Seat premiumSeat;
    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setup() {
        testUser = userRepo.findByEmail("booking_pricing_user@voyara.com").orElseGet(() ->
                userRepo.save(new User("Pricing User", "booking_pricing_user@voyara.com", "Password123!")));

        testFlight = flightRepo.findAll().stream().findFirst().orElseGet(() -> {
            Airport origin = airportRepo.findByCode("DEL").orElseGet(() ->
                    airportRepo.save(new Airport("DEL", "Indira Gandhi International", "Delhi", "India", 28.5562, 77.1000)));
            Airport dest = airportRepo.findByCode("BOM").orElseGet(() ->
                    airportRepo.save(new Airport("BOM", "Chhatrapati Shivaji International", "Mumbai", "India", 19.0896, 72.8656)));
            Airline airline = airlineRepo.findByCode("AI").orElseGet(() ->
                    airlineRepo.save(new Airline("AI", "Air India", "https://example.com/ai.png", 4.5)));

            Flight f = new Flight();
            f.setFlightNumber("AI-PRICE-99");
            f.setAirline(airline);
            f.setOrigin(origin);
            f.setDestination(dest);
            f.setOriginCode("DEL");
            f.setDestinationCode("BOM");
            f.setDepartureTime(LocalDateTime.now().plusDays(5));
            f.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
            f.setDepartureDate(LocalDateTime.now().plusDays(5).toLocalDate());
            f.setEconomyPrice(new BigDecimal("5000.00"));
            f.setPremiumEconomyPrice(new BigDecimal("7500.00"));
            f.setBusinessPrice(new BigDecimal("12000.00"));
            f.setFirstClassPrice(new BigDecimal("20000.00"));
            f.setTotalSeatsEconomy(100);
            f.setDurationMinutes(120);
            return flightRepo.save(f);
        });

        // Create or get standard seat
        standardSeat = new Seat();
        standardSeat.setFlight(testFlight);
        standardSeat.setSeatNumber("15B");
        standardSeat.setCabinClass("ECONOMY");
        standardSeat.setRowNumber(15);
        standardSeat.setColumnLetter("B");
        standardSeat.setPrice(BigDecimal.valueOf(400));
        standardSeat.setPremiumSurcharge(BigDecimal.ZERO);
        standardSeat.setSeatType("STANDARD");
        standardSeat.setAvailable(true);
        standardSeat = seatRepo.save(standardSeat);

        // Create or get premium extra legroom seat
        premiumSeat = new Seat();
        premiumSeat.setFlight(testFlight);
        premiumSeat.setSeatNumber("1A");
        premiumSeat.setCabinClass("ECONOMY");
        premiumSeat.setRowNumber(1);
        premiumSeat.setColumnLetter("A");
        premiumSeat.setWindow(true);
        premiumSeat.setExtraLegroom(true);
        premiumSeat.setPrice(BigDecimal.valueOf(500));
        premiumSeat.setPremiumSurcharge(BigDecimal.valueOf(1200));
        premiumSeat.setSeatType("EXTRA_LEGROOM");
        premiumSeat.setAvailable(true);
        premiumSeat = seatRepo.save(premiumSeat);

    }

    @Test
    @DisplayName("Flight Booking Total accurately includes premium seat surcharges")
    @Transactional
    void testFlightBookingIncludesSeatSurcharge() {
        // Base fare for ECONOMY
        BigDecimal basePrice = testFlight.getPriceForClass("ECONOMY");

        // 1. Booking without seats
        BookingRequest reqNoSeat = new BookingRequest();
        reqNoSeat.setBookingType("FLIGHT");
        reqNoSeat.setFlightId(testFlight.getId());
        reqNoSeat.setCabinClass("ECONOMY");
        reqNoSeat.setPassengerCount(1);
        reqNoSeat.setPaymentMethod("CARD");
        BookingResponse respNoSeat = bookingService.createBooking(reqNoSeat, testUser.getId());
        assertEquals(basePrice.setScale(2), respNoSeat.getTotalAmount().setScale(2));

        // 2. Booking with Premium Seat (surcharge = ₹1200)
        seatService.holdSeat(premiumSeat.getId(), testUser.getId());

        BookingRequest reqPremium = new BookingRequest();
        reqPremium.setBookingType("FLIGHT");
        reqPremium.setFlightId(testFlight.getId());
        reqPremium.setCabinClass("ECONOMY");
        reqPremium.setPassengerCount(1);
        reqPremium.setSeatIds(List.of(premiumSeat.getId()));
        reqPremium.setPaymentMethod("CARD");

        BookingResponse respPremium = bookingService.createBooking(reqPremium, testUser.getId());

        // Expected: basePrice + 1200
        BigDecimal expectedTotal = basePrice.add(BigDecimal.valueOf(1200)).setScale(2);
        assertEquals(expectedTotal, respPremium.getTotalAmount().setScale(2),
                "Flight total must strictly include the seat premium surcharge");
        assertEquals("1A", respPremium.getSeatNumbers());
    }

    @Test
    @DisplayName("Hotel Booking decrements inventory and confirms room hold")
    @Transactional
    void testHotelBookingConfirmsRoomHold() {
        Hotel h = new Hotel();
        h.setName("Pricing Grand Hotel Unique");
        h.setCity("Mumbai");
        h.setCountry("India");
        h.setAddress("Marine Drive");
        h.setStarRating(5);
        h.setStartingPrice(new BigDecimal("8000.00"));
        testHotel = hotelRepo.save(h);

        Room r = new Room();
        r.setHotel(testHotel);
        r.setName("Deluxe King Room");
        r.setRoomType("DELUXE");
        r.setPricePerNight(new BigDecimal("9000.00"));
        r.setAvailableRooms(10);
        r.setMaxGuests(2);
        testRoom = roomRepo.save(r);

        int initialRooms = testRoom.getAvailableRooms();

        // Hold room first
        roomService.holdRoom(testRoom.getId(), testUser.getId());

        BookingRequest reqHotel = new BookingRequest();
        reqHotel.setBookingType("HOTEL");
        reqHotel.setHotelId(testHotel.getId());
        reqHotel.setRoomId(testRoom.getId());
        reqHotel.setNumberOfNights(2);
        reqHotel.setPaymentMethod("CARD");

        BookingResponse resp = bookingService.createBooking(reqHotel, testUser.getId());
        assertNotNull(resp);
        assertEquals("CONFIRMED", resp.getStatus());
        assertEquals(testRoom.getName(), resp.getRoomName());

        BigDecimal expectedTotal = testRoom.getPricePerNight().multiply(BigDecimal.valueOf(2)).setScale(2);
        assertEquals(expectedTotal, resp.getTotalAmount().setScale(2));

        Room updatedRoom = roomRepo.findById(testRoom.getId()).orElseThrow();
        assertEquals(initialRooms - 1, updatedRoom.getAvailableRooms(), "Room inventory must be decremented by 1");
    }
}
