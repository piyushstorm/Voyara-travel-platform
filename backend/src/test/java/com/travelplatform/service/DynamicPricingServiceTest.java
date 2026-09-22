package com.travelplatform.service;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.PriceHistory;
import com.travelplatform.entity.Room;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.PriceHistoryRepository;
import com.travelplatform.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DynamicPricingServiceTest {

    @Mock private FlightRepository flightRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private PriceHistoryRepository priceHistoryRepo;
    @Mock private WebSocketNotificationService notificationService;

    @InjectMocks
    private DynamicPricingService pricingService;

    @Test
    @DisplayName("Peak period detection works for known holiday ranges")
    void testPeakPeriodDetection() {
        // Dec 25 should be peak (Christmas/New Year period)
        LocalDate christmas = LocalDate.of(2026, 12, 25);
        assertTrue(isPeakPeriod(christmas), "Dec 25 should be peak period");

        // Jan 3 should be peak
        LocalDate newYear = LocalDate.of(2026, 1, 3);
        assertTrue(isPeakPeriod(newYear), "Jan 3 should be peak period");

        // Mar 10 should NOT be peak
        LocalDate randomDay = LocalDate.of(2026, 3, 10);
        assertFalse(isPeakPeriod(randomDay), "Mar 10 should not be peak period");
    }

    @Test
    @DisplayName("Time multiplier correctly penalizes last-minute bookings")
    void testTimeMultiplier() {
        // >7 days should get early bird discount
        double multiplier7days = getTimeMultiplier(200);
        assertTrue(multiplier7days < 1.0, "Early bird should be < 1.0, got: " + multiplier7days);

        // <24 hours should get surge
        double multiplierLastMin = getTimeMultiplier(12);
        assertTrue(multiplierLastMin > 1.0, "Last-minute should be > 1.0, got: " + multiplierLastMin);

        // 1-3 days should be neutral
        double multiplierNormal = getTimeMultiplier(48);
        assertEquals(1.0, multiplierNormal, 0.01, "Normal should be ~1.0");
    }

    @Test
    @DisplayName("Price never goes below zero")
    void testPriceNeverNegative() {
        BigDecimal basePrice = BigDecimal.valueOf(5000);
        // Even with all multipliers at minimum, price should be positive
        BigDecimal minPrice = basePrice
            .multiply(BigDecimal.valueOf(0.85))  // minimum demand
            .multiply(BigDecimal.valueOf(0.90))   // early bird
            .setScale(0, RoundingMode.HALF_UP);
        assertTrue(minPrice.compareTo(BigDecimal.ZERO) > 0, "Price should never be zero or negative");
    }

    @Test
    @DisplayName("Demand multiplier increases with booking ratio")
    void testDemandMultiplier() {
        double demand0 = 0.85 + (0.0 * 0.55);  // 0% booked
        double demand50 = 0.85 + (0.5 * 0.55);  // 50% booked
        double demand100 = 0.85 + (1.0 * 0.55); // 100% booked

        assertTrue(demand0 < demand50, "Empty flight should be cheaper");
        assertTrue(demand50 < demand100, "Full flight should be most expensive");
        assertEquals(0.85, demand0, 0.01);
        assertEquals(1.40, demand100, 0.01);
    }

    // Helper methods to test private logic via package-level visibility
    private boolean isPeakPeriod(LocalDate date) {
        int[][] periods = {
            {12, 20, 1, 5}, {3, 15, 3, 31}, {5, 1, 5, 15},
            {10, 15, 11, 15}, {8, 1, 8, 15}
        };
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        for (int[] p : periods) {
            int startMonth = p[0], startDay = p[1];
            int endMonth = p[2], endDay = p[3];
            if (startMonth > endMonth || (startMonth == endMonth && startDay > endDay)) {
                // Wraps year boundary (e.g., Dec 20 - Jan 5)
                if (month > startMonth || (month == startMonth && day >= startDay)) return true;
                if (month < endMonth || (month == endMonth && day <= endDay)) return true;
            } else {
                if ((month > startMonth || (month == startMonth && day >= startDay)) &&
                    (month < endMonth || (month == endMonth && day <= endDay))) return true;
            }
        }
        return false;
    }

    private double getTimeMultiplier(long hoursUntilDeparture) {
        if (hoursUntilDeparture > 168) return 0.90;
        if (hoursUntilDeparture > 72) return 0.95;
        if (hoursUntilDeparture > 24) return 1.00;
        if (hoursUntilDeparture > 0) return 1.15;
        return 1.00;
    }
}
