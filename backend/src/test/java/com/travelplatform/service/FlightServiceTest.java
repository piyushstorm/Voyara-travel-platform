package com.travelplatform.service;

import com.travelplatform.dto.flight.FlightSearchRequest;
import com.travelplatform.dto.flight.FlightSearchResponse;
import com.travelplatform.entity.Airline;
import com.travelplatform.entity.Airport;
import com.travelplatform.entity.Flight;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.AirlineRepository;
import com.travelplatform.repository.AirportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock private FlightRepository flightRepo;
    @Mock private AirlineRepository airlineRepo;
    @Mock private AirportRepository airportRepo;

    @InjectMocks
    private FlightService flightService;

    private Airline indiGo;
    private Airport delhi;
    private Airport mumbai;
    private Flight flight1;

    @BeforeEach
    void setUp() {
        indiGo = new Airline("6E", "IndiGo", null, 4.2);
        delhi = new Airport("DEL", "Indira Gandhi Intl", "Delhi", "India", 28.5562, 77.1000);
        mumbai = new Airport("BOM", "Chhatrapati Shivaji Intl", "Mumbai", "India", 19.0896, 72.8656);

        flight1 = new Flight();
        flight1.setFlightNumber("6E-103");
        flight1.setAirline(indiGo);
        flight1.setOrigin(delhi);
        flight1.setDestination(mumbai);
        flight1.setOriginCode("DEL");
        flight1.setDestinationCode("BOM");
        flight1.setDepartureTime(LocalDateTime.now().plusHours(3));
        flight1.setArrivalTime(LocalDateTime.now().plusHours(5));
        flight1.setDepartureDate(LocalDate.now());
        flight1.setDurationMinutes(120);
        flight1.setStops(0);
        flight1.setEconomyPrice(BigDecimal.valueOf(4500));
        flight1.setPremiumEconomyPrice(BigDecimal.valueOf(7500));
        flight1.setBusinessPrice(BigDecimal.valueOf(15000));
        flight1.setFirstClassPrice(BigDecimal.valueOf(30000));
        flight1.setTotalSeatsEconomy(150);
        flight1.setBookedSeatsEconomy(30);
    }

    @Test
    @DisplayName("Flight search returns mapped responses for matching origin/destination")
    void testSearchFlights_returnsMappedResponses() {
        Page<Flight> page = new PageImpl<>(List.of(flight1));
        when(flightRepo.searchFlights(eq("DEL"), eq("BOM"), any(), isNull(), isNull(),
                isNull(), isNull(), eq("ECONOMY"), any(PageRequest.class)))
                .thenReturn(page);
        when(flightRepo.findAirlineCodes(eq("DEL"), eq("BOM"), any()))
                .thenReturn(List.of("6E"));

        FlightSearchRequest request = new FlightSearchRequest();
        request.setOrigin("DEL");
        request.setDestination("BOM");
        request.setDepartureDate(LocalDate.now());
        request.setCabinClass("ECONOMY");
        request.setSortBy("departureTime");
        request.setSortOrder("asc");
        request.setPage(0);
        request.setSize(10);

        Page<FlightSearchResponse> results = flightService.searchFlights(request);

        assertNotNull(results);
        assertEquals(1, results.getContent().size());

        FlightSearchResponse response = results.getContent().get(0);
        assertEquals("6E-103", response.getFlightNumber());
        assertEquals("IndiGo", response.getAirlineName());
        assertEquals("6E", response.getAirlineCode());
        assertEquals("DEL", response.getOriginCode());
        assertEquals("BOM", response.getDestinationCode());
        assertEquals("Delhi", response.getOriginCity());
        assertEquals("Mumbai", response.getDestinationCity());
        assertEquals(0, response.getStops());
        assertEquals(120, response.getDurationMinutes());
        assertEquals("ECONOMY", response.getCabinClass());
        assertEquals(0, BigDecimal.valueOf(4500).compareTo(response.getPrice()));
        assertEquals(120, response.getAvailableSeats());
    }

    @Test
    @DisplayName("Flight search with BUSINESS cabin class returns correct price")
    void testSearchFlights_businessCabinClass() {
        Page<Flight> page = new PageImpl<>(List.of(flight1));
        when(flightRepo.searchFlights(anyString(), anyString(), any(), isNull(), isNull(),
                isNull(), isNull(), eq("BUSINESS"), any(PageRequest.class)))
                .thenReturn(page);
        when(flightRepo.findAirlineCodes(anyString(), anyString(), any()))
                .thenReturn(List.of("6E"));

        FlightSearchRequest request = new FlightSearchRequest();
        request.setOrigin("DEL");
        request.setDestination("BOM");
        request.setDepartureDate(LocalDate.now());
        request.setCabinClass("BUSINESS");
        request.setSortBy("departureTime");
        request.setSortOrder("asc");
        request.setPage(0);
        request.setSize(10);

        Page<FlightSearchResponse> results = flightService.searchFlights(request);

        assertEquals("BUSINESS", results.getContent().get(0).getCabinClass());
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(results.getContent().get(0).getPrice()));
    }

    @Test
    @DisplayName("getFlightById returns correct flight details")
    void testGetFlightById() {
        when(flightRepo.findById(1L)).thenReturn(java.util.Optional.of(flight1));

        FlightSearchResponse response = flightService.getFlightById(1L, "ECONOMY");

        assertNotNull(response);
        assertEquals("6E-103", response.getFlightNumber());
        assertEquals("IndiGo", response.getAirlineName());
        assertEquals(0, BigDecimal.valueOf(4500).compareTo(response.getPrice()));
        assertEquals("Delhi", response.getOriginCity());
        assertEquals("Mumbai", response.getDestinationCity());
    }

    @Test
    @DisplayName("getFlightById with PREMIUM_ECONOMY returns correct price")
    void testGetFlightById_premiumEconomy() {
        when(flightRepo.findById(1L)).thenReturn(java.util.Optional.of(flight1));

        FlightSearchResponse response = flightService.getFlightById(1L, "PREMIUM_ECONOMY");

        assertNotNull(response);
        assertEquals("PREMIUM_ECONOMY", response.getCabinClass());
        assertEquals(0, BigDecimal.valueOf(7500).compareTo(response.getPrice()));
    }
}
