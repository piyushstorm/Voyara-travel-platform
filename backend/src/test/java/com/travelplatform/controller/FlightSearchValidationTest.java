package com.travelplatform.controller;

import com.travelplatform.entity.Airline;
import com.travelplatform.entity.Airport;
import com.travelplatform.entity.Flight;
import com.travelplatform.repository.AirlineRepository;
import com.travelplatform.repository.AirportRepository;
import com.travelplatform.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FlightSearchValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AirportRepository airportRepo;

    @Autowired
    private AirlineRepository airlineRepo;

    @Autowired
    private FlightRepository flightRepo;

    private Airport bom;
    private Airport del;
    private Airport blr;
    private Airport goi;
    private Airport dxb;
    private Airport hyd;
    private Airline ai;

    @BeforeEach
    void setUp() {
        bom = airportRepo.findByCode("BOM").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("BOM");
            a.setName("Chhatrapati Shivaji Maharaj International Airport");
            a.setCity("Mumbai");
            a.setCountry("India");
            return airportRepo.save(a);
        });

        del = airportRepo.findByCode("DEL").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("DEL");
            a.setName("Indira Gandhi International Airport");
            a.setCity("Delhi");
            a.setCountry("India");
            return airportRepo.save(a);
        });

        blr = airportRepo.findByCode("BLR").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("BLR");
            a.setName("Kempegowda International Airport");
            a.setCity("Bangalore");
            a.setCountry("India");
            return airportRepo.save(a);
        });

        goi = airportRepo.findByCode("GOI").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("GOI");
            a.setName("Dabolim Airport");
            a.setCity("Goa");
            a.setCountry("India");
            return airportRepo.save(a);
        });

        dxb = airportRepo.findByCode("DXB").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("DXB");
            a.setName("Dubai International Airport");
            a.setCity("Dubai");
            a.setCountry("United Arab Emirates");
            return airportRepo.save(a);
        });

        hyd = airportRepo.findByCode("HYD").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("HYD");
            a.setName("Rajiv Gandhi International Airport");
            a.setCity("Hyderabad");
            a.setCountry("India");
            return airportRepo.save(a);
        });

        ai = airlineRepo.findByCode("AI").orElseGet(() -> {
            Airline a = new Airline();
            a.setCode("AI");
            a.setName("Air India");
            return airlineRepo.save(a);
        });

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Seed sample flights for corridor tests idempotently
        ensureFlight("AI-101", bom, del, today, 120, new BigDecimal("3500.00"));
        ensureFlight("AI-102", bom, blr, today, 100, new BigDecimal("2800.00"));
        ensureFlight("AI-103", del, goi, today, 150, new BigDecimal("4200.00"));
        ensureFlight("AI-104", bom, dxb, today, 210, new BigDecimal("14500.00"));

        // Popular routes on tomorrow's date
        ensureFlight("AI-201", del, bom, tomorrow, 130, new BigDecimal("2899.00"));
        ensureFlight("AI-202", bom, blr, tomorrow, 105, new BigDecimal("1999.00"));
        ensureFlight("AI-203", del, blr, tomorrow, 160, new BigDecimal("3499.00"));
        ensureFlight("AI-204", del, goi, tomorrow, 150, new BigDecimal("2499.00"));
        ensureFlight("AI-205", bom, goi, tomorrow, 75, new BigDecimal("1799.00"));
        ensureFlight("AI-206", del, hyd, tomorrow, 135, new BigDecimal("2699.00"));
    }

    private void ensureFlight(String flightNum, Airport origin, Airport dest, LocalDate date, int duration, BigDecimal price) {
        if (flightRepo.findByFlightNumber(flightNum).isPresent()) {
            return;
        }
        createFlight(flightNum, origin, dest, date, duration, price);
    }

    private void createFlight(String flightNum, Airport origin, Airport dest, LocalDate date, int duration, BigDecimal price) {
        Flight f = new Flight();
        f.setFlightNumber(flightNum);
        f.setOrigin(origin);
        f.setDestination(dest);
        f.setOriginCode(origin.getCode());
        f.setDestinationCode(dest.getCode());
        f.setAirline(ai);
        f.setDepartureDate(date);
        f.setDepartureTime(LocalDateTime.of(date, java.time.LocalTime.of(10, 0)));
        f.setArrivalTime(LocalDateTime.of(date, java.time.LocalTime.of(10, 0)).plusMinutes(duration));
        f.setDurationMinutes(duration);
        f.setStops(0);
        f.setEconomyPrice(price);
        f.setEconomyBasePrice(price);
        f.setPremiumEconomyPrice(price.multiply(new BigDecimal("1.4")));
        f.setPremiumEconomyBasePrice(price.multiply(new BigDecimal("1.4")));
        f.setBusinessPrice(price.multiply(new BigDecimal("2.5")));
        f.setBusinessBasePrice(price.multiply(new BigDecimal("2.5")));
        f.setFirstClassPrice(price.multiply(new BigDecimal("4.0")));
        f.setFirstClassBasePrice(price.multiply(new BigDecimal("4.0")));
        f.setActive(true);
        flightRepo.save(f);
    }

    @Test
    @DisplayName("Valid flight search: Mumbai -> Delhi (BOM -> DEL)")
    void testSearchMumbaiToDelhi() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "BOM")
                        .param("destination", "DEL")
                        .param("departureDate", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("BOM"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("DEL"));
    }

    @Test
    @DisplayName("Valid flight search: Mumbai -> Bangalore (BOM -> BLR)")
    void testSearchMumbaiToBangalore() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "BOM")
                        .param("destination", "BLR")
                        .param("departureDate", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("BLR"));
    }

    @Test
    @DisplayName("Valid flight search: Delhi -> Goa (DEL -> GOI)")
    void testSearchDelhiToGoa() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "GOI")
                        .param("departureDate", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("DEL"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("GOI"));
    }

    @Test
    @DisplayName("Valid flight search: Mumbai -> Dubai (BOM -> DXB)")
    void testSearchMumbaiToDubai() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "BOM")
                        .param("destination", "DXB")
                        .param("departureDate", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("DXB"));
    }

    @Test
    @DisplayName("Validation: Same origin and destination returns 400 Bad Request")
    void testSameOriginAndDestination() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "DEL")
                        .param("departureDate", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("cannot be the same")));
    }

    @Test
    @DisplayName("Validation: Missing origin returns 400 Bad Request")
    void testMissingOrigin() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("destination", "BOM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Origin")));
    }

    @Test
    @DisplayName("Validation: Missing destination returns 400 Bad Request")
    void testMissingDestination() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Destination")));
    }

    @Test
    @DisplayName("Validation: Past departure date returns 400 Bad Request")
    void testPastDepartureDate() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("departureDate", "2020-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("past")));
    }

    @Test
    @DisplayName("Validation: Invalid date format returns 400 Bad Request")
    void testInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("departureDate", "not-a-valid-date")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid departure date format")));
    }

    @Test
    @DisplayName("Validation: Yesterday's departure date returns 400 Bad Request")
    void testYesterdayDepartureDateRejected() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "BOM")
                        .param("destination", "GOI")
                        .param("departureDate", yesterday)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Departure date cannot be in the past")));
    }

    @Test
    @DisplayName("Popular Route: New Delhi -> Mumbai (DEL -> BOM) with valid future date")
    void testPopularRouteNewDelhiToMumbai() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("departureDate", tomorrow)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("DEL"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("BOM"));
    }

    @Test
    @DisplayName("Popular Route: Mumbai -> Bangalore (BOM -> BLR) with valid future date")
    void testPopularRouteMumbaiToBangalore() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "BOM")
                        .param("destination", "BLR")
                        .param("departureDate", tomorrow)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("BOM"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("BLR"));
    }

    @Test
    @DisplayName("Popular Route: New Delhi -> Bangalore (DEL -> BLR) with valid future date")
    void testPopularRouteNewDelhiToBangalore() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BLR")
                        .param("departureDate", tomorrow)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("DEL"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("BLR"));
    }

    @Test
    @DisplayName("Popular Route: New Delhi -> Goa (DEL -> GOI) with valid future date")
    void testPopularRouteNewDelhiToGoa() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "GOI")
                        .param("departureDate", tomorrow)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("DEL"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("GOI"));
    }

    @Test
    @DisplayName("Popular Route: Mumbai -> Goa (BOM -> GOI) with valid future date")
    void testPopularRouteMumbaiToGoa() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "BOM")
                        .param("destination", "GOI")
                        .param("departureDate", tomorrow)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("BOM"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("GOI"));
    }

    @Test
    @DisplayName("Popular Route: New Delhi -> Hyderabad (DEL -> HYD) with valid future date")
    void testPopularRouteNewDelhiToHyderabad() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "HYD")
                        .param("departureDate", tomorrow)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].originCode").value("DEL"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("HYD"));
    }
}
