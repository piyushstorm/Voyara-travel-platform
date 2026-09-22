package com.travelplatform.config.seed;

import java.util.List;

/**
 * Curated, realistic, deterministic reference datasets for the Voyara seed data system.
 * Contains authentic geographic coordinates, real airport IATA codes, real airline codes,
 * realistic hotel templates across major destinations, train routes, bus corridors, and cab inventory.
 */
public final class SeedConstants {

    private SeedConstants() {}

    public record AirportDef(String code, String name, String city, String country, double latitude, double longitude) {}

    public record AirlineDef(String code, String name, String logoUrl, double rating) {}

    public record HotelTemplate(String name, String city, String country, int starRating, String description,
                                String address, double latitude, double longitude, int basePrice, String[] images) {}

    public record TrainDef(String number, String name, String type, String originCode, String originName, String originCity,
                            String destCode, String destName, String destCity, String depTime, String arrTime,
                            int durationMinutes, String runningDays, String intermediateStations) {}

    public record BusDef(String number, String operator, String type, String origin, String dest,
                         String depTime, String arrTime, int durationMinutes, int totalSeats, int baseFare,
                         double rating, String amenities, String[] boarding, String[] dropping) {}

    public record CabDef(String vehicleType, String vehicleName, int capacity, int luggage, boolean ac,
                         int baseFare, int perKmRate, int minFare, int bookingFee, double rating, String features, String city) {}

    public record HolidayDef(String title, String desc, String dest, String code, String depCity, String tripType,
                             String hotelCat, String mealPlan, String transport, String price, String origPrice,
                             int nights, int days, double rating, String highlights, String inclusions, String exclusions,
                             String imgUrl, String itinerary, boolean featured) {}

