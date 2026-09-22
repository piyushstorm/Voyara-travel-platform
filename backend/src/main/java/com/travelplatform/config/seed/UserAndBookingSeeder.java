package com.travelplatform.config.seed;

import com.travelplatform.entity.*;
import com.travelplatform.repository.BookingRepository;
import com.travelplatform.repository.PaymentRepository;
import com.travelplatform.repository.RefundRepository;
import com.travelplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class UserAndBookingSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserAndBookingSeeder.class);

    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RefundRepository refundRepo;
    private final com.travelplatform.repository.RoomRepository roomRepo;
    private final PasswordEncoder passwordEncoder;

    public UserAndBookingSeeder(UserRepository userRepo, BookingRepository bookingRepo,
                                PaymentRepository paymentRepo, RefundRepository refundRepo,
                                com.travelplatform.repository.RoomRepository roomRepo,
                                PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.bookingRepo = bookingRepo;
        this.paymentRepo = paymentRepo;
        this.refundRepo = refundRepo;
        this.roomRepo = roomRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, User> seedTestUsers() {
        log.info("Ensuring deterministic development & test users with valid BCrypt credentials...");

        record UserSpec(String email, String rawPassword, String name, Role role) {}

        List<UserSpec> specs = List.of(
            new UserSpec("admin@travel.com", "admin123", "Admin User", Role.ADMIN),
            new UserSpec("user@travel.com", "user123", "Demo User", Role.USER),
            new UserSpec("traveler@travel.com", "user123", "Frequent Traveler", Role.USER),
            new UserSpec("reviewer@travel.com", "user123", "Verified Reviewer", Role.USER),
            new UserSpec("moderator@travel.com", "admin123", "Review Moderator", Role.ADMIN),
            new UserSpec("trip.lead@travel.com", "user123", "Trip Organizer", Role.USER),
            new UserSpec("companion1@travel.com", "user123", "Arjun Verma", Role.USER),
            new UserSpec("companion2@travel.com", "user123", "Sneha Kapoor", Role.USER),
            new UserSpec("companion3@travel.com", "user123", "Rohan Mehta", Role.USER)
        );

        Map<String, User> userMap = new HashMap<>();

        for (UserSpec spec : specs) {
            Optional<User> existing = userRepo.findByEmail(spec.email());
            User user;
            if (existing.isPresent()) {
                user = existing.get();
                if (!passwordEncoder.matches(spec.rawPassword(), user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(spec.rawPassword()));
                }
                user.setRole(spec.role());
                user.setEmailVerified(true);
                user = userRepo.save(user);
            } else {
                user = new User();
                user.setEmail(spec.email());
                user.setPassword(passwordEncoder.encode(spec.rawPassword()));
                user.setName(spec.name());
                user.setRole(spec.role());
                user.setEmailVerified(true);
                user = userRepo.save(user);
            }
            userMap.put(spec.email(), user);
        }

        log.info("Test users initialized (total in system: {})", userRepo.count());
        return userMap;
    }

    @Transactional
    public List<Booking> seedControlledBookings(Map<String, User> users, List<Flight> flights, List<Hotel> hotels) {
        if (bookingRepo.count() >= 8 || bookingRepo.findByBookingReference("VOY-FL-2000").isPresent()) {
            log.info("Bookings already seeded ({}), skipping.", bookingRepo.count());
            return bookingRepo.findAll();
        }

        log.info("Seeding realistic confirmed, completed, cancelled, and refunded bookings...");

        User demoUser = users.get("user@travel.com");
        User traveler = users.get("traveler@travel.com");
        User reviewer = users.get("reviewer@travel.com");
        if (demoUser == null || traveler == null || flights.isEmpty() || hotels.isEmpty()) {
            return Collections.emptyList();
        }

        List<Booking> seededBookings = new ArrayList<>();
        int refSeq = 2000;
        LocalDateTime now = LocalDateTime.now();

        // 1. Confirmed Flight Booking for user@travel.com
        Flight f1 = flights.get(0);
        Booking b1 = new Booking();
        b1.setBookingReference("VOY-FL-" + (refSeq++));
        b1.setUser(demoUser);
        b1.setBookingType("FLIGHT");
        b1.setStatus("CONFIRMED");
        b1.setFlight(f1);
        b1.setCabinClass("ECONOMY");
        b1.setPassengerCount(1);
        b1.setSelectedSeatNumbers("12A");
        b1.setTravelDate(f1.getDepartureTime());
        BigDecimal total1 = f1.getEconomyPrice().add(BigDecimal.valueOf(800)); // ticket + exit row seat
        b1.setTotalAmount(total1);
        b1.setOriginalAmount(total1);
        b1.setPaymentMethod("CARD");
        b1.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_1");
        b1 = bookingRepo.save(b1);
        seededBookings.add(b1);

        Payment p1 = new Payment(b1.getPaymentId(), total1, "CARD", "COMPLETED");
        p1.setBooking(b1);
        p1.setRazorpayPaymentId("pay_test_mock_rzp_001");
        p1.setCapturedAt(now.minusHours(4));
        paymentRepo.save(p1);

        // 2. Confirmed Hotel Booking for user@travel.com
        Hotel h1 = hotels.get(0);
        Room r1 = roomRepo.findByHotelId(h1.getId()).stream().findFirst().orElse(null);
        Booking b2 = new Booking();
        b2.setBookingReference("VOY-HT-" + (refSeq++));
        b2.setUser(demoUser);
        b2.setBookingType("HOTEL");
        b2.setStatus("CONFIRMED");
        b2.setHotel(h1);
        b2.setRoom(r1);
        b2.setPassengerCount(2);
        b2.setNumberOfNights(3);
        b2.setCheckInDate(now.plusDays(3));
        b2.setCheckOutDate(now.plusDays(6));
        BigDecimal total2 = (r1 != null ? r1.getPricePerNight() : h1.getStartingPrice()).multiply(BigDecimal.valueOf(3));
        b2.setTotalAmount(total2);
        b2.setOriginalAmount(total2);
        b2.setPaymentMethod("UPI");
        b2.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_2");
        b2 = bookingRepo.save(b2);
        seededBookings.add(b2);

        Payment p2 = new Payment(b2.getPaymentId(), total2, "UPI", "COMPLETED");
        p2.setBooking(b2);
        p2.setRazorpayPaymentId("pay_test_mock_rzp_002");
        p2.setCapturedAt(now.minusDays(1));
        paymentRepo.save(p2);

        // 3. Completed Past Flight Trip for reviewer@travel.com (can be verified reviewed)
        Flight fPast = flights.stream().filter(f -> f.getDepartureTime().isBefore(now)).findFirst().orElse(flights.get(1));
        Booking b3 = new Booking();
        b3.setBookingReference("VOY-FL-" + (refSeq++));
        b3.setUser(reviewer);
        b3.setBookingType("FLIGHT");
        b3.setStatus("COMPLETED");
        b3.setFlight(fPast);
        b3.setCabinClass("ECONOMY");
        b3.setPassengerCount(1);
        b3.setSelectedSeatNumbers("15C");
        b3.setTravelDate(fPast.getDepartureTime());
        BigDecimal total3 = fPast.getEconomyPrice();
        b3.setTotalAmount(total3);
        b3.setOriginalAmount(total3);
        b3.setPaymentMethod("CARD");
        b3.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_3");
        b3 = bookingRepo.save(b3);
        seededBookings.add(b3);

        Payment p3 = new Payment(b3.getPaymentId(), total3, "CARD", "COMPLETED");
        p3.setBooking(b3);
        p3.setRazorpayPaymentId("pay_test_mock_rzp_003");
        p3.setCapturedAt(now.minusDays(2));
        paymentRepo.save(p3);

        // 4. Completed Past Hotel Stay for reviewer@travel.com (can be verified reviewed)
        Hotel hPast = hotels.size() > 1 ? hotels.get(1) : hotels.get(0);
        Room rPast = roomRepo.findByHotelId(hPast.getId()).stream().findFirst().orElse(null);
        Booking b4 = new Booking();
        b4.setBookingReference("VOY-HT-" + (refSeq++));
        b4.setUser(reviewer);
        b4.setBookingType("HOTEL");
        b4.setStatus("COMPLETED");
        b4.setHotel(hPast);
        b4.setRoom(rPast);
        b4.setPassengerCount(2);
        b4.setNumberOfNights(2);
        b4.setCheckInDate(now.minusDays(4));
        b4.setCheckOutDate(now.minusDays(2));
        BigDecimal total4 = (rPast != null ? rPast.getPricePerNight() : hPast.getStartingPrice()).multiply(BigDecimal.valueOf(2));
        b4.setTotalAmount(total4);
        b4.setOriginalAmount(total4);
        b4.setPaymentMethod("CARD");
        b4.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_4");
        b4 = bookingRepo.save(b4);
        seededBookings.add(b4);

        Payment p4 = new Payment(b4.getPaymentId(), total4, "CARD", "COMPLETED");
        p4.setBooking(b4);
        p4.setRazorpayPaymentId("pay_test_mock_rzp_004");
        p4.setCapturedAt(now.minusDays(5));
        paymentRepo.save(p4);

        // 5. Cancelled Flight Booking with Full Refund (100%) for traveler@travel.com
        Flight fCancel1 = flights.size() > 2 ? flights.get(2) : flights.get(0);
        Booking b5 = new Booking();
        b5.setBookingReference("VOY-FL-" + (refSeq++));
        b5.setUser(traveler);
        b5.setBookingType("FLIGHT");
        b5.setStatus("CANCELLED");
        b5.setFlight(fCancel1);
        b5.setCabinClass("PREMIUM_ECONOMY");
        b5.setPassengerCount(1);
        b5.setSelectedSeatNumbers("5A");
        b5.setTravelDate(now.plusDays(10));
        BigDecimal total5 = fCancel1.getPremiumEconomyPrice();
        b5.setTotalAmount(total5);
        b5.setOriginalAmount(total5);
        b5.setRefundAmount(total5);
        b5.setCancellationReason(CancellationReason.CHANGE_OF_PLANS.getCode());
        b5.setPaymentMethod("UPI");
        b5.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_5");
        b5 = bookingRepo.save(b5);
        seededBookings.add(b5);

        Payment p5 = new Payment(b5.getPaymentId(), total5, "UPI", "REFUNDED");
        p5.setBooking(b5);
        p5.setRazorpayPaymentId("pay_test_mock_rzp_005");
        p5.setCapturedAt(now.minusDays(3));
        paymentRepo.save(p5);

        Refund rfd1 = new Refund();
        rfd1.setBooking(b5);
        rfd1.setRefundId("rfd_test_sim_" + System.currentTimeMillis() + "_1");
        rfd1.setOriginalAmount(total5);
        rfd1.setRefundAmount(total5);
        rfd1.setRefundPercentage(BigDecimal.valueOf(100.00));
        rfd1.setStatus("COMPLETED");
        rfd1.setCancellationReason(CancellationReason.CHANGE_OF_PLANS.getCode());
        rfd1.setCancellationComment("Personal travel schedule changed, cancelled 10 days in advance.");
        rfd1.setCancellationPolicy("FLIGHT: More than 7 days (100% refund)");
        rfd1.setRazorpayRefundId("rfnd_test_mock_rzp_001");
        rfd1.setExternalRefundId("rfnd_test_mock_rzp_001");
        rfd1.setCurrency("INR");
        refundRepo.save(rfd1);

        // 6. Cancelled Flight Booking with Partial Refund (70%) for traveler@travel.com
        Flight fCancel2 = flights.size() > 3 ? flights.get(3) : flights.get(0);
        Booking b6 = new Booking();
        b6.setBookingReference("VOY-FL-" + (refSeq++));
        b6.setUser(traveler);
        b6.setBookingType("FLIGHT");
        b6.setStatus("CANCELLED");
        b6.setFlight(fCancel2);
        b6.setCabinClass("ECONOMY");
        b6.setPassengerCount(1);
        b6.setSelectedSeatNumbers("18D");
        b6.setTravelDate(now.plusDays(4));
        BigDecimal total6 = fCancel2.getEconomyPrice();
        BigDecimal refund6 = total6.multiply(BigDecimal.valueOf(0.70)).subtract(BigDecimal.valueOf(200)).setScale(2, RoundingMode.HALF_UP);
        b6.setTotalAmount(total6);
        b6.setOriginalAmount(total6);
        b6.setRefundAmount(refund6);
        b6.setCancellationReason(CancellationReason.FOUND_BETTER_PRICE.getCode());
        b6.setPaymentMethod("CARD");
        b6.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_6");
        b6 = bookingRepo.save(b6);
        seededBookings.add(b6);

        Payment p6 = new Payment(b6.getPaymentId(), total6, "CARD", "REFUNDED");
        p6.setBooking(b6);
        p6.setRazorpayPaymentId("pay_test_mock_rzp_006");
        p6.setCapturedAt(now.minusDays(1));
        paymentRepo.save(p6);

        Refund rfd2 = new Refund();
        rfd2.setBooking(b6);
        rfd2.setRefundId("rfd_test_sim_" + System.currentTimeMillis() + "_2");
        rfd2.setOriginalAmount(total6);
        rfd2.setRefundAmount(refund6);
        rfd2.setRefundPercentage(BigDecimal.valueOf(70.00));
        rfd2.setStatus("COMPLETED");
        rfd2.setCancellationReason(CancellationReason.FOUND_BETTER_PRICE.getCode());
        rfd2.setCancellationComment("Found better direct flight timings.");
        rfd2.setCancellationPolicy("FLIGHT: 3-7 days (70% refund, ₹200 fee)");
        rfd2.setRazorpayRefundId("rfnd_test_mock_rzp_002");
        rfd2.setExternalRefundId("rfnd_test_mock_rzp_002");
        rfd2.setCurrency("INR");
        refundRepo.save(rfd2);

        // 7. Cancelled Hotel Booking with Refund Pending for user@travel.com
        Hotel hCancel = hotels.size() > 2 ? hotels.get(2) : hotels.get(0);
        Booking b7 = new Booking();
        b7.setBookingReference("VOY-HT-" + (refSeq++));
        b7.setUser(demoUser);
        b7.setBookingType("HOTEL");
        b7.setStatus("CANCELLED");
        b7.setHotel(hCancel);
        b7.setPassengerCount(1);
        b7.setNumberOfNights(2);
        b7.setCheckInDate(now.plusDays(2));
        b7.setCheckOutDate(now.plusDays(4));
        BigDecimal total7 = hCancel.getStartingPrice().multiply(BigDecimal.valueOf(2));
        BigDecimal refund7 = total7.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
        b7.setTotalAmount(total7);
        b7.setOriginalAmount(total7);
        b7.setRefundAmount(refund7);
        b7.setCancellationReason(CancellationReason.MEDICAL_EMERGENCY.getCode());
        b7.setPaymentMethod("NET_BANKING");
        b7.setPaymentId("pay_test_sim_" + System.currentTimeMillis() + "_7");
        b7 = bookingRepo.save(b7);
        seededBookings.add(b7);

        Payment p7 = new Payment(b7.getPaymentId(), total7, "NET_BANKING", "COMPLETED");
        p7.setBooking(b7);
        p7.setRazorpayPaymentId("pay_test_mock_rzp_007");
        p7.setCapturedAt(now.minusHours(12));
        paymentRepo.save(p7);

        Refund rfd3 = new Refund();
        rfd3.setBooking(b7);
        rfd3.setRefundId("rfd_test_sim_" + System.currentTimeMillis() + "_3");
        rfd3.setOriginalAmount(total7);
        rfd3.setRefundAmount(refund7);
        rfd3.setRefundPercentage(BigDecimal.valueOf(50.00));
        rfd3.setStatus("PROCESSING");
        rfd3.setCancellationReason(CancellationReason.MEDICAL_EMERGENCY.getCode());
        rfd3.setCancellationComment("Family medical emergency requiring trip postponement.");
        rfd3.setCancellationPolicy("HOTEL: 24-72 hours (50% refund, ₹1000 fee)");
        rfd3.setCurrency("INR");
        refundRepo.save(rfd3);

        log.info("Seeded {} representative bookings, payments, and refund records.", seededBookings.size());
        return seededBookings;
    }
}
