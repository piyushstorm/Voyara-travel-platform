package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.dto.booking.BookingRequest;
import com.travelplatform.dto.booking.BookingResponse;
import com.travelplatform.dto.booking.CancelBookingRequest;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.security.JwtTokenProvider;
import com.travelplatform.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        BookingResponse booking = bookingService.createBooking(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully", booking));
    }

    @GetMapping
    @Operation(summary = "Get all bookings for current user")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Page<BookingResponse> bookings = bookingService.getUserBookings(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming bookings")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getUpcomingBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Page<BookingResponse> bookings = bookingService.getUpcomingBookings(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Upcoming bookings", bookings));
    }

    @GetMapping("/past")
    @Operation(summary = "Get past bookings")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getPastBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Page<BookingResponse> bookings = bookingService.getPastBookings(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Past bookings", bookings));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get booking details by reference")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String reference,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        BookingResponse booking = bookingService.getBookingByReference(reference, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking found", booking));
    }

    @PostMapping("/{reference}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String reference,
            @RequestBody(required = false) CancelBookingRequest request,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String effectiveReason = request != null && request.getReason() != null ? request.getReason() : reason;
        String effectiveComment = request != null ? request.getComment() : null;
        BookingResponse booking = bookingService.cancelBooking(reference, user.getId(), effectiveReason, effectiveComment);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", booking));
    }
}
