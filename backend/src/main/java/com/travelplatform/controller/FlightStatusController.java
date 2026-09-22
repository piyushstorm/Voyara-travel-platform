package com.travelplatform.controller;

import com.travelplatform.dto.flight.FlightStatusResponse;
import com.travelplatform.entity.FlightStatus;
import com.travelplatform.entity.FlightStatusHistory;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.FlightStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flight-status")
public class FlightStatusController {

    private final FlightStatusService statusService;
    private final UserRepository userRepository;

    public FlightStatusController(FlightStatusService statusService, UserRepository userRepository) {
        this.statusService = statusService;
        this.userRepository = userRepository;
    }

    /** Get live status for a single flight */
    @GetMapping("/{flightId}")
    public ResponseEntity<FlightStatusResponse> getFlightStatus(@PathVariable Long flightId) {
        FlightStatus status = statusService.getFlightStatus(flightId);
        return ResponseEntity.ok(FlightStatusResponse.from(status));
    }

    /** Search flights with their live statuses */
    @GetMapping("/search")
    public ResponseEntity<List<FlightStatusResponse>> searchFlightStatuses(@RequestParam(required = false, defaultValue = "") String query) {
        List<FlightStatus> statuses = statusService.searchFlightStatuses(query);
        return ResponseEntity.ok(statuses.stream().map(FlightStatusResponse::from).collect(Collectors.toList()));
    }

    /** Get live status for multiple tracked flights */
    @GetMapping("/tracked")
    public ResponseEntity<List<FlightStatusResponse>> getTrackedFlights(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<FlightStatus> statuses = statusService.getUserTrackedFlights(user.getId());
        return ResponseEntity.ok(statuses.stream().map(FlightStatusResponse::from).collect(Collectors.toList()));
    }

    /** Track a flight for the current user */
    @PostMapping("/{flightId}/track")
    public ResponseEntity<Map<String, String>> trackFlight(
            @PathVariable Long flightId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        statusService.trackFlight(user.getId(), flightId);
        return ResponseEntity.ok(Map.of("message", "Flight tracked successfully"));
    }

    /** Untrack a flight for the current user */
    @DeleteMapping("/{flightId}/untrack")
    public ResponseEntity<Map<String, String>> untrackFlight(
            @PathVariable Long flightId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        statusService.untrackFlight(user.getId(), flightId);
        return ResponseEntity.ok(Map.of("message", "Flight untracked successfully"));
    }

    /** Get status change history for a flight */
    @GetMapping("/{flightId}/history")
    public ResponseEntity<List<FlightStatusHistory>> getFlightHistory(@PathVariable Long flightId) {
        return ResponseEntity.ok(statusService.getFlightStatusHistory(flightId));
    }

    /** Simulate transition for controlled scenario testing */
    @PostMapping("/{flightId}/simulate")
    public ResponseEntity<FlightStatusResponse> simulateTransition(
            @PathVariable Long flightId,
            @RequestBody(required = false) Map<String, String> body) {
        String scenario = (body != null && body.containsKey("scenario")) ? body.get("scenario") : "ON_TIME";
        FlightStatus status = statusService.simulateTransition(flightId, scenario);
        return ResponseEntity.ok(FlightStatusResponse.from(status));
    }
}
