package com.travelplatform.service;

import com.travelplatform.dto.flight.FlightSearchRequest;
import com.travelplatform.dto.flight.FlightSearchResponse;
import com.travelplatform.dto.flight.MultiCitySearchRequest;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.FareOption;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.FareOptionRepository;
import com.travelplatform.repository.AirlineRepository;
import com.travelplatform.repository.AirportRepository;
import com.travelplatform.entity.Airport;
import com.travelplatform.entity.Airline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final FareOptionRepository fareOptionRepository;

    public FlightService(FlightRepository flightRepository,
                         AirlineRepository airlineRepository,
                         AirportRepository airportRepository,
                         FareOptionRepository fareOptionRepository) {
        this.flightRepository = flightRepository;
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
        this.fareOptionRepository = fareOptionRepository;
    }

    @Transactional(readOnly = true)
    public Page<FlightSearchResponse> searchFlights(FlightSearchRequest request) {
        // Apply departure time range filter as additional price filter
        BigDecimal minPrice = request.getMinPrice();
        BigDecimal maxPrice = request.getMaxPrice();

        Sort sort = switch (request.getSortBy()) {
            case "price" -> Sort.by("economyPrice");
            case "duration" -> Sort.by("durationMinutes");
            default -> Sort.by("departureTime");
        };
        if ("desc".equalsIgnoreCase(request.getSortOrder())) {
            sort = sort.descending();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Flight> flights = flightRepository.searchFlights(
                request.getOrigin(),
                request.getDestination(),
                request.getDepartureDate(),
                request.getAirlineCode(),
                request.getMaxStops(),
                minPrice,
                maxPrice,
                request.getCabinClass(),
                pageable
        );

        // Get filter metadata
        List<String> airlineCodes = flightRepository.findAirlineCodes(
                request.getOrigin(), request.getDestination(), request.getDepartureDate());

        return flights.map(flight -> mapToResponse(flight, request.getCabinClass()));
    }

    @Transactional(readOnly = true)
    public FlightSearchResponse getFlightById(Long id, String cabinClass) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
        return mapToResponse(flight, cabinClass);
    }

    @Transactional(readOnly = true)
    public List<FlightSearchResponse> getDirectFlights(String origin, String destination,
                                                        java.time.LocalDate date, String cabinClass) {
        List<Flight> flights = flightRepository.findDirectFlights(origin, destination, date);
        return flights.stream()
                .map(f -> mapToResponse(f, cabinClass))
                .collect(Collectors.toList());
    }

    /**
     * Multi-city search: search each leg independently and return results grouped by leg index.
     */
    @Transactional(readOnly = true)
    public Map<Integer, Page<FlightSearchResponse>> searchMultiCity(MultiCitySearchRequest request) {
        Map<Integer, Page<FlightSearchResponse>> results = new LinkedHashMap<>();
        if (request.getLegs() == null || request.getLegs().isEmpty()) return results;

        for (int i = 0; i < request.getLegs().size(); i++) {
            MultiCitySearchRequest.CityLeg leg = request.getLegs().get(i);
            FlightSearchRequest legRequest = new FlightSearchRequest();
            legRequest.setOrigin(leg.getOrigin());
            legRequest.setDestination(leg.getDestination());
            legRequest.setDepartureDate(leg.getDate());
            legRequest.setCabinClass(request.getCabinClass());
            legRequest.setAirlineCode(request.getAirlineCode());
            legRequest.setMaxStops(request.getMaxStops());
            legRequest.setMinPrice(request.getMinPrice());
            legRequest.setMaxPrice(request.getMaxPrice());
            legRequest.setSortBy(request.getSortBy());
            legRequest.setSortOrder(request.getSortOrder());
            legRequest.setPage(request.getPage());
            legRequest.setSize(request.getSize());
            legRequest.setPassengers(request.getPassengers());

            results.put(i, searchFlights(legRequest));
        }
        return results;
    }

    /** Get fare options for a specific flight */
    @Transactional(readOnly = true)
    public List<FareOption> getFareOptions(Long flightId) {
        // Ensure flight exists
        flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", flightId));
        return fareOptionRepository.findByFlightIdOrderByPriceMultiplierAsc(flightId);
    }

    private FlightSearchResponse mapToResponse(Flight flight, String cabinClass) {
        FlightSearchResponse response = new FlightSearchResponse();
        response.setId(flight.getId());
        response.setFlightNumber(flight.getFlightNumber());
        response.setAirlineName(flight.getAirline().getName());
        response.setAirlineCode(flight.getAirline().getCode());
        response.setAirlineLogo(flight.getAirline().getLogoUrl());
        response.setOriginCode(flight.getOriginCode());
        response.setOriginCity(flight.getOrigin().getCity());
        response.setDestinationCode(flight.getDestinationCode());
        response.setDestinationCity(flight.getDestination().getCity());
        response.setDepartureTime(flight.getDepartureTime());
        response.setArrivalTime(flight.getArrivalTime());
        response.setDepartureDate(flight.getDepartureDate());
        response.setDurationMinutes(flight.getDurationMinutes());
        response.setStops(flight.getStops());
        response.setStopoverCity(flight.getStopoverCity());
        response.setCabinClass(cabinClass);
        response.setPrice(flight.getPriceForClass(cabinClass));
        response.setAvailableSeats(flight.getAvailableSeatsForClass(cabinClass));
        return response;
    }
}
