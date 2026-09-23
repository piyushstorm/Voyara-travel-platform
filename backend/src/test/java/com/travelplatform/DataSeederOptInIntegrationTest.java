package com.travelplatform;

import com.travelplatform.repository.AirportRepository;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.HotelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class DataSeederOptInIntegrationTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "seed.data.enabled=false"
    })
    @DirtiesContext
    class WhenSeedingDisabledTest {
        @Autowired private AirportRepository airportRepo;
        @Autowired private FlightRepository flightRepo;
        @Autowired private HotelRepository hotelRepo;

        @Test
        @DisplayName("When seed.data.enabled=false, catalog remains empty")
        void testNoSeedingWhenDisabled() {
            assertThat(airportRepo.count()).isEqualTo(0);
            assertThat(flightRepo.count()).isEqualTo(0);
            assertThat(hotelRepo.count()).isEqualTo(0);
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles({"test", "production"})
    @TestPropertySource(properties = {
            "seed.data.enabled=true"
    })
    @DirtiesContext
    class WhenProductionProfileAndSeedingExplicitlyEnabledTest {
        @Autowired private AirportRepository airportRepo;
        @Autowired private FlightRepository flightRepo;
        @Autowired private HotelRepository hotelRepo;

        @Test
        @DisplayName("When active profile is production and seed.data.enabled=true, seeding runs successfully")
        void testSeedingRunsInProductionWhenExplicitlyEnabled() {
            assertThat(airportRepo.count()).isGreaterThanOrEqualTo(100);
            assertThat(flightRepo.count()).isGreaterThanOrEqualTo(500);
            assertThat(hotelRepo.count()).isGreaterThanOrEqualTo(200);
        }
    }
}
