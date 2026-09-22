package com.travelplatform.controller;

import com.travelplatform.entity.HolidayPackage;
import com.travelplatform.service.HolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService service;

    public HolidayController(HolidayService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String tripType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String hotelCategory,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String mealPlan,
            @RequestParam(required = false) String transportType,
            @RequestParam(defaultValue = "featured") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        List<HolidayPackage> results = service.search(destination, tripType, minPrice, maxPrice, hotelCategory, minRating, mealPlan, transportType, sortBy, sortOrder);
        return ResponseEntity.ok(Map.of("success", true, "data", results, "total", results.size()));
    }

    @GetMapping("/destinations")
    public ResponseEntity<Map<String, Object>> getDestinations() {
        List<String> destinations = service.getDestinations();
        return ResponseEntity.ok(Map.of("success", true, "data", destinations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        HolidayPackage pkg = service.getById(id);
        return ResponseEntity.ok(Map.of("success", true, "data", pkg));
    }

    @GetMapping("/featured")
    public ResponseEntity<Map<String, Object>> getFeatured() {
        List<HolidayPackage> featured = service.getFeatured();
        return ResponseEntity.ok(Map.of("success", true, "data", featured));
    }
}
