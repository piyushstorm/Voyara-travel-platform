package com.travelplatform.controller;

import com.travelplatform.entity.Train;
import com.travelplatform.entity.TrainSeat;
import com.travelplatform.service.TrainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainService service;

    public TrainController(TrainService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String classCode) {
        List<Train> results = service.search(origin, destination, sortBy, classCode);
        return ResponseEntity.ok(Map.of("success", true, "data", results, "total", results.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Train train = service.getById(id);
        return ResponseEntity.ok(Map.of("success", true, "data", train));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<Map<String, Object>> getSeats(@PathVariable Long id) {
        List<TrainSeat> seats = service.getSeats(id);
        return ResponseEntity.ok(Map.of("success", true, "data", seats));
    }
}
