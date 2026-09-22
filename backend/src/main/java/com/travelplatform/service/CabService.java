package com.travelplatform.service;

import com.travelplatform.entity.Cab;
import com.travelplatform.repository.CabRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CabService {

    private final CabRepository repo;

    public CabService(CabRepository repo) {
        this.repo = repo;
    }

    public List<Cab> getAll(String vehicleType, String sortBy) {
        List<Cab> results = repo.findByActiveTrue();
        if (vehicleType != null && !vehicleType.isEmpty()) {
            results = results.stream()
                .filter(c -> vehicleType.equalsIgnoreCase(c.getVehicleType()))
                .toList();
        }
        if (sortBy != null) {
            switch (sortBy) {
                case "price" -> results.sort(java.util.Comparator.comparing(Cab::getBaseFare));
                case "rating" -> results.sort(java.util.Comparator.comparing(Cab::getRating).reversed());
                case "capacity" -> results.sort(java.util.Comparator.comparing(Cab::getCapacity).reversed());
            }
        }
        return results;
    }

    public Cab getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Cab not found: " + id));
    }
}
