package com.travelplatform.service;

import com.travelplatform.dto.hotel.HotelSearchRequest;
import com.travelplatform.dto.hotel.HotelSearchResponse;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.Room;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.HotelRepository;
import com.travelplatform.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public Page<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        Sort sort = switch (request.getSortBy()) {
            case "starRating" -> Sort.by("starRating");
            case "guestRating" -> Sort.by("guestRating");
            case "reviewCount" -> Sort.by("reviewCount");
            default -> Sort.by("startingPrice");
        };
        if ("desc".equalsIgnoreCase(request.getSortOrder())) {
            sort = sort.descending();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Hotel> hotels = hotelRepository.searchHotels(
                request.getCity(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinStar(),
                request.getMaxStar(),
                request.getAmenity(),
                pageable
        );

        return hotels.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public HotelSearchResponse getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        return mapToResponse(hotel);
    }

    @Transactional(readOnly = true)
    public List<Room> getHotelRooms(Long hotelId) {
        return roomRepository.findAvailableRooms(hotelId);
    }

    @Transactional(readOnly = true)
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
    }

    @Transactional(readOnly = true)
    public List<String> getPopularCities() {
        return hotelRepository.findPopularCities();
    }

    private HotelSearchResponse mapToResponse(Hotel hotel) {
        HotelSearchResponse response = new HotelSearchResponse();
        response.setId(hotel.getId());
        response.setName(hotel.getName());
        response.setDescription(hotel.getDescription());
        response.setCity(hotel.getCity());
        response.setStarRating(hotel.getStarRating());
        response.setGuestRating(hotel.getGuestRating());
        response.setReviewCount(hotel.getReviewCount());
        response.setStartingPrice(hotel.getStartingPrice());
        response.setImageUrl(hotel.getImageUrl());
        response.setAddress(hotel.getAddress());
        response.setCountry(hotel.getCountry());
        response.setAmenities(hotel.getAmenities() != null ? new java.util.HashSet<>(hotel.getAmenities()) : java.util.Collections.emptySet());
        response.setImageUrls(hotel.getImageUrls() != null ? new java.util.ArrayList<>(hotel.getImageUrls()) : java.util.Collections.emptyList());
        return response;
    }
}
