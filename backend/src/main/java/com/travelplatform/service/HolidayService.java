package com.travelplatform.service;

import com.travelplatform.entity.HolidayPackage;
import com.travelplatform.repository.HolidayPackageRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayPackageRepository repo;

    public HolidayService(HolidayPackageRepository repo) {
        this.repo = repo;
    }

    public List<HolidayPackage> search(String destination, String tripType, BigDecimal minPrice, BigDecimal maxPrice,
                                        String hotelCategory, Double minRating, String mealPlan, String transportType,
                                        String sortBy, String sortOrder) {
        List<HolidayPackage> results = repo.search(destination, tripType, minPrice, maxPrice, hotelCategory, minRating, mealPlan, transportType);
        // Sort in memory for flexibility
        java.util.Comparator<HolidayPackage> comp = switch (sortBy != null ? sortBy : "featured") {
            case "price" -> java.util.Comparator.comparing(HolidayPackage::getPricePerPerson);
            case "rating" -> java.util.Comparator.comparing(HolidayPackage::getRating).reversed();
            case "duration" -> java.util.Comparator.comparing(HolidayPackage::getDurationNights);
            default -> java.util.Comparator.comparing(HolidayPackage::getFeatured).reversed()
                .thenComparing(java.util.Comparator.comparing(HolidayPackage::getRating).reversed());
        };
        if ("asc".equalsIgnoreCase(sortOrder)) { /* already asc for price/duration */ } else if (!"featured".equals(sortBy)) { comp = comp.reversed(); }
        results.sort(comp);
        return results;
    }

    public List<String> getDestinations() {
        return repo.findByActiveTrue().stream()
            .map(HolidayPackage::getDestination)
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
    }

    public HolidayPackage getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Holiday package not found: " + id));
    }

    public List<HolidayPackage> getFeatured() {
        return repo.findByFeaturedTrueAndActiveTrue();
    }
}
