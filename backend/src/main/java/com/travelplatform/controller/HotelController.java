package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.dto.hotel.HotelSearchRequest;
import com.travelplatform.dto.hotel.HotelSearchResponse;
import com.travelplatform.entity.Room;
import com.travelplatform.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@Tag(name = "Hotels", description = "Hotel search, detail, and room endpoints")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search hotels with filters")
    public ResponseEntity<ApiResponse<Page<HotelSearchResponse>>> searchHotels(
            @RequestParam String city,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(defaultValue = "2") int guests,
            @RequestParam(defaultValue = "1") int rooms,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) Integer minStar,
            @RequestParam(required = false) Integer maxStar,
            @RequestParam(required = false) String amenity,
            @RequestParam(defaultValue = "startingPrice") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (city == null || city.trim().isEmpty()) {
            throw new com.travelplatform.exception.BadRequestException("City is required");
        }

        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity(city.trim());
        request.setGuests(guests);
        request.setRooms(rooms);
        request.setAmenity(amenity);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);
        request.setPage(page);
        request.setSize(size);

        if (checkIn != null && !checkIn.trim().isEmpty()) {
            try {
                java.time.LocalDate parsedCheckIn = java.time.LocalDate.parse(checkIn.trim());
                if (parsedCheckIn.isBefore(java.time.LocalDate.now())) {
                    throw new com.travelplatform.exception.BadRequestException("Check-in date cannot be in the past");
                }
                request.setCheckIn(parsedCheckIn);
            } catch (java.time.format.DateTimeParseException e) {
                throw new com.travelplatform.exception.BadRequestException("Invalid check-in date format. Please use YYYY-MM-DD");
            }
        }
        if (checkOut != null && !checkOut.trim().isEmpty()) {
            try {
                java.time.LocalDate parsedCheckOut = java.time.LocalDate.parse(checkOut.trim());
                if (request.getCheckIn() != null && parsedCheckOut.isBefore(request.getCheckIn())) {
                    throw new com.travelplatform.exception.BadRequestException("Check-out date must be on or after check-in date");
                } else if (parsedCheckOut.isBefore(java.time.LocalDate.now())) {
                    throw new com.travelplatform.exception.BadRequestException("Check-out date cannot be in the past");
                }
                request.setCheckOut(parsedCheckOut);
            } catch (java.time.format.DateTimeParseException e) {
                throw new com.travelplatform.exception.BadRequestException("Invalid check-out date format. Please use YYYY-MM-DD");
            }
        }
        if (minPrice != null && !minPrice.isEmpty()) {
            request.setMinPrice(new BigDecimal(minPrice));
        }
        if (maxPrice != null && !maxPrice.isEmpty()) {
            request.setMaxPrice(new BigDecimal(maxPrice));
        }
        if (minStar != null) request.setMinStar(minStar);
        if (maxStar != null) request.setMaxStar(maxStar);

        Page<HotelSearchResponse> results = hotelService.searchHotels(request);
        return ResponseEntity.ok(ApiResponse.success("Hotels found", results));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel details by ID")
    public ResponseEntity<ApiResponse<HotelSearchResponse>> getHotel(@PathVariable Long id) {
        HotelSearchResponse hotel = hotelService.getHotelById(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel found", hotel));
    }

    @GetMapping("/{id}/rooms")
    @Operation(summary = "Get available rooms for a hotel")
    public ResponseEntity<ApiResponse<List<Room>>> getHotelRooms(@PathVariable Long id) {
        List<Room> rooms = hotelService.getHotelRooms(id);
        return ResponseEntity.ok(ApiResponse.success("Rooms found", rooms));
    }

    @GetMapping("/popular-cities")
    @Operation(summary = "Get popular hotel cities")
    public ResponseEntity<ApiResponse<List<String>>> getPopularCities() {
        List<String> cities = hotelService.getPopularCities();
        return ResponseEntity.ok(ApiResponse.success("Popular cities", cities));
    }
}
