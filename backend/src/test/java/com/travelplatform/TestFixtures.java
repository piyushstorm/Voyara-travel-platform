package com.travelplatform;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reusable test fixtures for creating test data.
 * All methods create and persist entities in the test database.
 */
@Component
public class TestFixtures {

    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AirlineRepository airlineRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    // ─── Users ────────────────────────────────────────────

    public User createUser(String name, String email) {
        User user = new User(name, email, passwordEncoder.encode("password123"));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    public User createAdmin(String name, String email) {
        User admin = new User(name, email, passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        return userRepository.save(admin);
    }

    public String getTokenFor(User user) {
        return tokenProvider.generateAccessToken(user.getEmail());
    }

    // ─── Flights ──────────────────────────────────────────

    public Flight createFlight(String number, String originCode, String destCode, BigDecimal price) {
        Airline airline = airlineRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Airline a = new Airline();
                    a.setName("Test Airways");
                    a.setCode("TA");
                    return airlineRepository.save(a);
                });

        Airport origin = airportRepository.findByCode(originCode).orElseGet(() -> {
            Airport ap = new Airport();
            ap.setCode(originCode);
            ap.setName(originCode + " Airport");
            ap.setCity(originCode);
            ap.setCountry("India");
            return airportRepository.save(ap);
        });

        Airport dest = airportRepository.findByCode(destCode).orElseGet(() -> {
            Airport ap = new Airport();
            ap.setCode(destCode);
            ap.setName(destCode + " Airport");
            ap.setCity(destCode);
            ap.setCountry("India");
            return airportRepository.save(ap);
        });

        Flight flight = new Flight();
        flight.setFlightNumber(number);
        flight.setAirline(airline);
        flight.setOrigin(origin);
        flight.setDestination(dest);
        flight.setOriginCode(originCode);
        flight.setDestinationCode(destCode);
        flight.setDepartureTime(LocalDateTime.now().plusDays(7));
        flight.setArrivalTime(LocalDateTime.now().plusDays(7).plusHours(2));
        flight.setDepartureDate(java.time.LocalDate.now().plusDays(7));
        flight.setDurationMinutes(120);
        flight.setStops(0);
        flight.setEconomyBasePrice(price);
        flight.setEconomyPrice(price);
        flight.setPremiumEconomyPrice(price.multiply(new BigDecimal("1.5")));
        flight.setBusinessPrice(price.multiply(new BigDecimal("2.5")));
        flight.setFirstClassPrice(price.multiply(new BigDecimal("4")));
        flight.setTotalSeatsEconomy(150);
        flight.setTotalSeatsPremiumEconomy(50);
        flight.setTotalSeatsBusiness(30);
        flight.setTotalSeatsFirst(10);
        flight.setBookedSeatsEconomy(10);
        flight.setBookedSeatsPremiumEconomy(5);
        flight.setBookedSeatsBusiness(2);
        flight.setBookedSeatsFirst(1);
        flight.setActive(true);
        return flightRepository.save(flight);
    }

    // ─── Hotels ───────────────────────────────────────────

    public Hotel createHotel(String name, String city, BigDecimal price) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setCity(city);
        hotel.setDescription("Test hotel in " + city);
        hotel.setAddress("123 Test Street, " + city);
        hotel.setStarRating(4);
        hotel.setActive(true);
        return hotelRepository.save(hotel);
    }

    public Room createRoom(Hotel hotel, String type, BigDecimal price, int available) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType(type);
        room.setName(type + " Room");
        room.setDescription("Comfortable " + type + " room");
        room.setPricePerNight(price);
        room.setBasePrice(price);
        room.setMaxGuests(2);
        room.setBedCount(1);
        room.setBedType("King");
        room.setTotalRooms(available);
        room.setAvailableRooms(available);
        room.setActive(true);
        return roomRepository.save(room);
    }

    // ─── Bookings ─────────────────────────────────────────

    public Booking createBooking(User user, String type, String status, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setBookingReference("TP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setUser(user);
        booking.setBookingType(type);
        booking.setStatus(status);
        booking.setTotalAmount(amount);
        booking.setOriginalAmount(amount);
        booking.setPassengerCount(1);
        return bookingRepository.save(booking);
    }

    public Payment createPayment(Booking booking, String status, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setPaymentMethod("RAZORPAY");
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    // ─── Coupons ──────────────────────────────────────────

    public Coupon createCoupon(String code, String discountType, BigDecimal value,
                                BigDecimal minAmount, String module, boolean active) {
        Coupon coupon = new Coupon();
        coupon.setCode(code.toUpperCase());
        coupon.setDescription("Test coupon " + code);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(value);
        coupon.setMinBookingAmount(minAmount);
        coupon.setApplicableModule(module);
        coupon.setActive(active);
        coupon.setStartDate(LocalDateTime.now().minusDays(1));
        coupon.setExpiryDate(LocalDateTime.now().plusDays(30));
        coupon.setUsageLimit(100);
        coupon.setPerUserLimit(2);
        return couponRepository.save(coupon);
    }
}
