package com.travelplatform.service;

import com.travelplatform.entity.Train;
import com.travelplatform.entity.TrainSeat;
import com.travelplatform.repository.TrainRepository;
import com.travelplatform.repository.TrainSeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainService {

    private final TrainRepository trainRepo;
    private final TrainSeatRepository seatRepo;

    public TrainService(TrainRepository trainRepo, TrainSeatRepository seatRepo) {
        this.trainRepo = trainRepo;
        this.seatRepo = seatRepo;
    }

    public List<Train> search(String origin, String destination, String sortBy, String classCode) {
        List<Train> results = trainRepo.findAllActive();
        if (origin != null && !origin.isEmpty()) {
            results = results.stream()
                .filter(t -> origin.equalsIgnoreCase(t.getOriginCity()) || origin.equalsIgnoreCase(t.getOriginName()) || origin.equalsIgnoreCase(t.getOriginCode()))
                .toList();
        }
        if (destination != null && !destination.isEmpty()) {
            results = results.stream()
                .filter(t -> destination.equalsIgnoreCase(t.getDestinationCity()) || destination.equalsIgnoreCase(t.getDestinationName()) || destination.equalsIgnoreCase(t.getDestinationCode()))
                .toList();
        }
        if (classCode != null && !classCode.isEmpty()) {
            results = results.stream()
                .filter(t -> t.getClasses() != null && t.getClasses().contains(classCode))
                .toList();
        }
        if (sortBy != null) {
            switch (sortBy) {
                case "departure" -> results.sort(java.util.Comparator.comparing(Train::getDepartureTime));
                case "duration" -> results.sort(java.util.Comparator.comparing(Train::getDurationMinutes));
                case "arrival" -> results.sort(java.util.Comparator.comparing(Train::getArrivalTime));
                case "name" -> results.sort(java.util.Comparator.comparing(Train::getTrainName));
            }
        }
        return results;
    }

    public Train getById(Long id) {
        return trainRepo.findById(id).orElseThrow(() -> new RuntimeException("Train not found: " + id));
    }

    public List<TrainSeat> getSeats(Long trainId) {
        return seatRepo.findByTrainId(trainId);
    }
}
