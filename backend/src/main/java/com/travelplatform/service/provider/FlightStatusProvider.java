package com.travelplatform.service.provider;

import com.travelplatform.entity.FlightStatus;
import java.util.List;
import java.util.Optional;

public interface FlightStatusProvider {
    /**
     * Retrieve the current live status for a specific flight ID.
     */
    Optional<FlightStatus> getLiveStatus(Long flightId);

    /**
     * Fetch live status updates for a batch of flights.
     */
    List<FlightStatus> getBatchLiveStatus(List<Long> flightIds);
    
    /**
     * Update/simulate flight statuses.
     */
    void synchronizeStatuses();

    /**
     * Manually transition or simulate a flight into a specific scenario (e.g. DELAYED, BOARDING, DEPARTED, LANDED).
     */
    FlightStatus simulateFlightTransition(Long flightId, String targetScenario);
}
