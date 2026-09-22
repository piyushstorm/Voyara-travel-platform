package com.travelplatform.controller;

import com.travelplatform.entity.Bus;
import com.travelplatform.service.BusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buses")
public class BusController {

    private final BusService service;

    public BusController(BusService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String busType) {
        List<Bus> results = service.search(origin, destination, sortBy, busType);
        return ResponseEntity.ok(Map.of("success", true, "data", results, "total", results.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Bus bus = service.getById(id);
        return ResponseEntity.ok(Map.of("success", true, "data", bus));
    }
}