    // =========================================================================
    // AIRPORTS (125 realistic airports: India hubs + International gateways)
    // =========================================================================
    public static final List<AirportDef> AIRPORTS = List.of(
        // India - Major Metros & Hubs
        new AirportDef("DEL", "Indira Gandhi International Airport", "Delhi", "India", 28.5562, 77.1000),
        new AirportDef("BOM", "Chhatrapati Shivaji Maharaj International Airport", "Mumbai", "India", 19.0896, 72.8656),
        new AirportDef("BLR", "Kempegowda International Airport", "Bangalore", "India", 13.1979, 77.7063),
        new AirportDef("HYD", "Rajiv Gandhi International Airport", "Hyderabad", "India", 17.2403, 78.4294),
        new AirportDef("MAA", "Chennai International Airport", "Chennai", "India", 12.9941, 80.1709),
        new AirportDef("CCU", "Netaji Subhas Chandra Bose International Airport", "Kolkata", "India", 22.6547, 88.4467),
        new AirportDef("PNQ", "Pune Airport", "Pune", "India", 18.5822, 73.9197),
        new AirportDef("AMD", "Sardar Vallabhbhai Patel International Airport", "Ahmedabad", "India", 23.0772, 72.6347),
        new AirportDef("GOI", "Dabolim International Airport", "Goa", "India", 15.3809, 73.8314),
        new AirportDef("GOX", "Manohar International Airport Mopa", "Goa", "India", 15.7686, 73.8672),
        new AirportDef("JAI", "Jaipur International Airport", "Jaipur", "India", 26.8242, 75.8122),
        new AirportDef("LKO", "Chaudhary Charan Singh International Airport", "Lucknow", "India", 26.7606, 80.8893),
        new AirportDef("COK", "Cochin International Airport", "Kochi", "India", 10.1520, 76.4019),
        new AirportDef("IXC", "Shaheed Bhagat Singh International Airport", "Chandigarh", "India", 30.6735, 76.7885),
        new AirportDef("IDR", "Devi Ahilyabai Holkar Airport", "Indore", "India", 22.7217, 75.8011),
        new AirportDef("BPL", "Raja Bhoj Airport", "Bhopal", "India", 23.2875, 77.3378),
        new AirportDef("NAG", "Dr. Babasaheb Ambedkar International Airport", "Nagpur", "India", 21.0922, 79.0472),
        new AirportDef("STV", "Surat International Airport", "Surat", "India", 21.1139, 72.7419),
        new AirportDef("VNS", "Lal Bahadur Shastri International Airport", "Varanasi", "India", 25.4522, 82.8594),
        new AirportDef("ATQ", "Sri Guru Ram Dass Jee International Airport", "Amritsar", "India", 31.7096, 74.7973),
        new AirportDef("SXR", "Sheikh ul-Alam International Airport", "Srinagar", "India", 33.9871, 74.7742),
        new AirportDef("PAT", "Jay Prakash Narayan Airport", "Patna", "India", 25.5913, 85.0880),
        new AirportDef("BBI", "Biju Patnaik International Airport", "Bhubaneswar", "India", 20.2444, 85.8178),
        new AirportDef("GAU", "Lokpriya Gopinath Bordoloi International Airport", "Guwahati", "India", 26.1061, 91.5859),
        new AirportDef("CJB", "Coimbatore International Airport", "Coimbatore", "India", 11.0300, 77.0434),
        new AirportDef("TRV", "Thiruvananthapuram International Airport", "Thiruvananthapuram", "India", 8.4821, 76.9200),
        new AirportDef("UDR", "Maharana Pratap Airport", "Udaipur", "India", 24.6178, 73.8961),
        new AirportDef("JDH", "Jodhpur Airport", "Jodhpur", "India", 26.2511, 73.0489),
        new AirportDef("DED", "Jolly Grant Airport", "Dehradun", "India", 30.1897, 78.1803),
        new AirportDef("AGR", "Agra Airport / Kheria Airforce Station", "Agra", "India", 27.1558, 77.9609),
        new AirportDef("IXZ", "Veer Savarkar International Airport", "Port Blair", "India", 11.6410, 92.7297),
        new AirportDef("IXB", "Bagdogra International Airport", "Siliguri", "India", 26.6812, 88.3286),
        new AirportDef("IXL", "Kushok Bakula Rimpochee Airport", "Leh", "India", 34.1359, 77.5465),
        new AirportDef("IXJ", "Jammu Civil Enclave", "Jammu", "India", 32.6891, 74.8374),
        new AirportDef("RPR", "Swami Vivekananda Airport", "Raipur", "India", 21.1804, 81.7388),
        new AirportDef("IXR", "Birsa Munda Airport", "Ranchi", "India", 23.3143, 85.3217),
        new AirportDef("VTZ", "Visakhapatnam International Airport", "Visakhapatnam", "India", 17.7212, 83.2245),
        new AirportDef("IXE", "Mangalore International Airport", "Mangalore", "India", 12.9613, 74.8900),
        new AirportDef("CCJ", "Calicut International Airport", "Kozhikode", "India", 11.1368, 75.9553),
        new AirportDef("TRZ", "Tiruchirappalli International Airport", "Tiruchirappalli", "India", 10.7654, 78.7097),
        new AirportDef("IXM", "Madurai Airport", "Madurai", "India", 9.8345, 78.0934),
        new AirportDef("VGA", "Vijayawada Airport", "Vijayawada", "India", 16.5304, 80.7968),
        new AirportDef("TIR", "Tirupati Airport", "Tirupati", "India", 13.6325, 79.5436),
        new AirportDef("GWL", "Gwalior Airport", "Gwalior", "India", 26.2933, 78.2278),
        new AirportDef("JLR", "Jabalpur Airport", "Jabalpur", "India", 23.1778, 80.0522),
        new AirportDef("KUU", "Kullu-Manali Airport Bhuntar", "Manali", "India", 31.8767, 77.1542),
        new AirportDef("DHM", "Kangra Airport Gaggal", "Dharamshala", "India", 32.1651, 76.2634),
        new AirportDef("BDQ", "Vadodara Airport", "Vadodara", "India", 22.3362, 73.2263),
        new AirportDef("RAJ", "Rajkot International Airport", "Rajkot", "India", 22.3092, 70.7794),
        new AirportDef("BHU", "Bhavnagar Airport", "Bhavnagar", "India", 21.7522, 72.1856),
        new AirportDef("IMF", "Bir Tikendrajit International Airport", "Imphal", "India", 24.7600, 93.8967),
        new AirportDef("IXA", "Maharaja Bir Bikram Airport", "Agartala", "India", 23.8869, 91.2405),
        new AirportDef("DMU", "Dimapur Airport", "Dimapur", "India", 25.8839, 93.7711),
        new AirportDef("SHL", "Shillong Airport Umroi", "Shillong", "India", 25.7036, 91.9786),
        new AirportDef("AJL", "Lengpui Airport", "Aizawl", "India", 23.8408, 92.6192),
        new AirportDef("IXS", "Silchar Airport Kumbhirgram", "Silchar", "India", 24.9128, 92.9792),
        new AirportDef("DIB", "Dibrugarh Airport", "Dibrugarh", "India", 27.4839, 95.0178),
        new AirportDef("IXT", "Pasighat Airport", "Pasighat", "India", 28.0667, 95.3333),
        new AirportDef("HJR", "Khajuraho Airport", "Khajuraho", "India", 24.8172, 79.9189),
        new AirportDef("BEK", "Bareilly Airport", "Bareilly", "India", 28.4222, 79.4500),
        new AirportDef("AYJ", "Maharishi Valmiki International Airport", "Ayodhya", "India", 26.7383, 82.1558),
        new AirportDef("CNN", "Kannur International Airport", "Kannur", "India", 11.9175, 75.5481),
        new AirportDef("HBX", "Hubli Airport", "Hubli", "India", 15.3617, 75.0847),
        new AirportDef("IXG", "Belgaum Airport", "Belgaum", "India", 15.8592, 74.6183),
        new AirportDef("KLH", "Kolhapur Airport", "Kolhapur", "India", 16.6644, 74.2817),
        new AirportDef("IXU", "Chhatrapati Sambhajinagar Airport", "Aurangabad", "India", 19.8631, 75.3981),
        new AirportDef("SAG", "Shirdi Airport", "Shirdi", "India", 19.6897, 74.3792),
        new AirportDef("GAY", "Gaya Airport", "Gaya", "India", 24.7442, 84.9511),
        new AirportDef("JRG", "Jharsuguda Airport Veer Surendra Sai", "Jharsuguda", "India", 21.9147, 84.0506),

        // International - Middle East & Gulf
        new AirportDef("DXB", "Dubai International Airport", "Dubai", "United Arab Emirates", 25.2532, 55.3657),
        new AirportDef("AUH", "Zayed International Airport", "Abu Dhabi", "United Arab Emirates", 24.4330, 54.6511),
        new AirportDef("SHJ", "Sharjah International Airport", "Sharjah", "United Arab Emirates", 25.3286, 55.5172),
        new AirportDef("DOH", "Hamad International Airport", "Doha", "Qatar", 25.2731, 51.6081),
        new AirportDef("BAH", "Bahrain International Airport", "Manama", "Bahrain", 26.2708, 50.6336),
        new AirportDef("KWI", "Kuwait International Airport", "Kuwait City", "Kuwait", 29.2267, 47.9789),
        new AirportDef("MCT", "Muscat International Airport", "Muscat", "Oman", 23.5933, 58.2844),
        new AirportDef("RUH", "King Khalid International Airport", "Riyadh", "Saudi Arabia", 24.9578, 46.6989),
        new AirportDef("JED", "King Abdulaziz International Airport", "Jeddah", "Saudi Arabia", 21.6796, 39.1565),

        // International - Southeast Asia & South Asia
        new AirportDef("SIN", "Singapore Changi Airport", "Singapore", "Singapore", 1.3644, 103.9915),
        new AirportDef("BKK", "Suvarnabhumi Airport", "Bangkok", "Thailand", 13.6900, 100.7501),
        new AirportDef("DMK", "Don Mueang International Airport", "Bangkok", "Thailand", 13.9126, 100.6068),
        new AirportDef("HKT", "Phuket International Airport", "Phuket", "Thailand", 8.1132, 98.3169),
        new AirportDef("KUL", "Kuala Lumpur International Airport", "Kuala Lumpur", "Malaysia", 2.7456, 101.7099),
        new AirportDef("DPS", "Ngurah Rai International Airport", "Bali", "Indonesia", -8.7482, 115.1672),
        new AirportDef("CGK", "Soekarno-Hatta International Airport", "Jakarta", "Indonesia", -6.1256, 106.6559),
        new AirportDef("HAN", "Noi Bai International Airport", "Hanoi", "Vietnam", 21.2212, 105.8072),
        new AirportDef("SGN", "Tan Son Nhat International Airport", "Ho Chi Minh City", "Vietnam", 10.8188, 106.6520),
        new AirportDef("CMB", "Bandaranaike International Airport", "Colombo", "Sri Lanka", 7.1808, 79.8841),
        new AirportDef("MLE", "Velana International Airport", "Male", "Maldives", 4.1918, 73.5291),
        new AirportDef("KTM", "Tribhuvan International Airport", "Kathmandu", "Nepal", 27.6966, 85.3591),
        new AirportDef("DAC", "Hazrat Shahjalal International Airport", "Dhaka", "Bangladesh", 23.8433, 90.4000),

        // International - Europe & UK
        new AirportDef("LHR", "Heathrow Airport", "London", "United Kingdom", 51.4700, -0.4543),
        new AirportDef("LGW", "Gatwick Airport", "London", "United Kingdom", 51.1537, -0.1821),
        new AirportDef("MAN", "Manchester Airport", "Manchester", "United Kingdom", 53.3537, -2.2750),
        new AirportDef("CDG", "Charles de Gaulle Airport", "Paris", "France", 49.0097, 2.5479),
        new AirportDef("ORY", "Orly Airport", "Paris", "France", 48.7262, 2.3652),
        new AirportDef("FRA", "Frankfurt Airport", "Frankfurt", "Germany", 50.0379, 8.5622),
        new AirportDef("MUC", "Munich Airport", "Munich", "Germany", 48.3537, 11.7861),
        new AirportDef("AMS", "Amsterdam Airport Schiphol", "Amsterdam", "Netherlands", 52.3105, 4.7683),
        new AirportDef("ZRH", "Zurich Airport", "Zurich", "Switzerland", 47.4582, 8.5555),
        new AirportDef("GVA", "Geneva Airport", "Geneva", "Switzerland", 46.2370, 6.1092),
        new AirportDef("FCO", "Leonardo da Vinci-Fiumicino Airport", "Rome", "Italy", 41.8003, 12.2389),
        new AirportDef("MXP", "Milan Malpensa Airport", "Milan", "Italy", 45.6301, 8.7255),
        new AirportDef("BCN", "Josep Tarradellas Barcelona-El Prat Airport", "Barcelona", "Spain", 41.2974, 2.0833),
        new AirportDef("MAD", "Adolfo Suarez Madrid-Barajas Airport", "Madrid", "Spain", 40.4839, -3.5680),
        new AirportDef("IST", "Istanbul Airport", "Istanbul", "Turkey", 41.2753, 28.7519),
        new AirportDef("VIE", "Vienna International Airport", "Vienna", "Austria", 48.1103, 16.5697),

        // International - North America & Australia
        new AirportDef("JFK", "John F. Kennedy International Airport", "New York", "United States", 40.6413, -73.7781),
        new AirportDef("EWR", "Newark Liberty International Airport", "New York", "United States", 40.6895, -74.1745),
        new AirportDef("SFO", "San Francisco International Airport", "San Francisco", "United States", 37.6213, -122.3790),
        new AirportDef("ORD", "O'Hare International Airport", "Chicago", "United States", 41.9742, -87.9073),
        new AirportDef("LAX", "Los Angeles International Airport", "Los Angeles", "United States", 33.9416, -118.4085),
        new AirportDef("YYZ", "Toronto Pearson International Airport", "Toronto", "Canada", 43.6777, -79.6248),
        new AirportDef("YVR", "Vancouver International Airport", "Vancouver", "Canada", 49.1967, -123.1815),
        new AirportDef("SYD", "Sydney Kingsford Smith Airport", "Sydney", "Australia", -33.9399, 151.1753),
        new AirportDef("MEL", "Melbourne Airport", "Melbourne", "Australia", -37.6690, 144.8410),
        new AirportDef("AKL", "Auckland Airport", "Auckland", "New Zealand", -37.0082, 174.7850),

        // East Asia
        new AirportDef("HND", "Tokyo Haneda Airport", "Tokyo", "Japan", 35.5494, 139.7798),
        new AirportDef("NRT", "Narita International Airport", "Tokyo", "Japan", 35.7720, 140.3929),
        new AirportDef("ICN", "Incheon International Airport", "Seoul", "South Korea", 37.4602, 126.4407),
        new AirportDef("HKG", "Hong Kong International Airport", "Hong Kong", "Hong Kong", 22.3080, 113.9185)
    );

