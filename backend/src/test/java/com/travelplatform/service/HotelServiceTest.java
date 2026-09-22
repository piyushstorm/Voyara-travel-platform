package com.travelplatform.service;

import com.travelplatform.dto.hotel.HotelSearchRequest;
import com.travelplatform.dto.hotel.HotelSearchResponse;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.Room;
import com.travelplatform.repository.HotelRepository;
import com.travelplatform.repository.RoomRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock private HotelRepository hotelRepo;
    @Mock private RoomRepository roomRepo;

    @InjectMocks
    private HotelService hotelService;

    private Hotel hotel1;

    @BeforeEach
    void setUp() {
        hotel1 = new Hotel();
        hotel1.setName("The Grand Palace");
        hotel1.setDescription("Luxury heritage hotel in Mumbai");
        hotel1.setCity("Mumbai");
        hotel1.setStarRating(5);
        hotel1.setGuestRating(4.5);
        hotel1.setReviewCount(200);
        hotel1.setStartingPrice(BigDecimal.valueOf(8999));
        Set<String> amenities = new HashSet<>();
        amenities.add("POOL");
        amenities.add("SPA");
        amenities.add("GYM");
        hotel1.setAmenities(amenities);
    }

    @Test
    @DisplayName("Hotel search returns mapped responses for matching city")
    void testSearchHotels_returnsMappedResponses() {
        Page<Hotel> page = new PageImpl<>(List.of(hotel1));
        when(hotelRepo.searchHotels(eq("Mumbai"), isNull(), isNull(),
                isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        HotelSearchRequest request = new HotelSearchRequest();
        request.setCity("Mumbai");
        request.setSortBy("startingPrice");
        request.setSortOrder("asc");
        request.setPage(0);
        request.setSize(10);

        Page<HotelSearchResponse> results = hotelService.searchHotels(request);

        assertNotNull(results);
        assertEquals(1, results.getContent().size());

        HotelSearchResponse response = results.getContent().get(0);
        assertEquals("The Grand Palace", response.getName());
        assertEquals("Mumbai", response.getCity());
        assertEquals(5, response.getStarRating());
        assertEquals(4.5, response.getGuestRating(), 0.01);
        assertEquals(200, response.getReviewCount());
        assertEquals(0, BigDecimal.valueOf(8999).compareTo(response.getStartingPrice()));
        assertTrue(response.getAmenities().contains("POOL"));
        assertTrue(response.getAmenities().contains("SPA"));
        assertTrue(response.getAmenities().contains("GYM"));
    }

    @Test
    @DisplayName("getHotelById returns correct hotel details")
    void testGetHotelById() {
        when(hotelRepo.findById(1L)).thenReturn(java.util.Optional.of(hotel1));

        HotelSearchResponse response = hotelService.getHotelById(1L);

        assertNotNull(response);
        assertEquals("The Grand Palace", response.getName());
        assertEquals("Mumbai", response.getCity());
        assertEquals(5, response.getStarRating());
    }

    @Test
    @DisplayName("getHotelRooms returns available rooms for a hotel")
    void testGetHotelRooms() {
        Room room = new Room();
        room.setName("Deluxe Suite");
        room.setRoomType("DELUXE");
        room.setPricePerNight(BigDecimal.valueOf(12000));
        room.setAvailableRooms(5);

        when(roomRepo.findAvailableRooms(1L)).thenReturn(List.of(room));

        List<Room> rooms = hotelService.getHotelRooms(1L);

        assertEquals(1, rooms.size());
        assertEquals("Deluxe Suite", rooms.get(0).getName());
        assertEquals(0, BigDecimal.valueOf(12000).compareTo(rooms.get(0).getPricePerNight()));
    }
}
