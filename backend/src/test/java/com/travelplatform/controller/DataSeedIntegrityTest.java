package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "seed.data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DataSeedIntegrityTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private AirportRepository airportRepo;
    @Autowired private AirlineRepository airlineRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private SeatRepository seatRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private RoomRepository roomRepo;
    @Autowired private TrainRepository trainRepo;
    @Autowired private TrainSeatRepository trainSeatRepo;
    @Autowired private BusRepository busRepo;
    @Autowired private CabRepository cabRepo;
    @Autowired private HolidayPackageRepository holidayRepo;
    @Autowired private ReviewRepository reviewRepo;
    @Autowired private ReviewPhotoRepository photoRepo;
    @Autowired private ReviewReplyRepository replyRepo;
    @Autowired private ReviewVoteRepository voteRepo;
    @Autowired private ReviewReportRepository reportRepo;
    @Autowired private ReviewModerationActionRepository actionRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private RefundRepository refundRepo;
    @Autowired private PriceHistoryRepository priceHistoryRepo;
    @Autowired private PriceFreezeRepository priceFreezeRepo;
    @Autowired private FlightStatusRepository flightStatusRepo;
    @Autowired private TrackedFlightRepository trackedFlightRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserTravelPreferenceRepository prefRepo;
    @Autowired private GroupTripRepository groupTripRepo;
    @Autowired private TripExpenseRepository expenseRepo;
    @Autowired private TripSettlementRepository settlementRepo;
    @Autowired private UserRepository userRepo;

    @Test
    @DisplayName("Verify target volume metrics across all domains")
    void testTargetDataVolumes() {
        assertThat(airportRepo.count()).isGreaterThanOrEqualTo(100);
        assertThat(airlineRepo.count()).isGreaterThanOrEqualTo(12);
        assertThat(flightRepo.count()).isGreaterThanOrEqualTo(500);
        assertThat(seatRepo.count()).isGreaterThanOrEqualTo(5000);
        assertThat(hotelRepo.count()).isGreaterThanOrEqualTo(200);
        assertThat(roomRepo.count()).isGreaterThanOrEqualTo(500);
        assertThat(trainRepo.count()).isGreaterThanOrEqualTo(150);
        assertThat(trainSeatRepo.count()).isGreaterThanOrEqualTo(400);
        assertThat(busRepo.count()).isGreaterThanOrEqualTo(200);
        assertThat(cabRepo.count()).isGreaterThanOrEqualTo(80);
        assertThat(holidayRepo.count()).isGreaterThanOrEqualTo(10);
        assertThat(reviewRepo.count()).isGreaterThanOrEqualTo(500);
        assertThat(photoRepo.count()).isGreaterThanOrEqualTo(50);
        assertThat(replyRepo.count()).isGreaterThanOrEqualTo(50);
        assertThat(voteRepo.count()).isGreaterThanOrEqualTo(50);
        assertThat(priceHistoryRepo.count()).isGreaterThanOrEqualTo(100);
        assertThat(priceFreezeRepo.count()).isGreaterThanOrEqualTo(4);
        assertThat(flightStatusRepo.count()).isGreaterThanOrEqualTo(200);
        assertThat(trackedFlightRepo.count()).isGreaterThanOrEqualTo(4);
        assertThat(bookingRepo.count()).isGreaterThanOrEqualTo(5);
        assertThat(paymentRepo.count()).isGreaterThanOrEqualTo(5);
        assertThat(refundRepo.count()).isGreaterThanOrEqualTo(2);
        assertThat(notificationRepo.count()).isGreaterThanOrEqualTo(5);
        assertThat(groupTripRepo.count()).isGreaterThanOrEqualTo(1);
        assertThat(expenseRepo.count()).isGreaterThanOrEqualTo(1);
        assertThat(settlementRepo.count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Verify relational foreign key integrity and absence of orphan records")
    void testRelationalIntegrity() {
        // Flights: origin and destination must be distinct, arrival after departure
        List<Flight> sampleFlights = flightRepo.findAll().stream().limit(100).toList();
        for (Flight f : sampleFlights) {
            assertThat(f.getAirline()).isNotNull();
            assertThat(f.getOrigin()).isNotNull();
            assertThat(f.getDestination()).isNotNull();
            assertThat(f.getOriginCode()).isNotEqualTo(f.getDestinationCode());
            assertThat(f.getArrivalTime()).isAfter(f.getDepartureTime());
            assertThat(f.getEconomyPrice()).isGreaterThan(java.math.BigDecimal.ZERO);
        }

        // Rooms: must reference valid hotels
        List<Room> sampleRooms = roomRepo.findAll().stream().limit(100).toList();
        for (Room r : sampleRooms) {
            assertThat(r.getHotel()).isNotNull();
            assertThat(r.getPricePerNight()).isGreaterThan(java.math.BigDecimal.ZERO);
            assertThat(r.getTotalRooms()).isGreaterThan(0);
            assertThat(r.getAvailableRooms()).isLessThanOrEqualTo(r.getTotalRooms());
        }

        // Bookings & Refunds: refund must not exceed payment amount
        List<Refund> refunds = refundRepo.findAll();
        for (Refund ref : refunds) {
            assertThat(ref.getBooking()).isNotNull();
            assertThat(ref.getRefundAmount()).isLessThanOrEqualTo(ref.getOriginalAmount());
            assertThat(ref.getCancellationReason()).isNotNull();
        }
    }

    @Test
    @DisplayName("Verify Flight Search API returns rich results on key domestic corridors")
    void testFlightSearchDomestic() throws Exception {
        String todayStr = LocalDate.now().toString();

        mockMvc.perform(get("/api/flights/search")
                .param("origin", "BOM")
                .param("destination", "DEL")
                .param("departureDate", todayStr)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.content[0].originCode").value("BOM"))
                .andExpect(jsonPath("$.data.content[0].destinationCode").value("DEL"))
                .andExpect(jsonPath("$.data.content[0].price").isNotEmpty());
    }

    @Test
    @DisplayName("Verify Flight Search API returns rich results on international corridor")
    void testFlightSearchInternational() throws Exception {
        String todayStr = LocalDate.now().toString();

        mockMvc.perform(get("/api/flights/search")
                .param("origin", "DEL")
                .param("destination", "DXB")
                .param("departureDate", todayStr)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Verify Hotel Search API returns rich results for multiple cities")
    void testHotelSearchCities() throws Exception {
        // Mumbai
        mockMvc.perform(get("/api/hotels/search")
                .param("city", "Mumbai")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(5)));

        // Goa
        mockMvc.perform(get("/api/hotels/search")
                .param("city", "Goa")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(5)));

        // Dubai (International)
        mockMvc.perform(get("/api/hotels/search")
                .param("city", "Dubai")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(3)));
    }

    @Test
    @DisplayName("Verify Train, Bus, Cab, and Holiday search APIs work seamlessly")
    void testGroundTransportAndHolidays() throws Exception {
        // Trains
        mockMvc.perform(get("/api/trains/search")
                .param("origin", "Delhi")
                .param("destination", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        // Buses
        mockMvc.perform(get("/api/buses/search")
                .param("origin", "Mumbai")
                .param("destination", "Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        // Cabs
        mockMvc.perform(get("/api/cabs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(10)));

        // Holiday Packages
        mockMvc.perform(get("/api/holidays/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(5)));
    }

    @Test
    @DisplayName("Verify Reviews and Moderation APIs return seeded reviews")
    void testReviewsAndRatings() throws Exception {
        Hotel sampleHotel = hotelRepo.findAll().get(0);

        mockMvc.perform(get("/api/reviews/hotel/" + sampleHotel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Autowired
    private org.springframework.boot.CommandLineRunner seedData;

    @Test
    @DisplayName("Verify seed process is 100% idempotent when re-run")
    void testSeedIdempotency() throws Exception {
        long airportsBefore = airportRepo.count();
        long airlinesBefore = airlineRepo.count();
        long flightsBefore = flightRepo.count();
        long seatsBefore = seatRepo.count();
        long hotelsBefore = hotelRepo.count();
        long roomsBefore = roomRepo.count();
        long holidaysBefore = holidayRepo.count();
        long trainsBefore = trainRepo.count();
        long busesBefore = busRepo.count();
        long cabsBefore = cabRepo.count();
        long reviewsBefore = reviewRepo.count();
        long bookingsBefore = bookingRepo.count();

        // Run seeder second time
        seedData.run();

        assertThat(airportRepo.count()).isEqualTo(airportsBefore);
        assertThat(airlineRepo.count()).isEqualTo(airlinesBefore);
        assertThat(flightRepo.count()).isEqualTo(flightsBefore);
        assertThat(seatRepo.count()).isEqualTo(seatsBefore);
        assertThat(hotelRepo.count()).isEqualTo(hotelsBefore);
        assertThat(roomRepo.count()).isEqualTo(roomsBefore);
        assertThat(holidaysBefore).isGreaterThan(0);
        assertThat(holidayRepo.count()).isEqualTo(holidaysBefore);
        assertThat(trainRepo.count()).isEqualTo(trainsBefore);
        assertThat(busRepo.count()).isEqualTo(busesBefore);
        assertThat(cabRepo.count()).isEqualTo(cabsBefore);
        assertThat(reviewRepo.count()).isEqualTo(reviewsBefore);
        assertThat(bookingRepo.count()).isEqualTo(bookingsBefore);
    }
}
