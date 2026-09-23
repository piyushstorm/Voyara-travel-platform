package com.travelplatform.config.seed;

import com.travelplatform.entity.HolidayPackage;
import com.travelplatform.repository.HolidayPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HolidayPackageSeeder {

    private static final Logger log = LoggerFactory.getLogger(HolidayPackageSeeder.class);

    private final HolidayPackageRepository holidayRepo;

    public HolidayPackageSeeder(HolidayPackageRepository holidayRepo) {
        this.holidayRepo = holidayRepo;
    }

    @Transactional
    public void seedHolidayPackages() {
        List<HolidayPackage> existingPackages = holidayRepo.findAll();
        Set<String> existingTitles = existingPackages.stream()
                .map(HolidayPackage::getTitle)
                .collect(java.util.stream.Collectors.toSet());

        if (existingTitles.size() >= 12) {
            log.info("Holiday packages already seeded ({}), skipping.", existingTitles.size());
            return;
        }

        log.info("Seeding realistic holiday package inventory across domestic and international tourist hubs...");

        List<SeedConstants.HolidayDef> packages = List.of(
            new SeedConstants.HolidayDef(
                "Goa Beach Paradise & Sunset Cruises",
                "5N/6D all-inclusive beach getaway with water sports, beach shack dining, and Mandovi river cruise. Stay at a premier beachfront resort.",
                "Goa", "GOI", "DELHI", "DOMESTIC", "4_STAR", "ALL_INCLUSIVE", "FLIGHT",
                "18999", "24999", 5, 6, 4.7,
                "Private Beach,Watersports,Dudhsagar Falls,Sunset Cruise",
                "Airport transfers,4-star Resort,Daily Breakfast & Dinner,Watersports Pass,Sightseeing",
                "Personal expenses,Alcoholic beverages,Optional scuba gear",
                "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800",
                "Day 1: Arrival at Goa Airport, private transfer to North Goa resort, welcome drink, evening at Calangute Beach.\nDay 2: Fort Aguada, Sinquerim Beach, parasailing, and evening Mandovi luxury sunset cruise with folk dance.\nDay 3: South Goa heritage tour - Basilica of Bom Jesus, Se Cathedral, Mangueshi Temple, and Miramar beach walk.\nDay 4: Day excursion to Dudhsagar Waterfalls and Sahakari spice plantation with authentic Goan lunch.\nDay 5: Leisure day for beach shacks, Anjuna flea market shopping, and beachside candlelight dinner.\nDay 6: Buffet breakfast, souvenir shopping, and airport drop.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Kerala Serenity: Munnar Tea Hills & Alleppey Houseboat",
                "5N/6D tranquil journey through God's Own Country. Explore tea estates in Munnar, wildlife in Thekkady, and a private backwater cruise in Alleppey.",
                "Kerala", "COK", "MUMBAI", "DOMESTIC", "4_STAR", "HALF_BOARD", "FLIGHT",
                "21499", "28999", 5, 6, 4.8,
                "Luxury Houseboat,Munnar Tea Gardens,Periyar Wildlife,Kathakali Show",
                "Airport pickup & drop,AC vehicle,Resort stays,Overnight deluxe houseboat stay with all meals,Sightseeing entry",
                "Airfare,Ayurvedic massage charges,Personal gratuities",
                "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800",
                "Day 1: Arrive at Cochin Airport, drive through scenic Cheeyappara waterfalls to Munnar. Check-in to hillside resort.\nDay 2: Full day Munnar - Eravikulam National Park, Mattupetty Dam, Echo Point, and Tata Tea Museum visit.\nDay 3: Scenic drive to Thekkady. Spice plantation walking tour, evening boat safari on Periyar Lake, Kathakali show.\nDay 4: Drive to Alleppey, board traditional luxury kettuvallam houseboat. Cruise scenic palm-fringed backwaters with freshly caught Karimeen lunch.\nDay 5: Disembark houseboat after breakfast, drive to Fort Kochi. Visit Chinese fishing nets, Jewish Synagogue, and Mattancherry Palace.\nDay 6: Morning Kochi harbor walk and departure transfer to Cochin International Airport.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Royal Rajasthan Heritage: Jaipur, Jodhpur & Udaipur",
                "6N/7D royal journey through majestic desert forts, grand palaces, and shimmering lake residences with royal heritage hospitality.",
                "Rajasthan", "JAI", "DELHI", "DOMESTIC", "4_STAR", "BREAKFAST", "CAR",
                "26999", "35999", 6, 7, 4.6,
                "Amber Fort,Mehrangarh Fort,Lake Pichola Boat Cruise,City Palace",
                "Dedicated AC Innova,Heritage Hotel stays,Daily breakfast,Monuments guided tour",
                "Camera fees,Lunches and dinners,Travel insurance",
                "https://images.unsplash.com/photo-1477587458883-47145ed94245?w=800",
                "Day 1: Arrive in Jaipur, check-in to heritage haveli, evening photo stop at Hawa Mahal and Johari Bazaar.\nDay 2: Full day Amber Fort with elephant/jeep ride, Jal Mahal, City Palace, and Jantar Mantar.\nDay 3: Drive to Jodhpur. Visit the towering Mehrangarh Fort, Jaswant Thada, and stroll through the Blue City lanes.\nDay 4: Visit Umaid Bhawan Palace museum, then drive to Udaipur via the intricate marble Ranakpur Jain Temples.\nDay 5: Explore Udaipur City Palace, Jagdish Temple, Saheliyon-ki-Bari, and a private sunset boat cruise on Lake Pichola.\nDay 6: Excursion to Monsoon Palace (Sajjangarh) and Bagore Ki Haveli folk dance show in the evening.\nDay 7: Breakfast at lakeside cafe and airport drop at Udaipur Maharana Pratap Airport.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Kashmir Paradise: Srinagar Houseboat, Gulmarg & Pahalgam",
                "5N/6D breathtaking Himalayan escape featuring luxury Dal Lake shikara rides, Gulmarg gondola excursions, and lush Betaab Valley meadows.",
                "Kashmir", "SXR", "DELHI", "DOMESTIC", "4_STAR", "HALF_BOARD", "FLIGHT",
                "24999", "32999", 5, 6, 4.9,
                "Dal Lake Shikara,Gulmarg Gondola Phase 2,Betaab Valley,Mughal Gardens",
                "Srinagar airport transfers,Dal Lake luxury houseboat stay,Pahalgam & Gulmarg hotel stays,Breakfast & Dinner",
                "Gondola Phase 2 ticket,Pony rides,Personal snow gear rentals",
                "https://images.unsplash.com/photo-1597074866923-dc0589150a32?w=800",
                "Day 1: Arrive Srinagar, transfer to intricately carved Dal Lake wooden houseboat. Evening romantic shikara sunset ride.\nDay 2: Day excursion to Gulmarg. Ride the world's highest cable car (Gondola) to Apharwat Peak, snow activities, pine forests.\nDay 3: Drive to Pahalgam (Valley of Shepherds) via saffron fields of Pampore and Awantipora ruins. Check-in to riverside resort.\nDay 4: Explore Aru Valley, Betaab Valley, and Chandanwari along the Lidder River.\nDay 5: Return to Srinagar. Visit famous Mughal Gardens: Nishat Bagh, Shalimar Bagh, and Shankaracharya Temple.\nDay 6: Morning floating vegetable market visit on Dal Lake, breakfast, and airport drop.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Himachal Wonders: Manali, Solang Valley & Rohtang Pass",
                "5N/6D alpine holiday with paragliding, river rafting, snow adventures at Rohtang, and pine cedar forest walks in Kullu Valley.",
                "Manali", "KUU", "DELHI", "DOMESTIC", "3_STAR", "HALF_BOARD", "CAR",
                "16999", "22999", 5, 6, 4.5,
                "Solang Valley Adventures,Rohtang Pass Snow Point,Hadimba Temple,River Rafting",
                "Delhi-Manali Volvo transfers / private car,Hotel accommodation,Breakfast & Dinner,Sightseeing tours",
                "Rohtang NGT permit if applicable,Paragliding/Zipline charges",
                "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=800",
                "Day 1: Arrival in Manali, check-in, rest, afternoon visit to 450-year-old Hadimba Devi Temple, Vashisht hot springs.\nDay 2: Solang Valley adventure day: zorbing, quad biking, and tandem paragliding overlooking snow-capped peaks.\nDay 3: Excursion to Rohtang Pass (subject to weather/permit) or Atal Tunnel and Sissu waterfall in Lahaul Valley.\nDay 4: White water river rafting in Beas River at Kullu, visit world-famous Kullu Shawl weaving centers, evening at Mall Road.\nDay 5: Trek to Jogini Waterfalls through apple orchards and pine groves, visit Old Manali cafes.\nDay 6: Leisure breakfast, souvenir shopping, and departure transfer.",
                false
            ),
            new SeedConstants.HolidayDef(
                "Andaman Island Odyssey: Havelock Beach & Scuba Haven",
                "5N/6D pristine tropical island retreat. Visit Cellular Jail, world-ranked Radhanagar Beach, and snorkel with sea turtles at Elephant Beach.",
                "Andaman", "IXZ", "CHENNAI", "DOMESTIC", "4_STAR", "BREAKFAST", "FLIGHT",
                "28499", "38999", 5, 6, 4.8,
                "Radhanagar Beach Sunset,Elephant Beach Snorkeling,Cellular Jail Light Show,Makruzz Cruise",
                "Airport pickup,Inter-island luxury catamaran cruise (Makruzz),Beach resort stays,Daily breakfast,Sightseeing permits",
                "Scuba diving certification,Water sports fees,Lunches and dinners",
                "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800",
                "Day 1: Arrive Port Blair, transfer to hotel. Afternoon visit to historic Cellular Jail and evening Sound & Light patriotic show.\nDay 2: Board high-speed luxury catamaran Makruzz to Havelock Island (Swaraj Dweep). Check-in to beachside villa. Sunset at Radhanagar Beach (Asia's #7).\nDay 3: Speedboat to Elephant Beach for complimentary guided snorkeling on living coral reefs and optional underwater sea walk.\nDay 4: Catamaran cruise to Neil Island (Shaheed Dweep). Visit natural rock formation (Howrah Bridge) and Laxmanpur white sand sunset beach.\nDay 5: Morning glass-bottom boat tour at Bharatpur Beach, return catamaran to Port Blair, shopping at Sagarika Emporium.\nDay 6: Breakfast and transfer to Port Blair airport.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Dubai Extravaganza: Burj Khalifa, Desert Safari & Marina",
                "4N/5D glamorous city break featuring 124th-floor Burj Khalifa views, 4x4 dune bashing desert safari with BBQ, and Dubai Marina luxury yacht cruise.",
                "Dubai", "DXB", "MUMBAI", "INTERNATIONAL", "5_STAR", "BREAKFAST", "FLIGHT",
                "42999", "58999", 4, 5, 4.9,
                "Burj Khalifa At The Top,Desert Safari with BBQ Dinner,Dubai Frame,Dubai Mall Fountain",
                "Return airport transfers,5-star hotel in Downtown/Marina,Daily buffet breakfast,Burj Khalifa tickets,Desert safari,Dhow cruise",
                "UAE tourist visa fee,Tourism Dirham hotel tax,Personal shopping",
                "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800",
                "Day 1: Arrive Dubai International Airport, private luxury transfer to hotel. Evening 2-hour Marina Dhow cruise with international buffet dinner.\nDay 2: Half-day Dubai city tour: Gold Souk, Spice Souk, Jumeirah Mosque, Burj Al Arab photo stop, Dubai Frame. Afternoon at Dubai Mall & Burj Khalifa 124th floor.\nDay 3: Morning at leisure for shopping. Afternoon 4x4 Land Cruiser desert safari: dune bashing, sandboarding, camel ride, belly dance, and BBQ dinner under stars.\nDay 4: Visit Museum of the Future and Atlantis The Palm Aquaventure Waterpark.\nDay 5: Morning at Dubai Miracle Garden (seasonal) or Souk Madinat, hotel check-out, and airport drop.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Singapore & Sentosa Island Fantasy",
                "4N/5D futuristic city escapade: Universal Studios Singapore, Gardens by the Bay Flower Dome & Cloud Forest, Night Safari, and Marina Bay Sands.",
                "Singapore", "SIN", "BANGALORE", "INTERNATIONAL", "4_STAR", "BREAKFAST", "FLIGHT",
                "52999", "69999", 4, 5, 4.8,
                "Universal Studios Full Day,Gardens by the Bay Supertrees,Sentosa Cable Car,Night Safari",
                "Changi airport transfers,4-star hotel in city center,Daily breakfast,Universal Studios ticket,Sentosa cable car,All entry permits",
                "Singapore visa fee,Meals not mentioned,Personal purchases",
                "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=800",
                "Day 1: Arrive Singapore Changi Airport (visit Jewel waterfall). Transfer to hotel. Evening tram safari through world's first Night Safari.\nDay 2: Half-day city tour (Merlion Park, Chinatown, Little India). Afternoon and evening at Gardens by the Bay with Spectra light & water show.\nDay 3: Scenic cable car to Sentosa Island. Full day pass at Universal Studios Singapore: Battlestar Galactica, Transformers 3D, and Jurassic Park.\nDay 4: Madame Tussauds, S.E.A. Aquarium, Siloso Beach walk, and evening Wings of Time laser show.\nDay 5: Shopping at Orchard Road or Bugis Street, check-out, and airport transfer.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Bali Tropical Dream: Ubud Rice Terraces & Uluwatu Sunset",
                "5N/6D exotic Indonesian holiday. Explore Ubud cultural arts, Bali jungle swings, sacred monkey forest, and Uluwatu cliff temple Kecak dance.",
                "Bali", "DPS", "DELHI", "INTERNATIONAL", "4_STAR", "BREAKFAST", "FLIGHT",
                "46999", "62999", 5, 6, 4.8,
                "Tegallalang Rice Terraces,Uluwatu Sunset Kecak Dance,Tanah Lot Sea Temple,Balinese Spa",
                "Airport transfers,Ubud jungle pool resort + Seminyak beach resort,Daily breakfast,2-hour authentic Balinese massage,Private tours",
                "Visa on Arrival (VOA),Water sports,Lunch & dinner",
                "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800",
                "Day 1: Arrive Ngurah Rai International Airport Denpasar, private flower lei welcome, transfer to Ubud forest resort.\nDay 2: Tegallalang Rice Terraces, Bali Jungle Swing, Sacred Monkey Forest Sanctuary, and traditional coffee tasting at luwak plantation.\nDay 3: Kintamani volcano panorama view of Mount Batur, Tirta Empul holy spring temple water blessing, transfer to Seminyak coastal resort.\nDay 4: Dramatic Tanah Lot Sea Temple, afternoon leisure on Seminyak beach, sunset cocktails at Potato Head Beach Club.\nDay 5: South Bali tour: Tanjung Benoa water sports (banana boat, parasailing), Padang Padang surf beach, and cliffside Uluwatu temple Kecak fire dance.\nDay 6: 2-hour Balinese herbal spa treatment, souvenir shopping at Krisna Oleh Oleh, and airport drop.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Maldives Luxury Overwater Villa Retreat",
                "4N/5D unforgettable luxury overwater bungalow experience in the Indian Ocean. Transparent turquoise lagoon, private plunge pool, and coral reef snorkeling.",
                "Maldives", "MLE", "MUMBAI", "INTERNATIONAL", "5_STAR", "ALL_INCLUSIVE", "FLIGHT",
                "88999", "119999", 4, 5, 4.9,
                "Overwater Bungalow with Plunge Pool,All-Inclusive Gourmet Dining,Coral Reef Snorkeling,Sunset Dolphin Cruise",
                "Return seaplane / speedboat transfers from Male,Overwater Villa,Breakfast, Lunch, Dinner & Premium Beverages,Snorkeling gear",
                "Green tax included,Spa salon treatments,Motorized water sports",
                "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=800",
                "Day 1: Arrive Male International Airport, scenic seaplane transfer over coral atolls directly to private island resort. Check-in to Overwater Villa.\nDay 2: Morning house reef snorkeling with reef sharks and sea turtles. Afternoon pool lounging, evening sunset dolphin watching cruise.\nDay 3: Water sports day: stand-up paddleboarding, kayak in crystal lagoon. Candlelight 5-course beachfront seafood dinner.\nDay 4: Indulge in overwater couple's spa treatment, private sandbank picnic lunch, night reef glow snorkeling.\nDay 5: Floating champagne breakfast in private villa plunge pool, seaplane transfer back to Male airport for flight home.",
                true
            ),
            new SeedConstants.HolidayDef(
                "Thailand Dual Discovery: Bangkok Temples & Phuket Beaches",
                "5N/6D popular twin-city getaway. Grand Palace and floating markets in Bangkok, combined with Phi Phi Islands speedboat tour in Phuket.",
                "Thailand", "BKK", "DELHI", "INTERNATIONAL", "3_STAR", "BREAKFAST", "FLIGHT",
                "32999", "44999", 5, 6, 4.6,
                "Phi Phi Islands Speedboat,Wat Arun Temple,Chao Phraya Dinner Cruise,Patong Nightlife",
                "Bangkok & Phuket airport transfers,Domestic flight BKK-HKT,Hotel stays,Daily breakfast,Full-day Phi Phi Island tour with lunch",
                "Thailand visa fee,National Park entry fees (400 THB),Optional water sports",
                "https://images.unsplash.com/photo-1528181304800-259b08848526?w=800",
                "Day 1: Arrive Bangkok Suvarnabhumi Airport, hotel transfer. Evening Chao Phraya Princess luxury dinner cruise with live band.\nDay 2: Bangkok cultural tour: Wat Phra Kaew (Emerald Buddha), Grand Palace, Wat Arun (Temple of Dawn), and Gems Gallery.\nDay 3: Flight to Phuket. Transfer to Patong beach resort. Evening stroll through bustling Bangla Road.\nDay 4: Full-day speedboat excursion to Phi Phi Islands: Maya Bay (The Beach movie site), Viking Cave, snorkeling at Monkey Beach, buffet lunch.\nDay 5: Phuket city tour: Big Buddha hilltop viewpoint, Wat Chalong, Promthep Cape sunset point, cashew nut factory.\nDay 6: Breakfast, duty-free shopping, and transfer to Phuket International Airport.",
                false
            ),
            new SeedConstants.HolidayDef(
                "European Grandeur: Paris, Swiss Alps & Amsterdam",
                "7N/8D magnificent journey across Europe's dream destinations. Eiffel Tower summit, Mount Titlis snow rotating cable car, and Amsterdam canal cruise.",
                "Europe", "CDG", "DELHI", "INTERNATIONAL", "4_STAR", "BREAKFAST", "FLIGHT",
                "134999", "179999", 7, 8, 4.9,
                "Eiffel Tower 2nd Level,Mount Titlis Rotair Cable Car,Amsterdam Canal Cruise,Lucerne Lake",
                "Intercity high-speed Eurostar/TGV trains,4-star hotels,Daily continental breakfast,Guided sightseeing tours,Eiffel Tower entry",
                "Schengen visa fees,Mandatory overseas travel insurance,City tourist taxes,Meals not mentioned",
                "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800",
                "Day 1: Arrive Paris Charles de Gaulle Airport, transfer to hotel. Evening Seine River sightseeing illumination cruise.\nDay 2: Paris city tour: Champs-Elysees, Arc de Triomphe, Louvre exterior, Notre-Dame, and Eiffel Tower 2nd level summit access.\nDay 3: Scenic high-speed TGV train across French countryside into Switzerland. Check-in to alpine hotel in Engelberg/Lucerne.\nDay 4: Mount Titlis excursion: ride the world's first revolving cable car (Rotair) to 10,000 feet, Glacier Cave, Cliff Walk suspension bridge.\nDay 5: Lucerne walking tour: Chapel Bridge, Lion Monument, Lake Lucerne cruise, and Swiss chocolate workshop.\nDay 6: High-speed ICE train to Amsterdam. Evening glass-topped canal cruise through historic 17th-century waterways.\nDay 7: Zaanse Schans windmill village, traditional Dutch cheese and clog factory demonstration, Dam Square, Rijksmuseum.\nDay 8: Breakfast and transfer to Amsterdam Schiphol Airport.",
                true
            )
        );

        List<HolidayPackage> toSave = new ArrayList<>();
        for (SeedConstants.HolidayDef p : packages) {
            if (existingTitles.contains(p.title())) {
                continue;
            }
            HolidayPackage h = new HolidayPackage();
            h.setTitle(p.title());
            h.setDescription(p.desc());
            h.setDestination(p.dest());
            h.setDestinationCode(p.code());
            h.setDepartureCity(p.depCity());
            h.setTripType(p.tripType());
            h.setHotelCategory(p.hotelCat());
            h.setMealPlan(p.mealPlan());
            h.setTransportType(p.transport());
            h.setPricePerPerson(new BigDecimal(p.price()));
            h.setOriginalPrice(new BigDecimal(p.origPrice()));
            h.setDurationNights(p.nights());
            h.setDurationDays(p.days());
            h.setRating(p.rating());
            h.setReviewCount(120 + (int)(p.rating() * 40));
            h.setHighlights(p.highlights());
            h.setInclusions(p.inclusions());
            h.setExclusions(p.exclusions());
            h.setMaxGroupSize(20);
            h.setActive(true);
            h.setFeatured(p.featured());
            h.setImageUrl(p.imgUrl());
            h.setItinerary(p.itinerary());

            toSave.add(h);
        }

        holidayRepo.saveAll(toSave);
        log.info("Total holiday packages persisted in database: {}", holidayRepo.count());
    }
}
