package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.dto.flight.FlightSearchRequest;
import com.travelplatform.dto.flight.FlightSearchResponse;
import com.travelplatform.dto.flight.MultiCitySearchRequest;
import com.travelplatform.entity.FareOption;
import com.travelplatform.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@Tag(name = "Flights", description = "Flight search and detail endpoints")
public class FlightController {

    private final FlightService flightService;
    private final com.travelplatform.service.DynamicPricingService dynamicPricingService;

    public FlightController(FlightService flightService, com.travelplatform.service.DynamicPricingService dynamicPricingService) {
        this.flightService = flightService;
        this.dynamicPricingService = dynamicPricingService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search flights with filters")
    public ResponseEntity<ApiResponse<Page<FlightSearchResponse>>> searchFlights(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String departureDate,
            @RequestParam(required = false) String returnDate,
            @RequestParam(defaultValue = "ECONOMY") String cabinClass,
            @RequestParam(required = false) String airlineCode,
            @RequestParam(required = false) Integer maxStops,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (origin == null || origin.trim().isEmpty()) {
            throw new com.travelplatform.exception.BadRequestException("Origin airport or city is required");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new com.travelplatform.exception.BadRequestException("Destination airport or city is required");
        }
        if (origin.trim().equalsIgnoreCase(destination.trim())) {
            throw new com.travelplatform.exception.BadRequestException("Origin and destination airports cannot be the same");
        }

        FlightSearchRequest request = new FlightSearchRequest();
        request.setOrigin(origin.trim().toUpperCase());
        request.setDestination(destination.trim().toUpperCase());
        request.setCabinClass(cabinClass);
        request.setAirlineCode(airlineCode);
        request.setMaxStops(maxStops);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);
        request.setPage(page);
        request.setSize(size);

        if (departureDate != null && !departureDate.trim().isEmpty()) {
            try {
                java.time.LocalDate parsedDate = java.time.LocalDate.parse(departureDate.trim());
                if (parsedDate.isBefore(java.time.LocalDate.now())) {
                    throw new com.travelplatform.exception.BadRequestException("Departure date cannot be in the past");
                }
                request.setDepartureDate(parsedDate);
            } catch (java.time.format.DateTimeParseException e) {
                throw new com.travelplatform.exception.BadRequestException("Invalid departure date format. Please use YYYY-MM-DD");
            }
        } else {
            request.setDepartureDate(java.time.LocalDate.now());
        }

        if (returnDate != null && !returnDate.trim().isEmpty()) {
            try {
                java.time.LocalDate parsedReturnDate = java.time.LocalDate.parse(returnDate.trim());
                if (parsedReturnDate.isBefore(request.getDepartureDate())) {
                    throw new com.travelplatform.exception.BadRequestException("Return date must be on or after departure date");
                }
                request.setReturnDate(parsedReturnDate);
            } catch (java.time.format.DateTimeParseException e) {
                throw new com.travelplatform.exception.BadRequestException("Invalid return date format. Please use YYYY-MM-DD");
            }
        }

        if (minPrice != null && !minPrice.isEmpty()) {
            request.setMinPrice(new java.math.BigDecimal(minPrice));
        }
        if (maxPrice != null && !maxPrice.isEmpty()) {
            request.setMaxPrice(new java.math.BigDecimal(maxPrice));
        }

        Page<FlightSearchResponse> results = flightService.searchFlights(request);
        return ResponseEntity.ok(ApiResponse.success("Flights found", results));
    }

    @PostMapping("/multi-city")
    @Operation(summary = "Search multi-city flights (multiple legs)")
    public ResponseEntity<ApiResponse<Map<Integer, Page<FlightSearchResponse>>>> searchMultiCity(
            @RequestBody MultiCitySearchRequest request) {
        Map<Integer, Page<FlightSearchResponse>> results = flightService.searchMultiCity(request);
        return ResponseEntity.ok(ApiResponse.success("Multi-city flights found", results));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get flight details by ID")
    public ResponseEntity<ApiResponse<FlightSearchResponse>> getFlight(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ECONOMY") String cabinClass) {
        FlightSearchResponse flight = flightService.getFlightById(id, cabinClass);
        return ResponseEntity.ok(ApiResponse.success("Flight found", flight));
    }

    @GetMapping("/{id}/fare-options")
    @Operation(summary = "Get fare options (Saver/Standard/Flex) for a flight")
    public ResponseEntity<ApiResponse<List<FareOption>>> getFareOptions(@PathVariable Long id) {
        List<FareOption> fareOptions = flightService.getFareOptions(id);
        return ResponseEntity.ok(ApiResponse.success("Fare options retrieved", fareOptions));
    }

    @GetMapping("/{id}/pricing-factors")
    @Operation(summary = "Get dynamic pricing factors for transparency")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPricingFactors(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ECONOMY") String cabinClass) {
        Map<String, Object> factors = dynamicPricingService.getPricingFactors(id, cabinClass);
        if (factors.containsKey("error")) {
            return ResponseEntity.badRequest().body(ApiResponse.error(factors.get("error").toString()));
        }
        return ResponseEntity.ok(ApiResponse.success("Pricing factors retrieved", factors));
    }
}
