package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.service.CancellationService;
import com.travelplatform.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HolidayPackageRepository holidayPackageRepository;
    private final TrainRepository trainRepository;
    private final BusRepository busRepository;
    private final CabRepository cabRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final TrainSeatRepository trainSeatRepository;
    private final AuditLogRepository auditLogRepository;
    private final CancellationService cancellationService;

    public AdminController(UserRepository userRepository, BookingRepository bookingRepository,
                           FlightRepository flightRepository, HotelRepository hotelRepository,
                           RoomRepository roomRepository, HolidayPackageRepository holidayPackageRepository,
                           TrainRepository trainRepository, BusRepository busRepository,
                           CabRepository cabRepository, PaymentRepository paymentRepository,
                           RefundRepository refundRepository, ReviewRepository reviewRepository,
                           ReviewService reviewService,
                           TrainSeatRepository trainSeatRepository,
                           AuditLogRepository auditLogRepository,
                           CancellationService cancellationService) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.holidayPackageRepository = holidayPackageRepository;
        this.trainRepository = trainRepository;
        this.busRepository = busRepository;
        this.cabRepository = cabRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.trainSeatRepository = trainSeatRepository;
        this.auditLogRepository = auditLogRepository;
        this.cancellationService = cancellationService;
    }

    // ==================== DASHBOARD ====================

    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("totalFlights", flightRepository.count());
        stats.put("totalHotels", hotelRepository.count());
        stats.put("totalRevenue", bookingRepository.findAll().stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.put("cancelledBookings", bookingRepository.findAll().stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .count());
        stats.put("confirmedBookings", bookingRepository.findAll().stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count());
        stats.put("pendingBookings", bookingRepository.findAll().stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .count());
        stats.put("totalRefunds", refundRepository.count());
        stats.put("totalReviews", reviewRepository.count());
        stats.put("totalTrains", trainRepository.count());
        stats.put("totalBuses", busRepository.count());
        stats.put("totalCabs", cabRepository.count());

        // Recent bookings (last 10)
        List<Booking> recentBookings = bookingRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        stats.put("recentBookings", recentBookings.stream().map(this::mapBookingToSummary).toList());

        // Recent users (last 10)
        List<User> recentUsers = userRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        stats.put("recentUsers", recentUsers.stream().map(this::mapUserToSummary).toList());

        // Recent payments (last 10)
        List<Payment> recentPayments = paymentRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        stats.put("recentPayments", recentPayments.stream().map(this::mapPaymentToSummary).toList());

        // Recent refunds (last 10)
        List<Refund> recentRefunds = refundRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        stats.put("recentRefunds", recentRefunds.stream().map(this::mapRefundToSummary).toList());

        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    // ==================== BOOKINGS ====================

    @Transactional(readOnly = true)
    @GetMapping("/bookings")
    public ResponseEntity<Map<String, Object>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        Page<Booking> bookings;
        if (status != null && !status.isEmpty()) {
            bookings = bookingRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            bookings = bookingRepository.findAll(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", bookings.map(this::mapBookingToSummary)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/bookings/{reference}")
    public ResponseEntity<Map<String, Object>> getBookingDetail(@PathVariable String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return ResponseEntity.ok(Map.of("success", true, "data", mapBookingToDetail(booking)));
    }

    // ==================== USERS ====================

    @Transactional(readOnly = true)
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(Map.of("success", true, "data", users.map(this::mapUserToSummary)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new com.travelplatform.exception.ResourceNotFoundException("User", "id", id));
        Map<String, Object> detail = mapUserToSummary(user);
        detail.put("bookingCount", bookingRepository.count());
        return ResponseEntity.ok(Map.of("success", true, "data", detail));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> changeUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {

        String newRoleStr = body.get("role");
        if (newRoleStr == null || (!newRoleStr.equals("ADMIN") && !newRoleStr.equals("USER"))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid role. Must be ADMIN or USER."));
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new com.travelplatform.exception.ResourceNotFoundException("User", "id", id));

        // Prevent self-role escalation
        String currentAdminEmail = principal.getUsername();
        if (targetUser.getEmail().equals(currentAdminEmail)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "You cannot change your own role."));
        }

        com.travelplatform.entity.Role newRole = com.travelplatform.entity.Role.valueOf(newRoleStr);
        com.travelplatform.entity.Role oldRole = targetUser.getRole();

        // Prevent demoting the last ADMIN
        if (oldRole == com.travelplatform.entity.Role.ADMIN && newRole == com.travelplatform.entity.Role.USER) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == com.travelplatform.entity.Role.ADMIN)
                    .count();
            if (adminCount <= 1) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot demote the last remaining admin account."));
            }
        }

        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        // Audit log
        AuditLog auditLog = new AuditLog(
                "ROLE_CHANGE",
                currentAdminEmail,
                targetUser.getId(),
                targetUser.getEmail(),
                oldRole.name(),
                newRole.name()
        );
        auditLog.setDetails("Role changed from " + oldRole.name() + " to " + newRole.name() + " by admin " + currentAdminEmail);
        auditLogRepository.save(auditLog);

        Map<String, Object> result = mapUserToSummary(targetUser);
        result.put("message", "User role updated successfully");
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // ==================== AUDIT LOG ====================

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(Map.of("success", true, "data", logs));
    }

    // ==================== FLIGHTS ====================

    @GetMapping("/flights")
    public ResponseEntity<Map<String, Object>> getFlights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Flight> flights = flightRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var mapped = flights.map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("flightNumber", f.getFlightNumber());
            m.put("originCode", f.getOriginCode());
            m.put("destinationCode", f.getDestinationCode());
            m.put("departureTime", f.getDepartureTime());
            m.put("arrivalTime", f.getArrivalTime());
            m.put("basePrice", f.getEconomyBasePrice() != null ? f.getEconomyBasePrice() : f.getEconomyPrice());
            m.put("active", f.isActive());
            m.put("airlineName", f.getAirline() != null ? f.getAirline().getName() : null);
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== HOTELS ====================

    @GetMapping("/hotels")
    public ResponseEntity<Map<String, Object>> getHotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Hotel> hotels = hotelRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var mapped = hotels.map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("name", h.getName());
            m.put("city", h.getCity());
            m.put("starRating", h.getStarRating());
            m.put("imageUrl", h.getImageUrl());
            m.put("active", h.isActive());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<Map<String, Object>> getHotelDetail(@PathVariable Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new RuntimeException("Hotel not found"));
        List<Room> rooms = roomRepository.findByHotelId(id);
        Map<String, Object> detail = new HashMap<>();
        detail.put("hotel", hotel);
        detail.put("rooms", rooms);
        return ResponseEntity.ok(Map.of("success", true, "data", detail));
    }

    // ==================== HOLIDAYS ====================

    @GetMapping("/holidays")
    public ResponseEntity<Map<String, Object>> getHolidays(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<HolidayPackage> holidays = holidayPackageRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var mapped = holidays.map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("title", h.getTitle());
            m.put("destination", h.getDestination());
            m.put("durationDays", h.getDurationDays());
            m.put("price", h.getPricePerPerson());
            m.put("tripType", h.getTripType());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== TRAINS ====================

    @GetMapping("/trains")
    public ResponseEntity<Map<String, Object>> getTrains(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Train> trains = trainRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var mapped = trains.map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("trainName", t.getTrainName());
            m.put("trainNumber", t.getTrainNumber());
            m.put("originStation", t.getOriginCode());
            m.put("destinationStation", t.getDestinationCode());
            m.put("departureTime", t.getDepartureTime());
            m.put("arrivalTime", t.getArrivalTime());
            m.put("active", t.getActive());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== BUSES ====================

    @GetMapping("/buses")
    public ResponseEntity<Map<String, Object>> getBuses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Bus> buses = busRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var mapped = buses.map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("busName", b.getBusNumber());
            m.put("busNumber", b.getBusNumber());
            m.put("operator", b.getOperatorName());
            m.put("busType", b.getBusType());
            m.put("origin", b.getOriginCode());
            m.put("destination", b.getDestinationCode());
            m.put("basePrice", b.getBasePrice());
            m.put("active", b.getActive());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== CABS ====================

    @GetMapping("/cabs")
    public ResponseEntity<Map<String, Object>> getCabs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Cab> cabs = cabRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var mapped = cabs.map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("vehicleName", c.getVehicleName());
            m.put("vehicleType", c.getVehicleType());
            m.put("category", c.getVehicleType());
            m.put("capacity", c.getCapacity());
            m.put("pricePerKm", c.getPerKmRate());
            m.put("active", c.getActive());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== PAYMENTS ====================

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Payment> payments = paymentRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var mapped = payments.map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("paymentId", p.getPaymentId());
            m.put("amount", p.getAmount());
            m.put("currency", p.getCurrency());
            m.put("status", p.getStatus());
            m.put("paymentMethod", p.getPaymentMethod());
            m.put("createdAt", p.getCreatedAt());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== REFUNDS ====================

    @GetMapping("/refunds")
    public ResponseEntity<Map<String, Object>> getRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Refund> refunds = refundRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var mapped = refunds.map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("refundId", r.getRefundId());
            m.put("refundAmount", r.getRefundAmount());
            m.put("originalAmount", r.getOriginalAmount());
            m.put("refundPercentage", r.getRefundPercentage());
            m.put("currency", r.getCurrency());
            m.put("status", r.getStatus());
            m.put("cancellationReason", r.getCancellationReason());
            m.put("createdAt", r.getCreatedAt());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    // ==================== REVIEWS ====================

    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Review> reviews = reviewRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var mapped = reviews.map(rv -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", rv.getId());
            m.put("title", rv.getTitle());
            m.put("rating", rv.getRating());
            m.put("text", rv.getText());
            m.put("status", rv.getStatus());
            m.put("helpfulCount", rv.getHelpfulCount());
            m.put("reportCount", rv.getReportCount());
            m.put("verifiedBooking", rv.isVerifiedBooking());
            m.put("createdAt", rv.getCreatedAt());
            return m;
        });
        return ResponseEntity.ok(Map.of("success", true, "data", mapped));
    }

    @GetMapping("/reviews/moderation-queue")
    public ResponseEntity<Map<String, Object>> getModerationQueue() {
        return ResponseEntity.ok(Map.of("success", true, "data", reviewService.getModerationQueue()));
    }

    @PutMapping("/reviews/{id}/moderate")
    public ResponseEntity<Map<String, Object>> moderateReview(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        String action = body.get("action"); // "APPROVE", "HIDE", "REMOVE"
        User admin = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        reviewService.moderateReview(id, action, admin.getId());
        auditLogRepository.save(new AuditLog("REVIEW_MODERATED", principal.getUsername(), id, "Review Moderation", "PENDING", action));
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Review moderated successfully"));
    }

    // ==================== ANALYTICS ====================

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        List<Booking> allBookings = bookingRepository.findAll();

        // Bookings by type
        Map<String, Long> byType = new HashMap<>();
        allBookings.forEach(b -> byType.merge(b.getBookingType(), 1L, Long::sum));
        analytics.put("bookingsByType", byType);

        // Bookings by status
        Map<String, Long> byStatus = new HashMap<>();
        allBookings.forEach(b -> byStatus.merge(b.getStatus(), 1L, Long::sum));
        analytics.put("bookingsByStatus", byStatus);

        // Revenue by type
        Map<String, BigDecimal> revenueByType = new HashMap<>();
        allBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
                .forEach(b -> revenueByType.merge(b.getBookingType(), b.getTotalAmount(), BigDecimal::add));
        analytics.put("revenueByType", revenueByType);

        // Total metrics
        analytics.put("totalRevenue", allBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        analytics.put("totalBookings", allBookings.size());
        analytics.put("totalUsers", userRepository.count());

        // Average booking value
        long confirmedCount = allBookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())).count();
        analytics.put("averageBookingValue", confirmedCount > 0
                ? analytics.get("totalRevenue").toString().isEmpty() ? BigDecimal.ZERO :
                  ((BigDecimal) analytics.get("totalRevenue")).divide(BigDecimal.valueOf(confirmedCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Cancellation rate
        long cancelled = allBookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        analytics.put("cancellationRate", allBookings.isEmpty() ? 0 :
                (double) cancelled / allBookings.size() * 100);

        // Popular routes (top 10)
        Map<String, Long> routes = new HashMap<>();
        allBookings.stream()
                .filter(b -> b.getFlight() != null)
                .forEach(b -> {
                    String route = b.getFlight().getOriginCode() + " → " + b.getFlight().getDestinationCode();
                    routes.merge(route, 1L, Long::sum);
                });
        analytics.put("popularRoutes", routes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .toList());

        // Bookings over time (last 30 days)
        Map<String, Long> bookingsOverTime = new HashMap<>();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        allBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(thirtyDaysAgo))
                .forEach(b -> {
                    String date = b.getCreatedAt().toLocalDate().toString();
                    bookingsOverTime.merge(date, 1L, Long::sum);
                });
        analytics.put("bookingsOverTime", bookingsOverTime);

        return ResponseEntity.ok(Map.of("success", true, "data", analytics));
    }

    // ==================== FLIGHT CRUD ====================

    @PostMapping("/flights")
    public ResponseEntity<Map<String, Object>> createFlight(@RequestBody Flight flight,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        if (flightRepository.findByFlightNumber(flight.getFlightNumber()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Flight number already exists"));
        }
        Flight saved = flightRepository.save(flight);
        auditLogRepository.save(new AuditLog("FLIGHT_CREATED", principal.getUsername(), saved.getId(), saved.getFlightNumber(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/flights/{id}")
    public ResponseEntity<Map<String, Object>> updateFlight(@PathVariable Long id, @RequestBody Flight updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Flight flight = flightRepository.findById(id).orElseThrow(() -> new RuntimeException("Flight not found"));
        flight.setFlightNumber(updates.getFlightNumber());
        flight.setAirline(updates.getAirline());
        flight.setOrigin(updates.getOrigin());
        flight.setDestination(updates.getDestination());
        flight.setOriginCode(updates.getOriginCode());
        flight.setDestinationCode(updates.getDestinationCode());
        flight.setDepartureTime(updates.getDepartureTime());
        flight.setArrivalTime(updates.getArrivalTime());
        flight.setDepartureDate(updates.getDepartureDate());
        flight.setDurationMinutes(updates.getDurationMinutes());
        flight.setStops(updates.getStops());
        flight.setEconomyPrice(updates.getEconomyPrice());
        flight.setPremiumEconomyPrice(updates.getPremiumEconomyPrice());
        flight.setBusinessPrice(updates.getBusinessPrice());
        flight.setFirstClassPrice(updates.getFirstClassPrice());
        flight.setActive(updates.isActive());
        Flight saved = flightRepository.save(flight);
        auditLogRepository.save(new AuditLog("FLIGHT_UPDATED", principal.getUsername(), saved.getId(), saved.getFlightNumber(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/flights/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFlight(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Flight flight = flightRepository.findById(id).orElseThrow(() -> new RuntimeException("Flight not found"));
        flight.setActive(!flight.isActive());
        Flight saved = flightRepository.save(flight);
        auditLogRepository.save(new AuditLog("FLIGHT_TOGGLED", principal.getUsername(), saved.getId(), saved.getFlightNumber(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", saved.getId(), "active", saved.isActive())));
    }

    @DeleteMapping("/flights/{id}")
    public ResponseEntity<Map<String, Object>> deleteFlight(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Flight flight = flightRepository.findById(id).orElseThrow(() -> new RuntimeException("Flight not found"));
        boolean hasBookings = bookingRepository.existsByFlightAndStatus(flight, "CONFIRMED") ||
                              bookingRepository.existsByFlightAndStatus(flight, "COMPLETED");
        if (hasBookings) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot delete flight with confirmed/completed bookings. Deactivate instead."));
        }
        flightRepository.deleteById(id);
        auditLogRepository.save(new AuditLog("FLIGHT_DELETED", principal.getUsername(), id, flight.getFlightNumber(), "ACTIVE", "DELETED"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Flight deleted"));
    }

    // ==================== HOTEL CRUD ====================

    @PostMapping("/hotels")
    public ResponseEntity<Map<String, Object>> createHotel(@RequestBody Hotel hotel,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Hotel saved = hotelRepository.save(hotel);
        auditLogRepository.save(new AuditLog("HOTEL_CREATED", principal.getUsername(), saved.getId(), saved.getName(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<Map<String, Object>> updateHotel(@PathVariable Long id, @RequestBody Hotel updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new RuntimeException("Hotel not found"));
        hotel.setName(updates.getName());
        hotel.setCity(updates.getCity());
        hotel.setDescription(updates.getDescription());
        hotel.setAddress(updates.getAddress());
        hotel.setStarRating(updates.getStarRating());
        hotel.setActive(updates.isActive());
        if (updates.getAmenities() != null) hotel.setAmenities(updates.getAmenities());
        Hotel saved = hotelRepository.save(hotel);
        auditLogRepository.save(new AuditLog("HOTEL_UPDATED", principal.getUsername(), saved.getId(), saved.getName(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/hotels/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleHotel(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new RuntimeException("Hotel not found"));
        hotel.setActive(!hotel.isActive());
        Hotel saved = hotelRepository.save(hotel);
        auditLogRepository.save(new AuditLog("HOTEL_TOGGLED", principal.getUsername(), saved.getId(), saved.getName(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", saved.getId(), "active", saved.isActive())));
    }

    // ==================== ROOM CRUD ====================

    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<Map<String, Object>> getHotelRooms(@PathVariable Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return ResponseEntity.ok(Map.of("success", true, "data", rooms));
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(@PathVariable Long hotelId, @RequestBody Room room,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new RuntimeException("Hotel not found"));
        room.setHotel(hotel);
        if (room.getBasePrice() == null) room.setBasePrice(room.getPricePerNight());
        Room saved = roomRepository.save(room);
        auditLogRepository.save(new AuditLog("ROOM_CREATED", principal.getUsername(), saved.getId(), saved.getName(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> updateRoom(@PathVariable Long id, @RequestBody Room updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));
        room.setName(updates.getName());
        room.setRoomType(updates.getRoomType());
        room.setDescription(updates.getDescription());
        room.setPricePerNight(updates.getPricePerNight());
        room.setMaxGuests(updates.getMaxGuests());
        room.setBedCount(updates.getBedCount());
        room.setBedType(updates.getBedType());
        room.setSizeSqm(updates.getSizeSqm());
        room.setTotalRooms(updates.getTotalRooms());
        room.setAvailableRooms(updates.getAvailableRooms());
        room.setActive(updates.isActive());
        Room saved = roomRepository.save(room);
        auditLogRepository.save(new AuditLog("ROOM_UPDATED", principal.getUsername(), saved.getId(), saved.getName(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/rooms/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRoom(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));
        room.setActive(!room.isActive());
        Room saved = roomRepository.save(room);
        auditLogRepository.save(new AuditLog("ROOM_TOGGLED", principal.getUsername(), saved.getId(), saved.getName(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", saved.getId(), "active", saved.isActive())));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> deleteRoom(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));
        roomRepository.deleteById(id);
        auditLogRepository.save(new AuditLog("ROOM_DELETED", principal.getUsername(), id, room.getName(), "ACTIVE", "DELETED"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Room deleted"));
    }

    // ==================== BULK OPERATIONS ====================

    @PutMapping("/bulk/toggle")
    public ResponseEntity<Map<String, Object>> bulkToggle(@RequestBody Map<String, Object> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        java.util.List<Integer> ids = (java.util.List<Integer>) body.get("ids");
        boolean active = Boolean.TRUE.equals(body.get("active"));
        int updated = 0;
        for (Integer id : ids) {
            try {
                switch (type) {
                    case "flights" -> {
                        Flight f = flightRepository.findById(id.longValue()).orElse(null);
                        if (f != null) { f.setActive(active); flightRepository.save(f); updated++; }
                    }
                    case "hotels" -> {
                        Hotel h = hotelRepository.findById(id.longValue()).orElse(null);
                        if (h != null) { h.setActive(active); hotelRepository.save(h); updated++; }
                    }
                    case "users" -> {
                        User u = userRepository.findById(id.longValue()).orElse(null);
                        if (u != null && !u.getEmail().equals(principal.getUsername())) { u.setEnabled(active); userRepository.save(u); updated++; }
                    }
                }
            } catch (Exception e) { /* skip individual failures */ }
        }
        auditLogRepository.save(new AuditLog("BULK_TOGGLE", principal.getUsername(), 0L, type + ":" + ids.size(), "N/A", active ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "updated", updated, "total", ids.size()));
    }

    // ==================== GLOBAL SEARCH ====================

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> globalSearch(@RequestParam String q) {
        Map<String, Object> results = new HashMap<>();
        String query = q.toLowerCase();

        // Users
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .filter(u -> u.getName().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query))
                .limit(5)
                .map(u -> { Map<String, Object> m = new HashMap<>(); m.put("id", u.getId()); m.put("name", u.getName()); m.put("email", u.getEmail()); m.put("role", u.getRole().name()); m.put("type", "USER"); return m; })
                .toList();
        results.put("users", users);

        // Bookings
        List<Map<String, Object>> bookings = bookingRepository.findAll().stream()
                .filter(b -> (b.getBookingReference() != null && b.getBookingReference().toLowerCase().contains(query)) ||
                             (b.getUser() != null && b.getUser().getEmail().toLowerCase().contains(query)))
                .limit(5)
                .map(b -> { Map<String, Object> m = new HashMap<>(); m.put("id", b.getId()); m.put("reference", b.getBookingReference()); m.put("type", b.getBookingType()); m.put("status", b.getStatus()); m.put("userName", b.getUser() != null ? b.getUser().getName() : ""); return m; })
                .toList();
        results.put("bookings", bookings);

        // Flights
        List<Map<String, Object>> flights = flightRepository.findAll().stream()
                .filter(f -> f.getFlightNumber().toLowerCase().contains(query) || f.getOriginCode().toLowerCase().contains(query) || f.getDestinationCode().toLowerCase().contains(query))
                .limit(5)
                .map(f -> { Map<String, Object> m = new HashMap<>(); m.put("id", f.getId()); m.put("flightNumber", f.getFlightNumber()); m.put("route", f.getOriginCode() + "→" + f.getDestinationCode()); m.put("type", "FLIGHT"); return m; })
                .toList();
        results.put("flights", flights);

        // Hotels
        List<Map<String, Object>> hotels = hotelRepository.findAll().stream()
                .filter(h -> h.getName().toLowerCase().contains(query) || h.getCity().toLowerCase().contains(query))
                .limit(5)
                .map(h -> { Map<String, Object> m = new HashMap<>(); m.put("id", h.getId()); m.put("name", h.getName()); m.put("city", h.getCity()); m.put("type", "HOTEL"); return m; })
                .toList();
        results.put("hotels", hotels);

        return ResponseEntity.ok(Map.of("success", true, "data", results));
    }

    // ==================== ENHANCED ANALYTICS ====================

    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        List<Booking> allBookings = bookingRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        // Revenue periods
        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
                .map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalRevenue", totalRevenue);

        BigDecimal revenue30d = allBookings.stream()
                .filter(b -> ("CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())) && b.getCreatedAt() != null && b.getCreatedAt().isAfter(now.minusDays(30)))
                .map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenue30Days", revenue30d);

        BigDecimal revenue7d = allBookings.stream()
                .filter(b -> ("CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())) && b.getCreatedAt() != null && b.getCreatedAt().isAfter(now.minusDays(7)))
                .map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenue7Days", revenue7d);

        // Bookings periods
        long totalBookings = allBookings.size();
        summary.put("totalBookings", totalBookings);
        summary.put("bookings30Days", allBookings.stream().filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(now.minusDays(30))).count());
        summary.put("bookings7Days", allBookings.stream().filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(now.minusDays(7))).count());
        summary.put("bookingsToday", allBookings.stream().filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().equals(now.toLocalDate())).count());

        // Status breakdown
        Map<String, Long> byStatus = new HashMap<>();
        allBookings.forEach(b -> byStatus.merge(b.getStatus(), 1L, Long::sum));
        summary.put("byStatus", byStatus);

        // Module breakdown
        Map<String, Long> byModule = new HashMap<>();
        allBookings.forEach(b -> byModule.merge(b.getBookingType(), 1L, Long::sum));
        summary.put("byModule", byModule);

        // Refunds
        summary.put("totalRefunds", refundRepository.count());
        BigDecimal refundAmount = refundRepository.findAll().stream()
                .map(Refund::getRefundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("refundAmount", refundAmount);

        // Avg booking
        long confirmedCount = allBookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())).count();
        summary.put("avgBookingValue", confirmedCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(confirmedCount), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // Users
        summary.put("totalUsers", userRepository.count());
        summary.put("totalFlights", flightRepository.count());
        summary.put("totalHotels", hotelRepository.count());

        // Cancellation rate
        long cancelled = allBookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        summary.put("cancellationRate", totalBookings > 0 ? (double) cancelled / totalBookings * 100 : 0);

        // Bookings over time (last 30 days)
        Map<String, Long> bookingsOverTime = new HashMap<>();
        Map<String, BigDecimal> revenueOverTime = new HashMap<>();
        allBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(now.minusDays(30)))
                .forEach(b -> {
                    String date = b.getCreatedAt().toLocalDate().toString();
                    bookingsOverTime.merge(date, 1L, Long::sum);
                    if ("CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())) {
                        revenueOverTime.merge(date, b.getTotalAmount(), BigDecimal::add);
                    }
                });
        summary.put("bookingsOverTime", bookingsOverTime);
        summary.put("revenueOverTime", revenueOverTime);

        return ResponseEntity.ok(Map.of("success", true, "data", summary));
    }

    // ==================== HOLIDAY CRUD ====================

    @PostMapping("/holidays")
    public ResponseEntity<Map<String, Object>> createHoliday(@RequestBody HolidayPackage holiday,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        HolidayPackage saved = holidayPackageRepository.save(holiday);
        auditLogRepository.save(new AuditLog("HOLIDAY_CREATED", principal.getUsername(), saved.getId(), saved.getTitle(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/holidays/{id}")
    public ResponseEntity<Map<String, Object>> updateHoliday(@PathVariable Long id, @RequestBody HolidayPackage updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        HolidayPackage holiday = holidayPackageRepository.findById(id).orElseThrow(() -> new RuntimeException("Holiday not found"));
        holiday.setTitle(updates.getTitle());
        holiday.setDescription(updates.getDescription());
        holiday.setDestination(updates.getDestination());
        holiday.setPricePerPerson(updates.getPricePerPerson());
        holiday.setDurationDays(updates.getDurationDays());
        holiday.setTripType(updates.getTripType());
        HolidayPackage saved = holidayPackageRepository.save(holiday);
        auditLogRepository.save(new AuditLog("HOLIDAY_UPDATED", principal.getUsername(), saved.getId(), saved.getTitle(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    // ==================== TRAIN CRUD ====================

    @PostMapping("/trains")
    public ResponseEntity<Map<String, Object>> createTrain(@RequestBody Train train,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Train saved = trainRepository.save(train);
        auditLogRepository.save(new AuditLog("TRAIN_CREATED", principal.getUsername(), saved.getId(), saved.getTrainName(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/trains/{id}")
    public ResponseEntity<Map<String, Object>> updateTrain(@PathVariable Long id, @RequestBody Train updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Train train = trainRepository.findById(id).orElseThrow(() -> new RuntimeException("Train not found"));
        train.setTrainName(updates.getTrainName());
        train.setTrainNumber(updates.getTrainNumber());
        train.setTrainType(updates.getTrainType());
        train.setOriginCode(updates.getOriginCode());
        train.setDestinationCode(updates.getDestinationCode());
        train.setDepartureTime(updates.getDepartureTime());
        train.setArrivalTime(updates.getArrivalTime());
        train.setActive(updates.getActive());
        Train saved = trainRepository.save(train);
        auditLogRepository.save(new AuditLog("TRAIN_UPDATED", principal.getUsername(), saved.getId(), saved.getTrainName(), "N/A", saved.getActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    // ==================== BUS CRUD ====================

    @PostMapping("/buses")
    public ResponseEntity<Map<String, Object>> createBus(@RequestBody Bus bus,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Bus saved = busRepository.save(bus);
        auditLogRepository.save(new AuditLog("BUS_CREATED", principal.getUsername(), saved.getId(), saved.getOperatorName(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<Map<String, Object>> updateBus(@PathVariable Long id, @RequestBody Bus updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Bus bus = busRepository.findById(id).orElseThrow(() -> new RuntimeException("Bus not found"));
        bus.setOperatorName(updates.getOperatorName());
        bus.setBusType(updates.getBusType());
        bus.setOriginCode(updates.getOriginCode());
        bus.setDestinationCode(updates.getDestinationCode());
        bus.setBasePrice(updates.getBasePrice());
        bus.setActive(updates.getActive());
        Bus saved = busRepository.save(bus);
        auditLogRepository.save(new AuditLog("BUS_UPDATED", principal.getUsername(), saved.getId(), saved.getOperatorName(), "N/A", saved.getActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    // ==================== CAB CRUD ====================

    @PostMapping("/cabs")
    public ResponseEntity<Map<String, Object>> createCab(@RequestBody Cab cab,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Cab saved = cabRepository.save(cab);
        auditLogRepository.save(new AuditLog("CAB_CREATED", principal.getUsername(), saved.getId(), saved.getVehicleName(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/cabs/{id}")
    public ResponseEntity<Map<String, Object>> updateCab(@PathVariable Long id, @RequestBody Cab updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Cab cab = cabRepository.findById(id).orElseThrow(() -> new RuntimeException("Cab not found"));
        cab.setVehicleName(updates.getVehicleName());
        cab.setVehicleType(updates.getVehicleType());
        cab.setBaseFare(updates.getBaseFare());
        cab.setPerKmRate(updates.getPerKmRate());
        cab.setCapacity(updates.getCapacity());
        cab.setActive(updates.getActive());
        Cab saved = cabRepository.save(cab);
        auditLogRepository.save(new AuditLog("CAB_UPDATED", principal.getUsername(), saved.getId(), saved.getVehicleName(), "N/A", saved.getActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    // ==================== USER TOGGLE ====================

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleUser(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getEmail().equals(principal.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot disable your own account"));
        }
        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);
        auditLogRepository.save(new AuditLog("USER_TOGGLED", principal.getUsername(), saved.getId(), saved.getEmail(), "N/A", saved.isEnabled() ? "ACTIVE" : "DISABLED"));
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", saved.getId(), "enabled", saved.isEnabled())));
    }

    // ==================== EXPORT (CSV) ====================

    @Transactional(readOnly = true)
    @GetMapping("/export/{type}")
    public ResponseEntity<Map<String, Object>> exportData(@PathVariable String type,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        String csv;
        switch (type) {
            case "users" -> {
                List<User> users = userRepository.findAll();
                csv = "ID,Name,Email,Role,Enabled,Created\n";
                for (User u : users) {
                    csv += u.getId() + ",\"" + u.getName() + "\",\"" + u.getEmail() + "\"," + u.getRole() + "," + u.isEnabled() + "," + u.getCreatedAt() + "\n";
                }
            }
            case "bookings" -> {
                List<Booking> bookings = bookingRepository.findAll();
                csv = "ID,Reference,Type,Status,Amount,User,TravelDate,Created\n";
                for (Booking b : bookings) {
                    csv += b.getId() + ",\"" + b.getBookingReference() + "\",\"" + b.getBookingType() + "\",\"" + b.getStatus() + "\"," + b.getTotalAmount() + ",\"" + (b.getUser() != null ? b.getUser().getEmail() : "") + "\",\"" + b.getTravelDate() + "\",\"" + b.getCreatedAt() + "\"\n";
                }
            }
            case "flights" -> {
                List<Flight> flights = flightRepository.findAll();
                csv = "ID,FlightNumber,Origin,Destination,Departure,Arrival,Price,Active\n";
                for (Flight f : flights) {
                    csv += f.getId() + ",\"" + f.getFlightNumber() + "\",\"" + f.getOriginCode() + "\",\"" + f.getDestinationCode() + "\",\"" + f.getDepartureTime() + "\",\"" + f.getArrivalTime() + "\"," + f.getEconomyPrice() + "," + f.isActive() + "\n";
                }
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid export type. Use: users, bookings, flights"));
            }
        }
        auditLogRepository.save(new AuditLog("DATA_EXPORT", principal.getUsername(), 0L, type, "N/A", "EXPORTED"));
        return ResponseEntity.ok(Map.of("success", true, "data", csv, "type", type, "count", csv.split("\n").length - 1));
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> mapBookingToSummary(Booking b) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", b.getId());
        map.put("bookingReference", b.getBookingReference());
        map.put("bookingType", b.getBookingType());
        map.put("status", b.getStatus());
        map.put("totalAmount", b.getTotalAmount());
        map.put("passengerCount", b.getPassengerCount());
        map.put("createdAt", b.getCreatedAt());
        map.put("travelDate", b.getTravelDate());
        if (b.getUser() != null) {
            map.put("userName", b.getUser().getName());
            map.put("userEmail", b.getUser().getEmail());
            map.put("userId", b.getUser().getId());
        }
        if (b.getFlight() != null) {
            map.put("flightNumber", b.getFlight().getFlightNumber());
            map.put("airlineName", b.getFlight().getAirline() != null ? b.getFlight().getAirline().getName() : "");
            map.put("originCode", b.getFlight().getOriginCode());
            map.put("destinationCode", b.getFlight().getDestinationCode());
            map.put("departureTime", b.getFlight().getDepartureTime());
            map.put("cabinClass", b.getCabinClass());
        }
        if (b.getHotel() != null) {
            map.put("hotelName", b.getHotel().getName());
            map.put("checkInDate", b.getCheckInDate());
            map.put("checkOutDate", b.getCheckOutDate());
            map.put("numberOfNights", b.getNumberOfNights());
        }
        return map;
    }

    private Map<String, Object> mapBookingToDetail(Booking b) {
        Map<String, Object> map = mapBookingToSummary(b);
        map.put("paymentMethod", b.getPaymentMethod());
        map.put("paymentId", b.getPaymentId());
        map.put("refundAmount", b.getRefundAmount());
        map.put("cancellationReason", b.getCancellationReason());
        map.put("selectedSeatNumbers", b.getSelectedSeatNumbers());
        map.put("specialRequests", b.getSpecialRequests());
        map.put("updatedAt", b.getUpdatedAt());
        return map;
    }

    private Map<String, Object> mapUserToSummary(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("role", u.getRole());
        map.put("enabled", u.isEnabled());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapPaymentToSummary(Payment p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("paymentId", p.getPaymentId());
        map.put("amount", p.getAmount());
        map.put("paymentMethod", p.getPaymentMethod());
        map.put("status", p.getStatus());
        map.put("createdAt", p.getCreatedAt());
        if (p.getBooking() != null) {
            map.put("bookingReference", p.getBooking().getBookingReference());
            if (p.getBooking().getUser() != null) {
                map.put("userName", p.getBooking().getUser().getName());
                map.put("userEmail", p.getBooking().getUser().getEmail());
            }
        }
        return map;
    }

    // ==================== CANCELLATION & REFUNDS ====================

    @GetMapping("/cancellations/analytics")
    public ResponseEntity<Map<String, Object>> getCancellationAnalytics() {
        return ResponseEntity.ok(Map.of("success", true, "data", cancellationService.getCancellationReasonAnalytics()));
    }


    private Map<String, Object> mapRefundToSummary(Refund r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("refundId", r.getRefundId());
        map.put("refundAmount", r.getRefundAmount());
        map.put("originalAmount", r.getOriginalAmount());
        map.put("refundPercentage", r.getRefundPercentage());
        map.put("reason", r.getCancellationReason());
        map.put("comment", r.getCancellationComment());
        map.put("policy", r.getCancellationPolicy());
        map.put("status", r.getStatus());
        map.put("failureReason", r.getFailureReason());
        map.put("externalRefundId", r.getExternalRefundId());
        map.put("expectedCompletionAt", r.getExpectedCompletionAt());
        map.put("completedAt", r.getCompletedAt());
        map.put("createdAt", r.getCreatedAt());
        if (r.getBooking() != null) {
            map.put("bookingReference", r.getBooking().getBookingReference());
            map.put("bookingType", r.getBooking().getBookingType());
            if (r.getBooking().getUser() != null) {
                map.put("userName", r.getBooking().getUser().getName());
                map.put("userEmail", r.getBooking().getUser().getEmail());
            }
        }
        return map;
    }
}
