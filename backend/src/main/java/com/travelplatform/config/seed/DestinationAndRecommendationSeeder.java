package com.travelplatform.config.seed;

import com.travelplatform.entity.Destination;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserInteraction;
import com.travelplatform.repository.DestinationRepository;
import com.travelplatform.repository.UserInteractionRepository;
import com.travelplatform.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DestinationAndRecommendationSeeder {

    private static final Logger log = LoggerFactory.getLogger(DestinationAndRecommendationSeeder.class);

    private final DestinationRepository destinationRepo;
    private final UserInteractionRepository interactionRepo;
    private final RecommendationService recommendationService;

    public DestinationAndRecommendationSeeder(DestinationRepository destinationRepo,
                                             UserInteractionRepository interactionRepo,
                                             RecommendationService recommendationService) {
        this.destinationRepo = destinationRepo;
        this.interactionRepo = interactionRepo;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public List<Destination> seedDestinations() {
        if (destinationRepo.count() >= 15) {
            log.info("Destinations already seeded ({}), skipping.", destinationRepo.count());
            return destinationRepo.findAll();
        }

        log.info("Seeding rich Destination taxonomy catalog for personalized travel discovery...");

        record DestSpec(String name, String city, String country, String category, String tags,
                        String climate, String season, BigDecimal budget, Double popularity, String image, String desc) {}

        List<DestSpec> specs = List.of(
            new DestSpec("Goa", "Goa", "India", "BEACH", "BEACH,NIGHTLIFE,RELAXATION,WATERSPORTS,SEAFOOD",
                "Tropical", "November to February", BigDecimal.valueOf(3500), 0.98,
                "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800",
                "Golden sun-kissed beaches, swaying palms, vibrant beach shacks, and colonial Portuguese architecture."),

            new DestSpec("Bali", "Denpasar", "Indonesia", "BEACH", "BEACH,ISLAND,RELAXATION,SPIRITUAL,NATURE,SURFING",
                "Tropical", "April to October", BigDecimal.valueOf(5500), 0.97,
                "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800",
                "Iconic island paradise with lush terraced rice paddies, sacred volcanic temples, and world-class surf breaks."),

            new DestSpec("Maldives", "Male", "Maldives", "LUXURY", "BEACH,ISLAND,LUXURY,ROMANTIC,SNORKELING,RESORT",
                "Tropical", "December to April", BigDecimal.valueOf(18000), 0.99,
                "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=800",
                "Ultra-luxury private island atolls with crystal-clear turquoise lagoons and overwater luxury villas."),

            new DestSpec("Dubai", "Dubai", "UAE", "CITY", "LUXURY,SHOPPING,CITY,FAMILY,DESERT_SAFARI,MODERN",
                "Desert", "November to March", BigDecimal.valueOf(12000), 0.96,
                "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800",
                "Ultramodern metropolis featuring awe-inspiring skyscrapers, luxury shopping mega-malls, and golden desert dunes."),

            new DestSpec("Manali", "Manali", "India", "MOUNTAIN", "MOUNTAIN,ADVENTURE,SNOW,TREKKING,NATURE",
                "Alpine", "March to June & Dec to Feb", BigDecimal.valueOf(3000), 0.95,
                "https://images.unsplash.com/photo-1605649487212-47bdab064df7?w=800",
                "Nestled in the Pir Panjal mountains, renowned for pine forests, Solang Valley snow sports, and roaring river rapids."),

            new DestSpec("Srinagar", "Srinagar", "India", "MOUNTAIN", "MOUNTAIN,LAKE,ROMANTIC,NATURE,HERITAGE",
                "Subtropical Highland", "April to October", BigDecimal.valueOf(4200), 0.94,
                "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=800",
                "Paradise on Earth featuring historic wooden houseboats on Dal Lake, Mughal gardens, and snow-capped Himalayan peaks."),

            new DestSpec("Jaipur", "Jaipur", "India", "HERITAGE", "HERITAGE,CULTURE,PALACES,SHOPPING,FORTS,ROYAL",
                "Semi-arid", "October to March", BigDecimal.valueOf(3800), 0.95,
                "https://images.unsplash.com/photo-1599661046289-e31897846e41?w=800",
                "The legendary Pink City showcasing grand hilltop fortresses, opulent royal palaces, and vibrant bazaars."),

            new DestSpec("Paris", "Paris", "France", "HERITAGE", "CULTURE,ROMANTIC,CITY,LUXURY,ART,GASTRONOMY",
                "Oceanic", "June to August & Sep to Oct", BigDecimal.valueOf(16000), 0.97,
                "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800",
                "The City of Light, famous for the Eiffel Tower, the Louvre museum, haute couture, and romantic Seine river walks."),

            new DestSpec("Singapore", "Singapore", "Singapore", "CITY", "CITY,FAMILY,FUTURISTIC,GARDENS,SHOPPING,FOOD",
                "Tropical Rainforest", "Year-round", BigDecimal.valueOf(11000), 0.96,
                "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=800",
                "Futuristic island city-state with Gardens by the Bay, Marina Bay Sands, diverse culinary hawker centers, and theme parks."),

            new DestSpec("Phuket", "Phuket", "Thailand", "BEACH", "BEACH,ISLAND,NIGHTLIFE,WATERSPORTS,BUDGET,DIVING",
                "Tropical", "November to April", BigDecimal.valueOf(4500), 0.94,
                "https://images.unsplash.com/photo-1589394815804-964ed0be2eb5?w=800",
                "Thailand's premier island holiday spot, featuring Phang Nga Bay karsts, Patong beach nightlife, and coral dive sites."),

            new DestSpec("Udaipur", "Udaipur", "India", "HERITAGE", "HERITAGE,ROMANTIC,LAKE,PALACES,LUXURY,ROYAL",
                "Semi-arid", "September to March", BigDecimal.valueOf(5000), 0.93,
                "https://images.unsplash.com/photo-1597040663342-45b6af3d91a5?w=800",
                "The City of Lakes with romantic marble palaces reflected upon serene Lake Pichola and Aravali hill horizons."),

            new DestSpec("Swiss Alps", "Interlaken", "Switzerland", "MOUNTAIN", "MOUNTAIN,SNOW,LUXURY,SCENIC_TRAINS,SKIING",
                "Alpine", "December to April & Jul to Aug", BigDecimal.valueOf(22000), 0.98,
                "https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?w=800",
                "Breathtaking mountain passes, Jungfraujoch ice glaciers, pristine alpine lakes, and world-class ski slopes."),

            new DestSpec("Tokyo", "Tokyo", "Japan", "CITY", "CITY,CULTURE,FOOD,ANIME,TECHNOLOGY,SHOPPING",
                "Humid Subtropical", "March to May & Sep to Nov", BigDecimal.valueOf(14000), 0.96,
                "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800",
                "Vibrant metropolis where ancient Shinto shrines blend seamlessly with neon-lit futuristic skyscrapers and sushi bars."),

            new DestSpec("London", "London", "UK", "CITY", "CITY,HERITAGE,THEATRE,SHOPPING,MUSEUMS,PARKS",
                "Oceanic", "May to September", BigDecimal.valueOf(15000), 0.95,
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800",
                "Historic global capital featuring Big Ben, the Tower of London, West End musicals, and royal parks."),

            new DestSpec("Kerala", "Kochi", "India", "NATURE", "NATURE,BACKWATERS,AYURVEDA,BEACH,RELAXATION,CULTURE",
                "Tropical", "September to March", BigDecimal.valueOf(3800), 0.94,
                "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800",
                "God's Own Country, celebrated for serene palm-fringed backwater houseboats, spice plantations, and Ayurvedic rejuvenation.")
        );

        List<Destination> saved = new ArrayList<>();
        for (DestSpec spec : specs) {
            Optional<Destination> existing = destinationRepo.findByNameIgnoreCase(spec.name());
            if (existing.isEmpty()) {
                Destination d = new Destination(
                    spec.name(), spec.city(), spec.country(), spec.category(), spec.tags(),
                    spec.climate(), spec.season(), spec.budget(), spec.popularity(), spec.image(), spec.desc()
                );
                saved.add(destinationRepo.save(d));
            } else {
                saved.add(existing.get());
            }
        }

        log.info("Total destinations in catalog: {}", destinationRepo.count());
        return saved;
    }

    /**
     * Seeds realistic behavioral interactions across test users so that
     * Content-Based and Collaborative Filtering algorithms have rich overlapping patterns.
     */
    @Transactional
    public void seedUserInteractionsAndInitialRecs(Map<String, User> userMap) {
        log.info("Seeding recommendation signals and computing initial recommendations...");

        User user = userMap.get("user@travel.com");
        User traveler = userMap.get("traveler@travel.com");
        User reviewer = userMap.get("reviewer@travel.com");

        if (user != null && interactionRepo.findByUserId(user.getId()).isEmpty()) {
            List<UserInteraction> interactions = new ArrayList<>();

            // User A (Beach Lover)
            interactions.add(new UserInteraction(user, "DESTINATION", 1L, "SEARCHED")); // Goa
            interactions.get(0).setTags("beach,relaxation,sun");

            interactions.add(new UserInteraction(user, "DESTINATION", 2L, "VIEWED")); // Bali
            interactions.get(1).setTags("beach,island,tropical");

            interactions.add(new UserInteraction(user, "DESTINATION", 3L, "SAVED")); // Maldives
            interactions.get(2).setTags("beach,luxury,island");

            interactions.add(new UserInteraction(user, "HOTEL", 1L, "BOOKED"));
            interactions.get(3).setRating(5);
            interactions.get(3).setTags("beach,pool,luxury");

            interactionRepo.saveAll(interactions);
        }

        // Trigger recommendation computation for test users
        if (user != null) {
            recommendationService.computeAndPersistForUser(user.getId(), "seed_cycle");
        }
        if (traveler != null) {
            recommendationService.computeAndPersistForUser(traveler.getId(), "seed_cycle");
        }
        if (reviewer != null) {
            recommendationService.computeAndPersistForUser(reviewer.getId(), "seed_cycle");
        }

        log.info("Recommendations initialized for test users.");
    }
}
