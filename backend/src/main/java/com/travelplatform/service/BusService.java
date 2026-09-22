package com.travelplatform.service;

import com.travelplatform.entity.Bus;
import com.travelplatform.repository.BusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusService {

    private final BusRepository repo;

    public BusService(BusRepository repo) {
        this.repo = repo;
    }

    public List<Bus> search(String origin, String destination, String sortBy, String busType) {
        List<Bus> results = repo.findAllActive();
        if (origin != null && !origin.isEmpty()) {
            results = results.stream()
                .filter(b -> origin.equalsIgnoreCase(b.getOriginCity()) || origin.equalsIgnoreCase(b.getOriginName()))
                .toList();
        }
        if (destination != null && !destination.isEmpty()) {
            results = results.stream()
                .filter(b -> destination.equalsIgnoreCase(b.getDestinationCity()) || destination.equalsIgnoreCase(b.getDestinationName()))
                .toList();
        }
        if (busType != null && !busType.isEmpty()) {
            results = results.stream()
                .filter(b -> busType.equalsIgnoreCase(b.getBusType()))
                .toList();
        }
        if (sortBy != null) {
            switch (sortBy) {
                case "departure" -> results.sort(java.util.Comparator.comparing(Bus::getDepartureTime));
                case "price" -> results.sort(java.util.Comparator.comparing(Bus::getBasePrice));
                case "duration" -> results.sort(java.util.Comparator.comparing(Bus::getDurationMinutes));
                case "rating" -> results.sort(java.util.Comparator.comparing(Bus::getRating).reversed());
            }
        }
        return results;
    }

    public Bus getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Bus not found: " + id));
    }
}