    // =========================================================================
    // AIRLINES (16 real domestic & international operating carriers)
    // =========================================================================
    public static final List<AirlineDef> AIRLINES = List.of(
        new AirlineDef("6E", "IndiGo", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.2),
        new AirlineDef("AI", "Air India", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.0),
        new AirlineDef("QP", "Akasa Air", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.1),
        new AirlineDef("SG", "SpiceJet", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 3.8),
        new AirlineDef("UK", "Vistara", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.5),
        new AirlineDef("IX", "Air India Express", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 3.9),
        new AirlineDef("EK", "Emirates", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.7),
        new AirlineDef("QR", "Qatar Airways", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.8),
        new AirlineDef("SQ", "Singapore Airlines", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.8),
        new AirlineDef("BA", "British Airways", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.3),
        new AirlineDef("LH", "Lufthansa", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.4),
        new AirlineDef("EY", "Etihad Airways", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.6),
        new AirlineDef("UL", "SriLankan Airlines", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.0),
        new AirlineDef("TG", "Thai Airways", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.2),
        new AirlineDef("MH", "Malaysia Airlines", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.1),
        new AirlineDef("AF", "Air France", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=120", 4.3)
    );

    // =========================================================================
    // FLIGHT CORRIDOR PAIRS (Major domestic + international trade/leisure lanes)
    // =========================================================================
    public static final String[] FLIGHT_CORRIDORS = {
        // High-density Golden Quad / Domestic Trunk Routes
        "BOM-DEL", "DEL-BOM",
        "BOM-BLR", "BLR-BOM",
        "DEL-BLR", "BLR-DEL",
        "BOM-GOI", "GOI-BOM",
        "DEL-GOI", "GOI-DEL",
        "BLR-GOI", "GOI-BLR",
        "BOM-HYD", "HYD-BOM",
        "DEL-HYD", "HYD-DEL",
        "BLR-HYD", "HYD-BLR",
        "BOM-MAA", "MAA-BOM",
        "DEL-MAA", "MAA-DEL",
        "BLR-MAA", "MAA-BLR",
        "DEL-CCU", "CCU-DEL",
        "BOM-CCU", "CCU-BOM",
        "DEL-JAI", "JAI-DEL",
        "BOM-JAI", "JAI-BOM",
        "DEL-PNQ", "PNQ-DEL",
        "BOM-PNQ", "PNQ-BOM",
        "DEL-AMD", "AMD-DEL",
        "BOM-AMD", "AMD-BOM",
        "DEL-LKO", "LKO-DEL",
        "BOM-LKO", "LKO-BOM",
        "DEL-COK", "COK-DEL",
        "BOM-COK", "COK-BOM",
        "BLR-COK", "COK-BLR",
        "DEL-IXC", "IXC-DEL",
        "BOM-IDR", "IDR-BOM",
        "DEL-IDR", "IDR-DEL",
        "DEL-VNS", "VNS-DEL",
        "BOM-VNS", "VNS-BOM",
        "DEL-ATQ", "ATQ-DEL",
        "DEL-SXR", "SXR-DEL",
        "BOM-SXR", "SXR-BOM",
        "DEL-PAT", "PAT-DEL",
        "BOM-BBI", "BBI-BOM",
        "DEL-GAU", "GAU-DEL",
        "CCU-GAU", "GAU-CCU",
        "DEL-UDR", "UDR-DEL",
        "BOM-UDR", "UDR-BOM",
        "DEL-DED", "DED-DEL",
        "MAA-CJB", "CJB-MAA",
        "MAA-TRV", "TRV-MAA",
        "BLR-IXZ", "IXZ-BLR",
        "DEL-IXL", "IXL-DEL",

        // International Corridors from India
        "BOM-DXB", "DXB-BOM",
        "DEL-DXB", "DXB-DEL",
        "BLR-DXB", "DXB-BLR",
        "HYD-DXB", "DXB-HYD",
        "COK-DXB", "DXB-COK",
        "BOM-SIN", "SIN-BOM",
        "DEL-SIN", "SIN-DEL",
        "BLR-SIN", "SIN-BLR",
        "MAA-SIN", "SIN-MAA",
        "BOM-BKK", "BKK-BOM",
        "DEL-BKK", "BKK-DEL",
        "DEL-LHR", "LHR-DEL",
        "BOM-LHR", "LHR-BOM",
        "BLR-LHR", "LHR-BLR",
        "DEL-CDG", "CDG-DEL",
        "BOM-CDG", "CDG-BOM",
        "DEL-FRA", "FRA-DEL",
        "BOM-FRA", "FRA-BOM",
        "DEL-DOH", "DOH-DEL",
        "BOM-DOH", "DOH-BOM",
        "DEL-AUH", "AUH-DEL",
        "BOM-AUH", "AUH-BOM",
        "MAA-CMB", "CMB-MAA",
        "BOM-CMB", "CMB-BOM",
        "DEL-MLE", "MLE-DEL",
        "BOM-MLE", "MLE-BOM",
        "DEL-KTM", "KTM-DEL",
        "DEL-KUL", "KUL-DEL",
        "BOM-KUL", "KUL-BOM",
        "DEL-DPS", "DPS-DEL",
        "BOM-DPS", "DPS-BOM",
        "DEL-JFK", "JFK-DEL",
        "BOM-EWR", "EWR-BOM",
        "DEL-SFO", "SFO-DEL",
        "BLR-SFO", "SFO-BLR"
    };

    // Realistic review titles and quotes
    public static final String[] REVIEW_TITLES_5_STAR = {
        "Exceptional experience, exceeded expectations!",
        "World-class hospitality and seamless travel",
        "Absolute luxury and top-notch comfort",
        "Smooth check-in, pristine rooms, delicious breakfast",
        "Punctual flight and very attentive crew",
        "Memorable stay! Will definitely book again",
        "Outstanding value and impeccable attention to detail"
    };

    public static final String[] REVIEW_TITLES_4_STAR = {
        "Very pleasant stay with great location",
        "Solid service and comfortable journey",
        "Good amenities and friendly staff",
        "On-time departure and comfortable seating",
        "Nice breakfast buffet and clean bathroom",
        "Great experience overall, minor wait at check-in"
    };

    public static final String[] REVIEW_TITLES_3_STAR = {
        "Decent for a short trip, average amenities",
        "Met basic expectations but room was compact",
        "Flight arrived safely, legroom was somewhat tight",
        "Location was prime, but breakfast choices were limited",
        "Good value but room decor feels slightly dated"
    };

    public static final String[] REVIEW_TITLES_2_STAR = {
        "Service was sluggish and check-in delayed",
        "Room was noisy and WiFi kept disconnecting",
        "Boarding queue was chaotic and flight had a minor delay",
        "Needs renovation and better staff responsiveness"
    };

    public static final String[] REVIEW_TITLES_1_STAR = {
        "Disappointing experience with unhelpful front desk",
        "AC was noisy and bathroom cleanliness was inadequate",
        "Lengthy unexplained delay without proper communication"
    };

    public static final String[] REVIEW_TEXTS_5_STAR = {
        "Everything was flawless. The staff went out of their way to ensure our comfort. Room was spotless with stunning views, and the dining was truly gourmet.",
        "Smooth flight with punctual takeoff and landing. The crew was exceptionally polite, cabin was immaculate, and baggage arrived promptly.",
        "Incredible beachfront property! The pool villas, sunset views, and spa treatments were worth every rupee. Outstanding concierge service.",
        "One of the best travel experiences we've had. Fast WiFi, luxurious bed, and delicious authentic breakfast buffet. Highly recommended."
    };

    public static final String[] REVIEW_TEXTS_4_STAR = {
        "Very enjoyable trip overall. The location is super convenient near transit and restaurants. Rooms were clean and quiet. Would return.",
        "Flight departed and arrived on schedule. Good seat ergonomics and clean lavatories. Cabin crew was helpful throughout the journey.",
        "Great city hotel for business or leisure. Well-equipped gym, prompt room service, and comfortable workspace."
    };

    public static final String[] REVIEW_TEXTS_3_STAR = {
        "The stay was adequate. Room was a bit smaller than expected from photos, but clean and functional. Decent for an overnight stopover.",
        "Flight was fine and landed safely. Cabin crew did their job, though in-flight entertainment choices were minimal and legroom is average."
    };

    public static final String[] REVIEW_TEXTS_2_STAR = {
        "Check-in took over 35 minutes despite having a confirmed online booking. Room cleanliness could be improved, and hot water was intermittent.",
        "Boarding was delayed by 45 minutes with minimal updates at the gate. Seat recliner was stiff and service was indifferent."
    };

    public static final String[] REVIEW_TEXTS_1_STAR = {
        "Unsatisfactory experience. Noisy air conditioning kept us awake all night. Front desk was indifferent when issues were reported.",
        "Multiple schedule changes and long waiting at baggage claim. Disappointing service compared to ticket fare."
    };

    public static final String[] REVIEW_PHOTOS = {
        "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800",
        "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800",
        "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800",
        "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800",
        "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800",
        "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800",
        "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800",
        "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800"
    };

    public static final String[] REVIEW_REPLIES_MGMT = {
        "Thank you for sharing your thoughtful feedback. We are delighted to hear you had a wonderful experience and look forward to welcoming you back soon!",
        "Thank you for your review. We appreciate your constructive notes regarding our service and have shared them with our team to enhance future guest experiences.",
        "We are glad our location and amenities added convenience to your journey. We hope to host you again on your next visit!",
        "Thank you for choosing Voyara. We deeply value your feedback and look forward to serving you with an even better journey next time."
    };

    public static final String[] REVIEW_REPLIES_USER = {
        "Thanks for the tip regarding early breakfast! Helpful for travelers catching morning flights.",
        "Completely agree about the view from the higher floors, it made our stay extra special too!",
        "Very accurate summary of the seat comfort. Appreciate the detailed review."
    };
}
