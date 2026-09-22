package com.travelplatform.controller;

import com.travelplatform.entity.Cab;
import com.travelplatform.service.CabService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cabs")
public class CabController {

    private final CabService service;

    public CabController(CabService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String sortBy) {
        List<Cab> cabs = service.getAll(vehicleType, sortBy);
        return ResponseEntity.ok(Map.of("success", true, "data", cabs, "total", cabs.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Cab cab = service.getById(id);
        return ResponseEntity.ok(Map.of("success", true, "data", cab));
    }
}
