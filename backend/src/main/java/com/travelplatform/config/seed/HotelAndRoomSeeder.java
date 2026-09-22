package com.travelplatform.config.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.Room;
import com.travelplatform.repository.HotelRepository;
import com.travelplatform.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
public class HotelAndRoomSeeder {

    private static final Logger log = LoggerFactory.getLogger(HotelAndRoomSeeder.class);

    private final HotelRepository hotelRepo;
    private final RoomRepository roomRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HotelAndRoomSeeder(HotelRepository hotelRepo, RoomRepository roomRepo) {
        this.hotelRepo = hotelRepo;
        this.roomRepo = roomRepo;
    }

    // High quality curated hotel photos
    private static final String[][] HOTEL_IMAGE_SETS = {
        {"https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800", "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800", "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800"},
        {"https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800", "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800"},
        {"https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=800", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800", "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800"},
        {"https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800", "https://images.unsplash.com/photo-1586611292717-f828b167408c?w=800", "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800"},
        {"https://images.unsplash.com/photo-1596178065887-1198b6148b2b?w=800", "https://images.unsplash.com/photo-1587381420270-3e1a5b9e6904?w=800", "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800"},
        {"https://images.unsplash.com/photo-1585135497273-1a86b09fe70e?w=800", "https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?w=800", "https://images.unsplash.com/photo-1563911302283-d2bc129e7570?w=800"},
        {"https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800", "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800"}
    };

    private static final String[] AMENITY_POOL = {
        "WIFI", "POOL", "SPA", "GYM", "RESTAURANT", "BAR", "ROOM_SERVICE",
        "PARKING", "AIRPORT_SHUTTLE", "BREAKFAST_INCLUDED", "FITNESS_CENTER",
        "BUSINESS_CENTER", "CONCIERGE", "VALET_PARKING", "BEACH_ACCESS"
    };

    @Transactional
    public List<Hotel> seedHotelsAndRooms() {
        long currentHotelCount = hotelRepo.count();
        if (currentHotelCount >= 300) {
            log.info("Hotels already seeded ({}), skipping bulk hotel generation.", currentHotelCount);
            return hotelRepo.findAll();
        }

        log.info("Generating realistic hotel & room inventory across 35+ destinations...");

        // Destinations with realistic hotel naming roots and coordinates
        record CityMeta(String city, String country, double lat, double lon, String[] namePrefixes, String[] areas) {}

        List<CityMeta> destinations = List.of(
            new CityMeta("Mumbai", "India", 18.9220, 72.8347,
                new String[]{"The Grand", "Taj Marine", "Trident", "St. Regis", "Novotel", "JW Marriott", "The Oberoi", "Suba Palace", "Residency", "Sea Green", "Boutique"},
                new String[]{"Juhu Beach", "Marine Drive", "Bandra West", "BKC", "Colaba", "Powai", "Andheri East"}),
            new CityMeta("Delhi", "India", 28.6139, 77.2090,
                new String[]{"The Imperial", "ITC Maurya", "The Leela", "Taj Mahal", "Hyatt Regency", "Radisson Blu", "The Lalit", "Bloomrooms", "The Claridges", "Park Plaza"},
                new String[]{"Connaught Place", "Chanakyapuri", "Aerocity", "South Extension", "Saket", "Karol Bagh", "Paharganj"}),
            new CityMeta("Goa", "India", 15.2993, 74.1240,
                new String[]{"W Goa", "Alila Diwa", "Grand Hyatt", "Taj Exotica", "Caravela Beach", "Heritage Village", "Santana Beach", "Sea Breeze", "Acron Waterfront", "Hard Rock"},
                new String[]{"Calangute", "Baga", "Candolim", "Anjuna", "Vagator", "Colva", "Morjim", "Panaji"}),
            new CityMeta("Bangalore", "India", 12.9716, 77.5946,
                new String[]{"The Leela Palace", "ITC Gardenia", "Taj West End", "Ritz-Carlton", "Sheraton Grand", "Lemon Tree Premier", "Chancery Pavilion", "Ibis", "Keys Select"},
                new String[]{"Indiranagar", "Koramangala", "Whitefield", "MG Road", "UB City", "Electronic City", "Hebbal"}),
            new CityMeta("Hyderabad", "India", 17.3850, 78.4867,
                new String[]{"Taj Falaknuma", "ITC Kohenur", "Park Hyatt", "Trident", "Marriott Hotel", "Novotel Hitec", "Golkonda Hotel", "Avasa", "Daspalla"},
                new String[]{"Banjara Hills", "Jubilee Hills", "Hitec City", "Gachibowli", "Begumpet", "Abids"}),
            new CityMeta("Chennai", "India", 13.0827, 80.2707,
                new String[]{"ITC Grand Chola", "Taj Coromandel", "The Leela Palace", "Feathers Hotel", "Hyatt Regency", "Park Elanza", "Residency Towers", "Savera"},
                new String[]{"OMR", "T Nagar", "Mylapore", "Guindy", "Nungambakkam", "ECR"}),
            new CityMeta("Jaipur", "India", 26.9124, 75.7873,
                new String[]{"Rambagh Palace", "Jai Mahal", "ITC Rajputana", "Fairmont", "Samode Haveli", "Alsisar Haveli", "Pearl Palace", "Shahpura House", "Umaid Bhawan"},
                new String[]{"Civil Lines", "MI Road", "Bani Park", "C-Scheme", "Amer Road", "Tonk Road"}),
            new CityMeta("Udaipur", "India", 24.5854, 73.7125,
                new String[]{"Taj Lake Palace", "The Oberoi Udaivilas", "Leela Palace", "Fateh Prakash", "Trident", "Chunda Palace", "Amet Haveli", "Jagat Niwas"},
                new String[]{"Lake Pichola", "Fateh Sagar", "City Palace Complex", "Hathipole"}),
            new CityMeta("Jodhpur", "India", 26.2389, 73.0243,
                new String[]{"Umaid Bhawan Palace", "Ajit Bhawan", "Raas", "Indana Palace", "Ranbanka Palace", "Ratan Vilas"},
                new String[]{"Old City", "Ratanada", "Cantt Area", "Clock Tower"}),
            new CityMeta("Manali", "India", 32.2432, 77.1892,
                new String[]{"Span Resort", "The Himalayan", "Solang Valley Resort", "Snow Peak Retreat", "Apple Country Resort", "Johnson Lodge"},
                new String[]{"Mall Road", "Old Manali", "Aleo", "Log Huts", "Solang"}),
            new CityMeta("Srinagar", "India", 34.0837, 74.7973,
                new String[]{"The Lalit Grand Palace", "Vivanta Dal View", "Heritage Luxury Houseboats", "Wangnoo Houseboats", "Hotel Broadway"},
                new String[]{"Dal Lake", "Boulevard Road", "Nigeen Lake", "Rajbagh"}),
            new CityMeta("Rishikesh", "India", 30.0869, 78.2676,
                new String[]{"Ananda in the Himalayas", "Aloha on the Ganges", "Taj Rishikesh Resort", "Ganga Kinare", "Divine Resort"},
                new String[]{"Tapovan", "Laxman Jhula", "Ram Jhula", "Shivpuri"}),
            new CityMeta("Agra", "India", 27.1767, 78.0081,
                new String[]{"The Oberoi Amarvilas", "ITC Mughal", "Taj Hotel & Convention", "Courtyard by Marriott", "Crystal Sarovar"},
                new String[]{"Taj East Gate", "Fatehabad Road", "Cantonment"}),
            new CityMeta("Kolkata", "India", 22.5726, 88.3639,
                new String[]{"The Oberoi Grand", "ITC Sonar", "Taj Bengal", "JW Marriott", "The Lalit Great Eastern", "Peerless Inn"},
                new String[]{"Park Street", "New Town", "Salt Lake", "Alipore"}),
            new CityMeta("Pune", "India", 18.5204, 73.8567,
                new String[]{"JW Marriott", "Conrad Pune", "The Ritz-Carlton", "Blue Diamond", "Sheraton Grand", "Hyatt Pune"},
                new String[]{"Senapati Bapat Road", "Koregaon Park", "Viman Nagar", "Hinjewadi"}),
            new CityMeta("Kochi", "India", 9.9312, 76.2673,
                new String[]{"Grand Hyatt Bolgatty", "Brunton Boatyard", "Taj Malabar", "Fragrant Nature", "Casino Hotel", "Forte Kochi"},
                new String[]{"Fort Kochi", "Willingdon Island", "Marine Drive", "Bolgatty"}),
            new CityMeta("Dubai", "United Arab Emirates", 25.2048, 55.2708,
                new String[]{"Burj Al Arab", "Atlantis The Palm", "Armani Hotel", "Jumeirah Beach Hotel", "Address Downtown", "Raffles Dubai", "Sofitel Downtown"},
                new String[]{"Downtown Dubai", "Palm Jumeirah", "Dubai Marina", "Business Bay", "Deira"}),
            new CityMeta("Singapore", "Singapore", 1.3521, 103.8198,
                new String[]{"Marina Bay Sands", "Raffles Hotel", "The Fullerton", "Shangri-La", "Capella Sentosa", "Pan Pacific", "PARKROYAL COLLECTION"},
                new String[]{"Marina Bay", "Orchard Road", "Sentosa Island", "Bugis", "Clarke Quay"}),
            new CityMeta("Bangkok", "Thailand", 13.7563, 100.5018,
                new String[]{"Mandarin Oriental", "The Peninsula", "Capella Bangkok", "Siam Kempinski", "Anantara Riverside", "Banyan Tree"},
                new String[]{"Chao Phraya Riverside", "Sukhumvit", "Siam", "Silom"}),
            new CityMeta("Maldives", "Maldives", 4.1755, 73.5093,
                new String[]{"Soneva Jani", "Gili Lankanfushi", "Conrad Rangali", "Baros Maldives", "Kurumba Maldives", "Anantara Veli"},
                new String[]{"North Male Atoll", "South Male Atoll", "Baa Atoll", "Noonu Atoll"}),
            new CityMeta("Bali", "Indonesia", -8.4095, 115.1889,
                new String[]{"Four Seasons Sayan", "Ayana Resort", "The Mulia", "Bulgari Resort", "Alila Villas Uluwatu", "Viceroy Bali"},
                new String[]{"Ubud", "Seminyak", "Uluwatu", "Nusa Dua", "Canggu"}),
            new CityMeta("Varanasi", "India", 25.3176, 82.9739,
                new String[]{"BrijRama Palace", "Taj Ganges", "Radisson Hotel", "Suryauday Haveli", "Heritage Inn"},
                new String[]{"Ghats", "Cantonment", "Assi Ghat"}),
            new CityMeta("Amritsar", "India", 31.6340, 74.8723,
                new String[]{"Taj Swarna", "Radisson Blu", "Hyatt Regency", "Ramada", "Hotel City Park"},
                new String[]{"Golden Temple Area", "Mall Road", "Airport Road"}),
            new CityMeta("Chandigarh", "India", 30.7333, 76.7794,
                new String[]{"The Oberoi Sukhvilas", "JW Marriott", "Taj Chandigarh", "Hyatt Regency", "Mountview"},
                new String[]{"Sector 17", "Sector 35", "Industrial Area"})
        );

        List<Hotel> hotelsToSave = new ArrayList<>();
        List<Room> roomsToSave = new ArrayList<>();
        Random rng = new Random(42);

        for (CityMeta cm : destinations) {
            // Seed 10-16 hotels per city to reach 350+ hotels across destinations
            int hotelCountForCity = 12 + rng.nextInt(5);

            for (int i = 0; i < hotelCountForCity; i++) {
                String prefix = cm.namePrefixes()[i % cm.namePrefixes().length];
                String area = cm.areas()[i % cm.areas().length];
                String name = prefix + " " + cm.city() + (i >= cm.namePrefixes().length ? " " + area : "");

                int starRating;
                if (i < 3) starRating = 5;
                else if (i < 8) starRating = 4;
                else if (i < 12) starRating = 3;
                else starRating = 2;

                double guestRating = Math.round((3.4 + (rng.nextDouble() * 1.5)) * 10.0) / 10.0;
                int reviewCount = 45 + rng.nextInt(600);

                int basePrice = switch (starRating) {
                    case 5 -> 7500 + (rng.nextInt(12) * 500);
                    case 4 -> 3800 + (rng.nextInt(8) * 300);
                    case 3 -> 2100 + (rng.nextInt(6) * 200);
                    default -> 1200 + (rng.nextInt(4) * 150);
                };

                Hotel hotel = new Hotel();
                hotel.setName(name);
                hotel.setCity(cm.city());
                hotel.setCountry(cm.country());
                hotel.setStarRating(starRating);
                hotel.setAddress(area + ", " + cm.city() + ", " + cm.country());
                hotel.setLatitude(cm.lat() + ((rng.nextDouble() - 0.5) * 0.08));
                hotel.setLongitude(cm.lon() + ((rng.nextDouble() - 0.5) * 0.08));
                hotel.setGuestRating(guestRating);
                hotel.setReviewCount(reviewCount);
                hotel.setStartingPrice(BigDecimal.valueOf(basePrice));
                hotel.setDescription("Experience refined hospitality and bespoke comfort at " + name + ", strategically located in " + area + ". Features stylish appointments, gourmet culinary options, and dedicated concierge services.");

                // Image Selection
                String[] imgSet = HOTEL_IMAGE_SETS[(i + cm.city().hashCode()) & 0x7FFFFFFF % HOTEL_IMAGE_SETS.length];
                hotel.setImageUrl(imgSet[0]);
                hotel.setImageUrls(Arrays.asList(imgSet));

                // Amenities
                Set<String> amenities = new HashSet<>();
                amenities.add("WIFI");
                amenities.add("AIRPORT_SHUTTLE");
                if (starRating >= 3) { amenities.add("RESTAURANT"); amenities.add("ROOM_SERVICE"); }
                if (starRating >= 4) { amenities.add("POOL"); amenities.add("GYM"); amenities.add("BREAKFAST_INCLUDED"); }
                if (starRating == 5) { amenities.add("SPA"); amenities.add("CONCIERGE"); amenities.add("BAR"); amenities.add("VALET_PARKING"); }
                hotel.setAmenities(amenities);
                hotel.setActive(true);

                hotelsToSave.add(hotel);
            }
        }

        List<Hotel> savedHotels = hotelRepo.saveAll(hotelsToSave);
        log.info("Persisted {} hotels. Now creating room categories...", savedHotels.size());

        // Room Types: Standard, Deluxe, Executive Suite, Presidential Suite
        String[][] roomTemplates = {
            {"STANDARD", "Standard Room", "KING", "24", "Comfortable room featuring king bed, ergonomic desk, high-speed Wi-Fi, and ensuite shower.", "Free Wi-Fi,Air Conditioning,Ensuite Bathroom,Smart LED TV,Work Desk,Coffee/Tea Maker"},
            {"DELUXE", "Deluxe City View Room", "KING", "34", "Spacious deluxe room with panoramic skyline or garden views, mini bar, and plush bathrobes.", "Free Wi-Fi,Air Conditioning,City View,Mini Bar,King Bed,Smart TV,Bathrobes,Free Breakfast,Coffee Machine"},
            {"SUITE", "Executive Club Suite", "KING", "52", "Luxury suite offering separate living salon, executive lounge privileges, and marble soaking tub.", "Panoramic View,Executive Lounge Access,Living Area,Espresso Machine,Luxury Toiletries,Breakfast Included,Priority Check-in"},
            {"PRESIDENTIAL", "Royal Presidential Suite", "KING", "85", "Opulent premier suite with expansive dining hall, dedicated butler, private jacuzzi, and skyline terrace.", "Private Terrace,Butler Service,Panoramic View,Jacuzzi,Executive Lounge,Dining Area,Airport Limousine"}
        };

        for (Hotel hotel : savedHotels) {
            int categoriesForHotel = hotel.getStarRating() >= 4 ? 4 : (hotel.getStarRating() >= 3 ? 3 : 2);
            int baseHotelPrice = hotel.getStartingPrice().intValue();

            for (int rIdx = 0; rIdx < categoriesForHotel; rIdx++) {
                String[] tmpl = roomTemplates[rIdx];
                Room room = new Room();
                room.setHotel(hotel);
                room.setRoomType(tmpl[0]);
                room.setName(tmpl[1]);
                room.setBedType(tmpl[2]);
                room.setSizeSqm(Double.parseDouble(tmpl[3]));
                room.setMaxGuests(tmpl[0].equals("PRESIDENTIAL") ? 4 : (tmpl[0].equals("SUITE") ? 3 : 2));
                room.setBedCount(tmpl[0].equals("PRESIDENTIAL") ? 2 : 1);
                room.setDescription(tmpl[4]);
                room.setAmenities(tmpl[5]);
                room.setCurrency("INR");
                room.setActive(true);

                double multiplier = switch (tmpl[0]) {
                    case "STANDARD" -> 1.0;
                    case "DELUXE" -> 1.35;
                    case "SUITE" -> 2.15;
                    case "PRESIDENTIAL" -> 3.60;
                    default -> 1.0;
                };
                BigDecimal roomPrice = BigDecimal.valueOf((int) (baseHotelPrice * multiplier));
                room.setPricePerNight(roomPrice);
                room.setBasePrice(roomPrice);

                int totalRooms = 6 + rng.nextInt(12);
                int booked = rng.nextInt(totalRooms - 1);
                room.setTotalRooms(totalRooms);
                room.setAvailableRooms(totalRooms - booked);

                // Images
                List<String> hotelImgs = hotel.getImageUrls();
                if (hotelImgs != null && !hotelImgs.isEmpty()) {
                    String roomImg = hotelImgs.get(rIdx % hotelImgs.size());
                    room.setImageUrl(roomImg);
                    try {
                        room.setImagesJson(objectMapper.writeValueAsString(hotelImgs));
                    } catch (Exception ignored) {}
                }

                roomsToSave.add(room);

                if (roomsToSave.size() >= 500) {
                    roomRepo.saveAll(roomsToSave);
                    roomsToSave.clear();
                }
            }
        }

        if (!roomsToSave.isEmpty()) {
            roomRepo.saveAll(roomsToSave);
            roomsToSave.clear();
        }

        log.info("Total rooms persisted: {}", roomRepo.count());
        return hotelRepo.findAll();
    }
}
