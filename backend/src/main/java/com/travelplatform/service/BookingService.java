package com.travelplatform.service;

import com.travelplatform.dto.booking.BookingRequest;
import com.travelplatform.dto.booking.BookingResponse;
import com.travelplatform.entity.*;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.payment.PaymentGateway;
import com.travelplatform.payment.PaymentResult;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final CancellationService cancellationService;
    private final PriceFreezeService freezeService;
    private final FareOptionRepository fareOptionRepository;
    private final AddOnRepository addOnRepository;
    private final HolidayPackageRepository holidayPackageRepository;
    private final TrainRepository trainRepository;
    private final TrainSeatRepository trainSeatRepository;
    private final BusRepository busRepository;
    private final CabRepository cabRepository;
    private final CouponService couponService;
    private final RewardService rewardService;
    private final NotificationService notificationService;
    private final SeatSelectionService seatSelectionService;
    private final RoomSelectionService roomSelectionService;
    private final TripReadinessService readinessService;
    private final SmartTimelineService timelineService;
    private final TravelGuardianService guardianService;
    private final PaymentRepository paymentRepository;

    public BookingService(BookingRepository bookingRepository,
                          FlightRepository flightRepository,
                          HotelRepository hotelRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository,
                          PaymentGateway paymentGateway,
                          CancellationService cancellationService,
                          PriceFreezeService freezeService,
                          FareOptionRepository fareOptionRepository,
                          AddOnRepository addOnRepository,
                          HolidayPackageRepository holidayPackageRepository,
                          TrainRepository trainRepository,
                          TrainSeatRepository trainSeatRepository,
                          BusRepository busRepository,
                          CabRepository cabRepository,
                          CouponService couponService,
                          RewardService rewardService,
                          NotificationService notificationService,
                          SeatSelectionService seatSelectionService,
                          RoomSelectionService roomSelectionService,
                          TripReadinessService readinessService,
                          SmartTimelineService timelineService,
                          TravelGuardianService guardianService,
                          PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.cancellationService = cancellationService;
        this.freezeService = freezeService;
        this.fareOptionRepository = fareOptionRepository;
        this.addOnRepository = addOnRepository;
        this.holidayPackageRepository = holidayPackageRepository;
        this.trainRepository = trainRepository;
        this.trainSeatRepository = trainSeatRepository;
        this.busRepository = busRepository;
        this.cabRepository = cabRepository;
        this.couponService = couponService;
        this.rewardService = rewardService;
        this.notificationService = notificationService;
        this.seatSelectionService = seatSelectionService;
        this.roomSelectionService = roomSelectionService;
        this.readinessService = readinessService;
        this.timelineService = timelineService;
        this.guardianService = guardianService;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookingReference(Booking.generateReference());
        booking.setBookingType(request.getBookingType());
        booking.setStatus("PENDING");
        booking.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount;

        if ("FLIGHT".equalsIgnoreCase(request.getBookingType())) {
            totalAmount = createFlightBooking(booking, request, userId);
        } else if ("HOTEL".equalsIgnoreCase(request.getBookingType())) {
            totalAmount = createHotelBooking(booking, request, userId);
        } else if ("HOLIDAY".equalsIgnoreCase(request.getBookingType())) {
            totalAmount = createHolidayBooking(booking, request);
        } else if ("TRAIN".equalsIgnoreCase(request.getBookingType())) {
            totalAmount = createTrainBooking(booking, request);
        } else if ("BUS".equalsIgnoreCase(request.getBookingType())) {
            totalAmount = createBusBooking(booking, request);
        } else if ("CAB".equalsIgnoreCase(request.getBookingType())) {
            totalAmount = createCabBooking(booking, request);
        } else {
            throw new BadRequestException("Invalid booking type: " + request.getBookingType());
        }

        // Apply coupon discount if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Map<String, Object> couponResult = couponService.validateAndApplyCoupon(
                request.getCouponCode(), totalAmount, request.getBookingType(), user.getEmail());
            if (Boolean.TRUE.equals(couponResult.get("valid"))) {
                discountAmount = (BigDecimal) couponResult.get("discountAmount");
                totalAmount = (BigDecimal) couponResult.get("finalAmount");
                booking.setCouponCode(request.getCouponCode());
                booking.setDiscountAmount(discountAmount);
                booking.setOriginalAmount(totalAmount.add(discountAmount));
            }
        } else {
            booking.setOriginalAmount(totalAmount);
        }

        booking.setTotalAmount(totalAmount);
        booking = bookingRepository.save(booking);

        // Check for price freeze and apply if available
        if ("FLIGHT".equalsIgnoreCase(request.getBookingType()) && request.getFreezeId() != null) {
            try {
                BigDecimal frozenPrice = freezeService.useFreeze(
                    request.getFreezeId(), 
                    booking.getId(), 
                    userId, 
                    request.getFlightId(), 
                    request.getCabinClass()
                );
                // Recalculate base and add-ons with frozen price
                BigDecimal addOnsTotal = BigDecimal.ZERO;
                if (request.getAddOnIds() != null && !request.getAddOnIds().isEmpty()) {
                    for (Long addOnId : request.getAddOnIds()) {
                        AddOn addOn = addOnRepository.findById(addOnId)
                                .orElseThrow(() -> new ResourceNotFoundException("AddOn", "id", addOnId));
                        addOnsTotal = addOnsTotal.add(addOn.getPrice());
                    }
                }
                BigDecimal frozenTotal = frozenPrice.multiply(BigDecimal.valueOf(request.getPassengerCount()))
                        .add(addOnsTotal.multiply(BigDecimal.valueOf(request.getPassengerCount())));
                
                booking.setOriginalAmount(frozenTotal);

                if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
                    Map<String, Object> couponResult = couponService.validateAndApplyCoupon(
                        request.getCouponCode(), frozenTotal, request.getBookingType(), user.getEmail());
                    if (Boolean.TRUE.equals(couponResult.get("valid"))) {
                        BigDecimal disc = (BigDecimal) couponResult.get("discountAmount");
                        frozenTotal = (BigDecimal) couponResult.get("finalAmount");
                        booking.setDiscountAmount(disc);
                    }
                }
                
                booking.setTotalAmount(frozenTotal);
                booking = bookingRepository.save(booking);
            } catch (Exception e) {
                logger.warn("Price freeze {} could not be applied: {}", request.getFreezeId(), e.getMessage());
                throw new BadRequestException("Could not apply price freeze: " + e.getMessage());
            }
        }

        // Associate pre-verified payment or verify via gateway
        Payment existingPayment = null;
        if (request.getRazorpayPaymentId() != null && !request.getRazorpayPaymentId().isBlank()) {
            existingPayment = paymentRepository.findByRazorpayPaymentId(request.getRazorpayPaymentId()).orElse(null);
        }
        if (existingPayment == null && request.getRazorpayOrderId() != null && !request.getRazorpayOrderId().isBlank()) {
            existingPayment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElse(null);
        }
        if (existingPayment == null && request.getPaymentId() != null && !request.getPaymentId().isBlank()) {
            existingPayment = paymentRepository.findByPaymentId(request.getPaymentId()).orElse(null);
        }

        if (existingPayment != null && "COMPLETED".equalsIgnoreCase(existingPayment.getStatus())) {
            existingPayment.setBooking(booking);
            paymentRepository.save(existingPayment);
            booking.setPaymentId(existingPayment.getPaymentId());
            booking.setStatus("CONFIRMED");
            booking = bookingRepository.save(booking);
            logger.info("Booking {} linked to completed payment {}", booking.getBookingReference(), existingPayment.getPaymentId());
        } else {
            // Process payment via gateway fallback (simulated / mock mode)
            PaymentResult paymentResult = paymentGateway.verifyPayment(
                    request.getRazorpayOrderId() != null ? request.getRazorpayOrderId() : "order_simulated",
                    request.getPaymentId() != null ? request.getPaymentId() : Payment.generatePaymentId(),
                    "simulated_signature"
            );

            if (!paymentResult.success()) {
                booking.setStatus("FAILED");
                booking = bookingRepository.save(booking);
                throw new BadRequestException("Payment failed: " + paymentResult.failureReason());
            }

            booking.setPaymentId(paymentResult.paymentId());
            booking.setStatus("CONFIRMED");
            booking = bookingRepository.save(booking);
        }

        // Record coupon usage after successful payment
        if (booking.getCouponCode() != null && booking.getDiscountAmount() != null &&
                booking.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            couponService.recordUsage(booking.getCouponCode(), user.getEmail(),
                    booking.getId(), booking.getDiscountAmount());
        }

        // Award Voyara Rewards points for confirmed booking
        try {
            rewardService.awardBookingPoints(booking);
        } catch (Exception e) {
            logger.warn("Could not award reward points for booking {}: {}", booking.getBookingReference(), e.getMessage());
        }

        // Send booking confirmed notification
        try {
            notificationService.notifyBookingConfirmed(booking);
            notificationService.notifyPaymentSuccessful(booking, booking.getPaymentId());
        } catch (Exception e) {
            logger.warn("Could not send booking notification: {}", e.getMessage());
        }

        // Auto-generate differentiator data for the new booking
        try {
            readinessService.calculateReadiness(booking.getId());
        } catch (Exception e) {
            logger.warn("Could not generate readiness for booking {}: {}", booking.getBookingReference(), e.getMessage());
        }
        try {
            timelineService.generateTimeline(booking.getId());
        } catch (Exception e) {
            logger.warn("Could not generate timeline for booking {}: {}", booking.getBookingReference(), e.getMessage());
        }
        try {
            guardianService.analyzeAndGenerateAlerts(userId);
        } catch (Exception e) {
            logger.warn("Could not run guardian analysis for user {}: {}", userId, e.getMessage());
        }

        logger.info("Booking {} created for user {}", booking.getBookingReference(), userId);
        return mapToResponse(booking);
    }

    @Transactional
    public Page<BookingResponse> getUserBookings(Long userId, int page, int size) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional
    public Page<BookingResponse> getUpcomingBookings(Long userId, int page, int size) {
        return bookingRepository.findUpcomingBookings(userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional
    public Page<BookingResponse> getPastBookings(Long userId, int page, int size) {
        return bookingRepository.findPastBookings(userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional
    public BookingResponse getBookingByReference(String reference, Long userId) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", reference));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied: booking does not belong to this user");
        }

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(String reference, Long userId, String reason) {
        return cancelBooking(reference, userId, reason, null);
    }

    @Transactional
    public BookingResponse cancelBooking(String reference, Long userId, String reason, String comment) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", reference));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied");
        }

        // Delegate to CancellationService for refund calculation + processing
        cancellationService.cancelBooking(booking, reason, comment);

        // Send cancellation notification
        try {
            notificationService.notifyBookingCancelled(booking);
        } catch (Exception e) {
            logger.warn("Could not send cancellation notification: {}", e.getMessage());
        }

        logger.info("Booking {} cancelled by user {} with reason {} [comment: {}]", reference, userId, reason, comment);
        return mapToResponse(booking);
    }

    private BigDecimal createFlightBooking(Booking booking, BookingRequest request, Long userId) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));

        if (flight.getAvailableSeatsForClass(request.getCabinClass()) < request.getPassengerCount()) {
            throw new BadRequestException("Not enough seats available in " + request.getCabinClass());
        }

        booking.setFlight(flight);
        booking.setCabinClass(request.getCabinClass());
        booking.setPassengerCount(request.getPassengerCount());
        booking.setTravelDate(flight.getDepartureTime());

        // Process seat confirmation and calculate seat surcharges
        BigDecimal seatSurchargesTotal = BigDecimal.ZERO;
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            List<com.travelplatform.entity.Seat> confirmedSeats = seatSelectionService.confirmSeats(request.getSeatIds(), userId);
            String confirmedSeatNumbers = confirmedSeats.stream()
                    .map(com.travelplatform.entity.Seat::getSeatNumber)
                    .collect(java.util.stream.Collectors.joining(","));
            booking.setSelectedSeatNumbers(confirmedSeatNumbers);
            for (com.travelplatform.entity.Seat s : confirmedSeats) {
                if (s.getPremiumSurcharge() != null && s.getPremiumSurcharge().compareTo(BigDecimal.ZERO) > 0) {
                    seatSurchargesTotal = seatSurchargesTotal.add(s.getPremiumSurcharge());
                }
            }
        } else if (request.getSeatNumbers() != null) {
            booking.setSelectedSeatNumbers(request.getSeatNumbers());
        }

        // Calculate base fare from fare option or cabin class price
        BigDecimal baseFarePerPerson = flight.getPriceForClass(request.getCabinClass());
        String fareType = "STANDARD";

        if (request.getFareOptionId() != null) {
            FareOption fareOption = fareOptionRepository.findById(request.getFareOptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("FareOption", "id", request.getFareOptionId()));
            baseFarePerPerson = fareOption.calculatePrice(flight.getEconomyBasePrice());
            fareType = fareOption.getFareType();
        }

        // Calculate add-ons total
        BigDecimal addOnsTotal = BigDecimal.ZERO;
        List<Long> selectedAddOnIds = request.getAddOnIds();
        if (selectedAddOnIds != null && !selectedAddOnIds.isEmpty()) {
            for (Long addOnId : selectedAddOnIds) {
                AddOn addOn = addOnRepository.findById(addOnId)
                        .orElseThrow(() -> new ResourceNotFoundException("AddOn", "id", addOnId));
                addOnsTotal = addOnsTotal.add(addOn.getPrice());
            }
        }

        // Update booked seats
        switch (request.getCabinClass().toUpperCase()) {
            case "ECONOMY" -> flight.setBookedSeatsEconomy(flight.getBookedSeatsEconomy() + request.getPassengerCount());
            case "PREMIUM_ECONOMY" -> flight.setBookedSeatsPremiumEconomy(flight.getBookedSeatsPremiumEconomy() + request.getPassengerCount());
            case "BUSINESS" -> flight.setBookedSeatsBusiness(flight.getBookedSeatsBusiness() + request.getPassengerCount());
            case "FIRST" -> flight.setBookedSeatsFirst(flight.getBookedSeatsFirst() + request.getPassengerCount());
        }
        flightRepository.save(flight);

        // Total = (base fare × passengers) + add-ons (per-passenger) + seat surcharges
        BigDecimal total = baseFarePerPerson.multiply(BigDecimal.valueOf(request.getPassengerCount()))
                .add(addOnsTotal.multiply(BigDecimal.valueOf(request.getPassengerCount())))
                .add(seatSurchargesTotal);

        return total;
    }

    private BigDecimal createHotelBooking(Booking booking, BookingRequest request, Long userId) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", request.getHotelId()));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        // Concurrency-safe room confirmation & inventory decrement
        roomSelectionService.confirmRoom(request.getRoomId(), userId);

        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setNumberOfNights(request.getNumberOfNights());
        booking.setSpecialRequests(request.getSpecialRequests());

        if (request.getCheckInDate() != null) {
            booking.setCheckInDate(LocalDateTime.parse(request.getCheckInDate() + "T14:00:00"));
        }
        if (request.getCheckOutDate() != null) {
            booking.setCheckOutDate(LocalDateTime.parse(request.getCheckOutDate() + "T11:00:00"));
            booking.setTravelDate(booking.getCheckInDate());
        }

        return room.getPricePerNight().multiply(BigDecimal.valueOf(request.getNumberOfNights()));
    }

    private BigDecimal createHolidayBooking(Booking booking, BookingRequest request) {
        if (request.getHolidayPackageId() == null) {
            throw new BadRequestException("Holiday package ID is required");
        }
        HolidayPackage pkg = holidayPackageRepository.findById(request.getHolidayPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Holiday Package", "id", request.getHolidayPackageId()));

        booking.setTravelDate(LocalDateTime.now());
        int travellers = request.getNumberOfTravellers() > 0 ? request.getNumberOfTravellers() : 1;
        booking.setPassengerCount(travellers);
        booking.setSpecialRequests(request.getSpecialRequests());

        return pkg.getPricePerPerson().multiply(BigDecimal.valueOf(travellers));
    }

    private BigDecimal createTrainBooking(Booking booking, BookingRequest request) {
        if (request.getTrainId() == null) {
            throw new BadRequestException("Train ID is required");
        }
        Train train = trainRepository.findById(request.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train", "id", request.getTrainId()));

        booking.setTravelDate(LocalDateTime.now());
        booking.setPassengerCount(request.getPassengerCount());
        booking.setSpecialRequests(request.getSpecialRequests());

        // Find fare for the class
        BigDecimal farePerPerson = BigDecimal.valueOf(500); // default
        for (TrainSeat seat : train.getSeats()) {
            if (seat.getClassCode().equalsIgnoreCase(request.getTrainClass())) {
                farePerPerson = seat.getFare();
                break;
            }
        }
        return farePerPerson.multiply(BigDecimal.valueOf(request.getPassengerCount()));
    }

    private BigDecimal createBusBooking(Booking booking, BookingRequest request) {
        if (request.getBusId() == null) {
            throw new BadRequestException("Bus ID is required");
        }
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", request.getBusId()));

        booking.setTravelDate(LocalDateTime.now());
        booking.setPassengerCount(request.getPassengerCount());
        booking.setSpecialRequests(request.getSpecialRequests());

        return bus.getBasePrice().multiply(BigDecimal.valueOf(request.getPassengerCount()));
    }

    private BigDecimal createCabBooking(Booking booking, BookingRequest request) {
        if (request.getCabId() == null) {
            throw new BadRequestException("Cab ID is required");
        }
        Cab cab = cabRepository.findById(request.getCabId())
                .orElseThrow(() -> new ResourceNotFoundException("Cab", "id", request.getCabId()));

        booking.setTravelDate(LocalDateTime.now());
        booking.setPassengerCount(1);
        booking.setSpecialRequests(request.getSpecialRequests());

        return cab.getBaseFare();
    }

    private BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setBookingType(booking.getBookingType());
        response.setStatus(booking.getStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setOriginalAmount(booking.getOriginalAmount());
        response.setCouponCode(booking.getCouponCode());
        response.setRefundAmount(booking.getRefundAmount());
        response.setPaymentId(booking.getPaymentId());
        response.setPaymentMethod(booking.getPaymentMethod());
        response.setPassengerCount(booking.getPassengerCount());
        response.setCancellationReason(booking.getCancellationReason());
        response.setCreatedAt(booking.getCreatedAt());

        if (booking.getFlight() != null) {
            response.setFlightId(booking.getFlight().getId());
            response.setFlightNumber(booking.getFlight().getFlightNumber());
            response.setAirlineName(booking.getFlight().getAirline().getName());
            response.setOriginCode(booking.getFlight().getOriginCode());
            response.setDestinationCode(booking.getFlight().getDestinationCode());
            response.setDepartureTime(booking.getFlight().getDepartureTime());
            response.setArrivalTime(booking.getFlight().getArrivalTime());
            response.setDurationMinutes(booking.getFlight().getDurationMinutes());
            response.setStops(booking.getFlight().getStops());
            response.setCabinClass(booking.getCabinClass());
            response.setSeatNumbers(booking.getSelectedSeatNumbers());
        }

        if (booking.getHotel() != null) {
            response.setHotelName(booking.getHotel().getName());
            response.setCheckInDate(booking.getCheckInDate());
            response.setCheckOutDate(booking.getCheckOutDate());
            response.setNumberOfNights(booking.getNumberOfNights());
            response.setSpecialRequests(booking.getSpecialRequests());
        }
        if (booking.getRoom() != null) {
            response.setRoomType(booking.getRoom().getRoomType());
            response.setRoomName(booking.getRoom().getName());
        }

        // Get payment status
        if (booking.getPaymentId() != null) {
            response.setPaymentStatus("COMPLETED"); // Simplified for Phase 2
        }

        return response;
    }
}
