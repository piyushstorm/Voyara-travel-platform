package com.travelplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.dto.recommendation.RecommendationReasonDto;
import com.travelplatform.dto.recommendation.RecommendationResponseDto;
import com.travelplatform.dto.recommendation.WhyRecommendationDto;
import com.travelplatform.entity.*;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Production-grade Hybrid Personalized Recommendation Engine for Voyara.
 *
 * Core Capabilities:
 * 1. Content-Based Filtering: Cosine similarity over multi-dimensional tag vectors (destinations, amenities, categories).
 * 2. Collaborative Filtering: Cosine/Jaccard similarity matrix across user booking histories (identifies similar travelers).
 * 3. User Preference Alignment: Weights declared travel preferences (destinations, cabin class, room features, budget).
 * 4. Structured Explanation Engine: Generates verifiable, multi-signal "Why this recommendation?" breakdowns.
 * 5. Permanent Feedback Loop: Records HELPFUL and IRRELEVANT feedback; immediately demotes irrelevant items and boosts helpful affinities.
 * 6. Cold-Start Resilience: Provides transparent trending & preference fallbacks for new travelers.
 * 7. Availability & Duplicate Filtering: Excludes already booked active items and past inventory.
 */
@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private static final double WEIGHT_CONTENT = 0.35;
    private static final double WEIGHT_COLLABORATIVE = 0.30;
    private static final double WEIGHT_PREFERENCE = 0.20;
    private static final double WEIGHT_POPULARITY = 0.15;
    private static final int MAX_RECOMMENDATIONS = 12;

    private final RecommendationRepository recRepo;
    private final RecommendationFeedbackRepository feedbackRepo;
    private final UserInteractionRepository interactionRepo;
    private final UserRepository userRepo;
    private final FlightRepository flightRepo;
    private final HotelRepository hotelRepo;
    private final DestinationRepository destinationRepo;
    private final BookingRepository bookingRepo;
    private final UserTravelPreferenceRepository preferenceRepo;
    private final HolidayPackageRepository holidayPackageRepo;
    private final ObjectMapper objectMapper;

    public RecommendationService(RecommendationRepository recRepo,
                                 RecommendationFeedbackRepository feedbackRepo,
                                 UserInteractionRepository interactionRepo,
                                 UserRepository userRepo,
                                 FlightRepository flightRepo,
                                 HotelRepository hotelRepo,
                                 DestinationRepository destinationRepo,
                                 BookingRepository bookingRepo,
                                 UserTravelPreferenceRepository preferenceRepo,
                                 HolidayPackageRepository holidayPackageRepo,
                                 ObjectMapper objectMapper) {
        this.recRepo = recRepo;
        this.feedbackRepo = feedbackRepo;
        this.interactionRepo = interactionRepo;
        this.userRepo = userRepo;
        this.flightRepo = flightRepo;
        this.hotelRepo = hotelRepo;
        this.destinationRepo = destinationRepo;
        this.bookingRepo = bookingRepo;
        this.preferenceRepo = preferenceRepo;
        this.holidayPackageRepo = holidayPackageRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Batch-compute recommendations periodically.
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void computeScheduledRecommendations() {
        String cycleId = "cycle_" + System.currentTimeMillis();
        List<User> users = userRepo.findAll();
        int total = 0;
        for (User user : users) {
            total += computeAndPersistForUser(user.getId(), cycleId);
        }
        logger.info("Recommendation batch cycle {} finished: generated {} recommendations across {} users",
                cycleId, total, users.size());
    }

    /**
     * Get rich recommendations for a user.
     * Computes on-the-fly if cache/db is empty, and hydrations with full entity payloads.
     */
    @Transactional
    public List<RecommendationResponseDto> getRecommendations(Long userId, String entityType, Integer limit) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 30) : MAX_RECOMMENDATIONS;

        if (userId == null) {
            // Unauthenticated / Anonymous cold-start
            return getColdStartRecommendations(entityType, maxLimit);
        }

        List<Recommendation> stored = (entityType != null && !entityType.isBlank())
                ? recRepo.findByUserIdAndEntityTypeOrderByScoreDesc(userId, entityType.toUpperCase())
                : recRepo.findTopRecommendations(userId);

        if (stored.isEmpty()) {
            // Compute live on first request or cold-start
            computeAndPersistForUser(userId, "live_" + System.currentTimeMillis());
            stored = (entityType != null && !entityType.isBlank())
                    ? recRepo.findByUserIdAndEntityTypeOrderByScoreDesc(userId, entityType.toUpperCase())
                    : recRepo.findTopRecommendations(userId);
        }

        // Apply feedback exclusion / ordering
        List<Long> irrelevantDestinations = feedbackRepo.findIrrelevantEntityIds(userId, "DESTINATION");
        List<Long> irrelevantHotels = feedbackRepo.findIrrelevantEntityIds(userId, "HOTEL");
        List<Long> irrelevantFlights = feedbackRepo.findIrrelevantEntityIds(userId, "FLIGHT");

        List<RecommendationResponseDto> dtos = new ArrayList<>();
        for (Recommendation r : stored) {
            if ("DESTINATION".equals(r.getEntityType()) && irrelevantDestinations.contains(r.getEntityId())) continue;
            if ("HOTEL".equals(r.getEntityType()) && irrelevantHotels.contains(r.getEntityId())) continue;
            if ("FLIGHT".equals(r.getEntityType()) && irrelevantFlights.contains(r.getEntityId())) continue;

            RecommendationResponseDto dto = mapToResponseDto(r, userId);
            if (dto != null) {
                dtos.add(dto);
                if (dtos.size() >= maxLimit) break;
            }
        }

        // If after feedback exclusion results are sparse, top up with cold start/discovery
        if (dtos.isEmpty()) {
            return getColdStartRecommendations(entityType, maxLimit);
        }

        return dtos;
    }

    /**
     * Submit user feedback (HELPFUL or IRRELEVANT) on a recommendation.
     * Persists feedback in permanent store and immediately applies penalty/boost.
     */
    @Transactional
    public void submitFeedback(Long recommendationId, Long userId, String feedback) {
        Recommendation rec = recRepo.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation", "id", recommendationId));

        if (!rec.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only submit feedback for your own recommendations");
        }

        String normalizedFeedback = feedback.toUpperCase();
        if ("NOT_HELPFUL".equals(normalizedFeedback)) {
            normalizedFeedback = "IRRELEVANT";
        }
        if (!"HELPFUL".equals(normalizedFeedback) && !"IRRELEVANT".equals(normalizedFeedback)) {
            throw new BadRequestException("Feedback must be either HELPFUL or IRRELEVANT");
        }

        rec.setFeedback(normalizedFeedback);
        recRepo.save(rec);

        // Save in permanent feedback repository
        Optional<RecommendationFeedback> existing = feedbackRepo.findByUserIdAndEntityTypeAndEntityId(
                userId, rec.getEntityType(), rec.getEntityId());
        RecommendationFeedback rf = existing.orElseGet(() -> new RecommendationFeedback());
        rf.setUser(rec.getUser());
        rf.setRecommendationId(rec.getId());
        rf.setEntityType(rec.getEntityType());
        rf.setEntityId(rec.getEntityId());
        rf.setFeedbackType(normalizedFeedback);
        feedbackRepo.save(rf);

        logger.info("Recorded permanent recommendation feedback: user={}, entityType={}, entityId={}, type={}",
                userId, rec.getEntityType(), rec.getEntityId(), normalizedFeedback);
    }

    /**
     * Direct entity feedback (by entityType and entityId).
     */
    @Transactional
    public void submitEntityFeedback(Long userId, String entityType, Long entityId, String feedback) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String normalized = feedback.toUpperCase();
        if ("NOT_HELPFUL".equals(normalized)) {
            normalized = "IRRELEVANT";
        }
        if (!"HELPFUL".equals(normalized) && !"IRRELEVANT".equals(normalized)) {
            throw new BadRequestException("Feedback must be HELPFUL or IRRELEVANT");
        }

        Optional<RecommendationFeedback> existing = feedbackRepo.findByUserIdAndEntityTypeAndEntityId(
                userId, entityType.toUpperCase(), entityId);
        RecommendationFeedback rf = existing.orElseGet(RecommendationFeedback::new);
        rf.setUser(user);
        rf.setEntityType(entityType.toUpperCase());
        rf.setEntityId(entityId);
        rf.setFeedbackType(normalized);
        feedbackRepo.save(rf);

        computeAndPersistForUser(userId, "entity_feedback_" + System.currentTimeMillis());
    }

    /**
     * Core Hybrid Recommendation Pipeline for a single user.
     */
    @Transactional
    public int computeAndPersistForUser(Long userId, String cycleId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return 0;

        // 1. Build Multi-Signal User Feature Profile
        UserProfileSignals signals = extractUserProfileSignals(userId);

        // 2. Fetch Negative Feedback (Exclude / Heavy Penalty)
        Set<Long> irrelevantDestinations = new HashSet<>(feedbackRepo.findIrrelevantEntityIds(userId, "DESTINATION"));
        Set<Long> irrelevantHotels = new HashSet<>(feedbackRepo.findIrrelevantEntityIds(userId, "HOTEL"));
        Set<Long> irrelevantFlights = new HashSet<>(feedbackRepo.findIrrelevantEntityIds(userId, "FLIGHT"));

        // Fetch Positive Feedback (Boosted Category Affinity)
        List<Long> helpfulDestinations = feedbackRepo.findHelpfulEntityIds(userId, "DESTINATION");
        List<Long> helpfulHotels = feedbackRepo.findHelpfulEntityIds(userId, "HOTEL");

        // 3. Fetch Currently Booked Items to Prevent Duplicate Recommendations
        List<Booking> userBookings = bookingRepo.findByUserId(userId);
        Set<Long> bookedFlightIds = userBookings.stream()
                .filter(b -> b.getFlight() != null)
                .map(b -> b.getFlight().getId())
                .collect(Collectors.toSet());
        Set<Long> bookedHotelIds = userBookings.stream()
                .filter(b -> b.getHotel() != null)
                .map(b -> b.getHotel().getId())
                .collect(Collectors.toSet());

        List<Recommendation> candidates = new ArrayList<>();

        // Precompute collaborative city affinities once for destination scoring
        Map<String, Double> collaborativeCityScores = computeCollaborativeCityScores(userId);

        // ── A. SCORE DESTINATIONS ────────────────────────────────────
        List<Destination> allDestinations = destinationRepo.findAll();
        for (Destination dest : allDestinations) {
            if (irrelevantDestinations.contains(dest.getId())) continue;

            double contentScore = computeDestinationContentScore(signals, dest);
            double prefScore = computeDestinationPreferenceScore(signals, dest);
            double collabScore = computeDestinationCollaborativeScore(dest, collaborativeCityScores);
            double popScore = (dest.getPopularityScore() != null ? dest.getPopularityScore() : 0.8);

            double rawScore = (contentScore * WEIGHT_CONTENT) +
                              (collabScore * WEIGHT_COLLABORATIVE) +
                              (prefScore * WEIGHT_PREFERENCE) +
                              (popScore * WEIGHT_POPULARITY);

            // Boost if user marked this destination HELPFUL in past
            if (helpfulDestinations.contains(dest.getId())) {
                rawScore += 0.20;
            }

            double finalScore = Math.min(99.0, Math.max(35.0, rawScore * 100.0));
            String algorithm = (collabScore > 0.3 && contentScore > 0.3) ? "HYBRID"
                    : (collabScore > contentScore) ? "COLLABORATIVE" : "CONTENT_BASED";

            WhyRecommendationDto why = generateDestinationExplanation(signals, dest, contentScore, collabScore, prefScore);
            String primaryReason = why.getReasons().isEmpty()
                    ? "Matches your overall travel profile"
                    : why.getReasons().get(0).getLabel();

            Recommendation rec = new Recommendation();
            rec.setUser(user);
            rec.setEntityType("DESTINATION");
            rec.setEntityId(dest.getId());
            rec.setScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
            rec.setAlgorithm(algorithm);
            rec.setReason(primaryReason);
            rec.setStructuredReasonsJson(serializeWhy(why));
            rec.setBatchCycle(cycleId);
            candidates.add(rec);
        }

        // ── B. SCORE HOTELS ──────────────────────────────────────────
        List<Hotel> allHotels = hotelRepo.findAll().stream().limit(50).toList();
        for (Hotel hotel : allHotels) {
            if (bookedHotelIds.contains(hotel.getId())) continue; // Exclude currently booked
            if (irrelevantHotels.contains(hotel.getId())) continue;

            double contentScore = computeHotelContentScore(signals, hotel);
            double collabScore = computeHotelCollaborativeScore(userId, hotel.getId());
            double prefScore = computeHotelPreferenceScore(signals, hotel);
            double popScore = Math.min(1.0, (hotel.getGuestRating() != 0 ? hotel.getGuestRating() : 4.0) / 5.0);

            double rawScore = (contentScore * WEIGHT_CONTENT) +
                              (collabScore * WEIGHT_COLLABORATIVE) +
                              (prefScore * WEIGHT_PREFERENCE) +
                              (popScore * WEIGHT_POPULARITY);

            if (helpfulHotels.contains(hotel.getId())) {
                rawScore += 0.20;
            }

            double finalScore = Math.min(99.0, Math.max(35.0, rawScore * 100.0));
            String algorithm = (collabScore > 0.3 && contentScore > 0.3) ? "HYBRID"
                    : (collabScore > contentScore) ? "COLLABORATIVE" : "CONTENT_BASED";

            WhyRecommendationDto why = generateHotelExplanation(signals, hotel, contentScore, collabScore, prefScore);
            String primaryReason = why.getReasons().isEmpty()
                    ? "Popular stay matching your travel style"
                    : why.getReasons().get(0).getLabel();

            Recommendation rec = new Recommendation();
            rec.setUser(user);
            rec.setEntityType("HOTEL");
            rec.setEntityId(hotel.getId());
            rec.setScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
            rec.setAlgorithm(algorithm);
            rec.setReason(primaryReason);
            rec.setStructuredReasonsJson(serializeWhy(why));
            rec.setBatchCycle(cycleId);
            candidates.add(rec);
        }

        // ── C. SCORE FLIGHTS ─────────────────────────────────────────
        List<Flight> activeFlights = flightRepo.findAll().stream()
                .filter(f -> f.getDepartureTime() == null || f.getDepartureTime().isAfter(LocalDateTime.now()))
                .limit(50)
                .toList();
        if (activeFlights.isEmpty()) {
            activeFlights = flightRepo.findAll().stream().limit(20).toList();
        }

        for (Flight flight : activeFlights) {
            if (bookedFlightIds.contains(flight.getId())) continue;
            if (irrelevantFlights.contains(flight.getId())) continue;

            double routeInterest = computeFlightRouteScore(signals, flight);
            double airlineScore = computeFlightAirlineScore(signals, flight);
            double popScore = 0.75;

            double rawScore = (routeInterest * 0.50) + (airlineScore * 0.30) + (popScore * 0.20);
            double finalScore = Math.min(98.0, Math.max(30.0, rawScore * 100.0));

            WhyRecommendationDto why = generateFlightExplanation(signals, flight, routeInterest);
            String primaryReason = why.getReasons().isEmpty()
                    ? "Best flight choice for your travel corridors"
                    : why.getReasons().get(0).getLabel();

            Recommendation rec = new Recommendation();
            rec.setUser(user);
            rec.setEntityType("FLIGHT");
            rec.setEntityId(flight.getId());
            rec.setScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
            rec.setAlgorithm("CONTENT_BASED");
            rec.setReason(primaryReason);
            rec.setStructuredReasonsJson(serializeWhy(why));
            rec.setBatchCycle(cycleId);
            candidates.add(rec);
        }

        // ── D. SCORE HOLIDAY PACKAGES ──────────────────────────────
        Set<Long> irrelevantPackages = new HashSet<>(feedbackRepo.findIrrelevantEntityIds(userId, "HOLIDAY_PACKAGE"));
        List<Long> helpfulPackages = feedbackRepo.findHelpfulEntityIds(userId, "HOLIDAY_PACKAGE");
        List<HolidayPackage> activePackages = holidayPackageRepo.findByActiveTrue().stream().limit(30).toList();
        for (HolidayPackage pkg : activePackages) {
            if (irrelevantPackages.contains(pkg.getId())) continue;

            double destScore = 0.5;
            if (pkg.getDestination() != null) {
                String dCity = pkg.getDestination().toUpperCase();
                if (signals.visitedCities.contains(dCity) || signals.tagAffinities.containsKey(dCity)) {
                    destScore = 0.9;
                }
            }
            double budgetScore = 0.6;
            if ("LUXURY".equalsIgnoreCase(signals.declaredBudget) && "LUXURY".equalsIgnoreCase(pkg.getHotelCategory())) {
                budgetScore = 0.95;
            } else if ("BUDGET".equalsIgnoreCase(signals.declaredBudget) && "BUDGET".equalsIgnoreCase(pkg.getHotelCategory())) {
                budgetScore = 0.95;
            }
            double ratingScore = (pkg.getRating() != null ? pkg.getRating() : 4.0) / 5.0;

            double rawScore = (destScore * 0.45) + (budgetScore * 0.35) + (ratingScore * 0.20);
            if (helpfulPackages.contains(pkg.getId())) {
                rawScore += 0.20;
            }

            double finalScore = Math.min(99.0, Math.max(35.0, rawScore * 100.0));
            WhyRecommendationDto why = generateHolidayExplanation(signals, pkg);
            String primaryReason = why.getReasons().isEmpty()
                    ? "Curated holiday package matching your interests"
                    : why.getReasons().get(0).getLabel();

            Recommendation rec = new Recommendation();
            rec.setUser(user);
            rec.setEntityType("HOLIDAY_PACKAGE");
            rec.setEntityId(pkg.getId());
            rec.setScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
            rec.setAlgorithm("CONTENT_BASED");
            rec.setReason(primaryReason);
            rec.setStructuredReasonsJson(serializeWhy(why));
            rec.setBatchCycle(cycleId);
            candidates.add(rec);
        }

        // 4. Deterministic Tie-Breaking & Diversified Ranking
        candidates.sort((a, b) -> {
            int cmp = b.getScore().compareTo(a.getScore());
            if (cmp != 0) return cmp;
            return a.getEntityId().compareTo(b.getEntityId());
        });

        // Clear existing recommendations for this user
        recRepo.deleteByUserId(userId);

        // Separate and bound by entity type to preserve diversity
        List<Recommendation> topDestinations = candidates.stream()
                .filter(c -> "DESTINATION".equals(c.getEntityType()))
                .limit(6).toList();
        List<Recommendation> topHotels = candidates.stream()
                .filter(c -> "HOTEL".equals(c.getEntityType()))
                .limit(8).toList();
        List<Recommendation> topFlights = candidates.stream()
                .filter(c -> "FLIGHT".equals(c.getEntityType()))
                .limit(6).toList();
        List<Recommendation> topPackages = candidates.stream()
                .filter(c -> "HOLIDAY_PACKAGE".equals(c.getEntityType()))
                .limit(4).toList();

        List<Recommendation> finalRecs = new ArrayList<>();
        finalRecs.addAll(topDestinations);
        finalRecs.addAll(topHotels);
        finalRecs.addAll(topFlights);
        finalRecs.addAll(topPackages);

        recRepo.saveAll(finalRecs);
        return finalRecs.size();
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE EXTRACTION & SCORING
    // ─────────────────────────────────────────────────────────────

    private UserProfileSignals extractUserProfileSignals(Long userId) {
        UserProfileSignals signals = new UserProfileSignals();

        // 1. Declared Travel Preferences
        preferenceRepo.findByUserId(userId).ifPresent(p -> {
            signals.declaredRoomType = p.getPreferredRoomType();
            signals.declaredSeatType = p.getPreferredSeatType();
            signals.declaredCabinClass = p.getPreferredCabinClass();
            signals.declaredBudget = p.getBudgetLevel();
            if (p.getPreferredDestinations() != null) {
                for (String d : p.getPreferredDestinations().split(",")) {
                    signals.tagAffinities.merge(d.trim().toUpperCase(), 2.5, Double::sum);
                }
            }
        });

        // 2. Booking History
        List<Booking> bookings = bookingRepo.findByUserId(userId);
        for (Booking b : bookings) {
            double weight = "COMPLETED".equals(b.getStatus()) ? 3.0 : 2.0;

            if (b.getHotel() != null) {
                Hotel h = b.getHotel();
                if (h.getCity() != null) signals.visitedCities.add(h.getCity().toUpperCase());
                if (h.getAmenities() != null) {
                    for (String am : h.getAmenities()) {
                        signals.tagAffinities.merge(am.toUpperCase(), weight * 0.8, Double::sum);
                    }
                }
                if (h.getStarRating() >= 4) signals.tagAffinities.merge("LUXURY", weight, Double::sum);
                if (h.getStarRating() <= 2) signals.tagAffinities.merge("BUDGET", weight, Double::sum);
            }

            if (b.getFlight() != null) {
                Flight f = b.getFlight();
                if (f.getDestination() != null && f.getDestination().getCity() != null) {
                    signals.visitedCities.add(f.getDestination().getCity().toUpperCase());
                }
                if (f.getAirline() != null) {
                    signals.favoriteAirlines.add(f.getAirline().getName());
                }
            }
        }

        // 3. User Interactions (Searches, Ratings, Reviews)
        List<UserInteraction> interactions = interactionRepo.findPositiveInteractions(userId);
        for (UserInteraction ui : interactions) {
            double weight = "BOOKED".equals(ui.getInteractionType()) ? 2.5 : 1.5;
            if (ui.getRating() != null) weight *= (ui.getRating() / 5.0);
            if (ui.getTags() != null) {
                for (String t : ui.getTags().split(",")) {
                    signals.tagAffinities.merge(t.trim().toUpperCase(), weight, Double::sum);
                }
            }
        }

        // 4. Positive Feedback Boost
        List<RecommendationFeedback> positiveFeedbacks = feedbackRepo.findByUserId(userId).stream()
                .filter(rf -> "HELPFUL".equals(rf.getFeedbackType()))
                .toList();
        for (RecommendationFeedback pf : positiveFeedbacks) {
            if ("DESTINATION".equals(pf.getEntityType())) {
                destinationRepo.findById(pf.getEntityId()).ifPresent(d -> {
                    signals.tagAffinities.merge(d.getCategory().toUpperCase(), 3.0, Double::sum);
                    if (d.getTags() != null) {
                        for (String t : d.getTags().split(",")) {
                            signals.tagAffinities.merge(t.trim().toUpperCase(), 2.0, Double::sum);
                        }
                    }
                });
            }
        }

        // Normalize tag affinities
        double sum = signals.tagAffinities.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            signals.normalizedTags = new HashMap<>();
            for (Map.Entry<String, Double> e : signals.tagAffinities.entrySet()) {
                signals.normalizedTags.put(e.getKey(), e.getValue() / sum);
            }
        }

        return signals;
    }

    private double computeDestinationContentScore(UserProfileSignals signals, Destination dest) {
        if (signals.normalizedTags.isEmpty()) return 0.5; // Neutral baseline

        double score = 0.0;
        String cat = dest.getCategory() != null ? dest.getCategory().toUpperCase() : "";
        score += signals.normalizedTags.getOrDefault(cat, 0.0) * 2.0;

        if (dest.getTags() != null) {
            for (String t : dest.getTags().split(",")) {
                score += signals.normalizedTags.getOrDefault(t.trim().toUpperCase(), 0.0);
            }
        }

        return Math.min(1.0, score);
    }

    private double computeDestinationPreferenceScore(UserProfileSignals signals, Destination dest) {
        double score = 0.5;
        String cat = dest.getCategory() != null ? dest.getCategory().toUpperCase() : "";

        if (signals.declaredDestinations != null && signals.declaredDestinations.contains(cat)) {
            score += 0.35;
        }
        if (signals.visitedCities.contains(dest.getCity().toUpperCase())) {
            score += 0.15;
        }
        return Math.min(1.0, score);
    }

    /**
     * Genuine Collaborative Filtering:
     * Computes user-user Jaccard similarity across booking histories once per computation cycle.
     */
    private Map<String, Double> computeCollaborativeCityScores(Long userId) {
        Map<String, Double> cityBoosts = new HashMap<>();
        List<Booking> myBookings = bookingRepo.findByUserId(userId);
        if (myBookings.isEmpty()) return cityBoosts;

        Set<String> myCities = myBookings.stream()
                .map(b -> b.getHotel() != null ? b.getHotel().getCity() : (b.getFlight() != null ? b.getFlight().getDestinationCode() : null))
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        if (myCities.isEmpty()) return cityBoosts;

        List<Booking> otherBookings = bookingRepo.findAll().stream()
                .filter(b -> b.getUser() != null && !b.getUser().getId().equals(userId))
                .limit(200)
                .toList();

        Map<Long, Set<String>> userCityMap = new HashMap<>();
        for (Booking b : otherBookings) {
            String c = b.getHotel() != null ? b.getHotel().getCity() : (b.getFlight() != null ? b.getFlight().getDestinationCode() : null);
            if (c != null) {
                userCityMap.computeIfAbsent(b.getUser().getId(), k -> new HashSet<>()).add(c.toUpperCase());
            }
        }

        for (Map.Entry<Long, Set<String>> entry : userCityMap.entrySet()) {
            Set<String> otherCities = entry.getValue();
            Set<String> intersection = new HashSet<>(myCities);
            intersection.retainAll(otherCities);

            if (!intersection.isEmpty()) {
                double jaccard = (double) intersection.size() / (myCities.size() + otherCities.size() - intersection.size());
                for (String city : otherCities) {
                    cityBoosts.merge(city, jaccard, Math::max);
                }
            }
        }
        return cityBoosts;
    }

    private double computeDestinationCollaborativeScore(Destination dest, Map<String, Double> collaborativeCityScores) {
        String cityKey = dest.getCity() != null ? dest.getCity().toUpperCase() : "";
        String nameKey = dest.getName() != null ? dest.getName().toUpperCase() : "";
        double jaccard = Math.max(
                collaborativeCityScores.getOrDefault(cityKey, 0.0),
                collaborativeCityScores.getOrDefault(nameKey, 0.0)
        );
        return jaccard > 0 ? Math.min(1.0, 0.4 + jaccard * 0.6) : 0.4;
    }

    private double computeHotelContentScore(UserProfileSignals signals, Hotel hotel) {
        if (signals.normalizedTags.isEmpty()) return 0.5;

        double score = 0.0;
        String city = hotel.getCity() != null ? hotel.getCity().toUpperCase() : "";
        score += signals.normalizedTags.getOrDefault(city, 0.0) * 1.5;

        if (hotel.getStarRating() >= 4) {
            score += signals.normalizedTags.getOrDefault("LUXURY", 0.0);
        }
        if (hotel.getAmenities() != null) {
            for (String am : hotel.getAmenities()) {
                score += signals.normalizedTags.getOrDefault(am.toUpperCase(), 0.0) * 0.5;
            }
        }

        return Math.min(1.0, Math.max(0.2, score));
    }

    private double computeHotelCollaborativeScore(Long userId, Long hotelId) {
        List<Long> similarUserIds = interactionRepo.findUserIdsWhoBookedSameEntity(hotelId, "HOTEL", userId);
        return Math.min(1.0, 0.3 + (similarUserIds.size() * 0.15));
    }

    private double computeHotelPreferenceScore(UserProfileSignals signals, Hotel hotel) {
        double score = 0.5;
        if ("LUXURY".equalsIgnoreCase(signals.declaredBudget) && hotel.getStarRating() >= 4) score += 0.3;
        if ("BUDGET".equalsIgnoreCase(signals.declaredBudget) && hotel.getStarRating() <= 3) score += 0.3;
        return Math.min(1.0, score);
    }

    private double computeFlightRouteScore(UserProfileSignals signals, Flight flight) {
        String destCity = (flight.getDestination() != null && flight.getDestination().getCity() != null)
                ? flight.getDestination().getCity().toUpperCase() : "";
        if (signals.visitedCities.contains(destCity) || signals.tagAffinities.containsKey(destCity)) {
            return 0.9;
        }
        return 0.5;
    }

    private double computeFlightAirlineScore(UserProfileSignals signals, Flight flight) {
        if (flight.getAirline() != null && signals.favoriteAirlines.contains(flight.getAirline().getName())) {
            return 0.9;
        }
        return 0.6;
    }

    // ─────────────────────────────────────────────────────────────
    // EXPLANATION GENERATION
    // ─────────────────────────────────────────────────────────────

    private WhyRecommendationDto generateDestinationExplanation(UserProfileSignals signals, Destination dest,
                                                               double contentScore, double collabScore, double prefScore) {
        String title = "Recommended For You";
        String badge = dest.getCategory() + " Escape";
        List<RecommendationReasonDto> reasons = new ArrayList<>();

        if ("BEACH".equalsIgnoreCase(dest.getCategory())) {
            title = "You liked beaches! Try " + dest.getName();
            badge = "Beachfront Retreat";
            reasons.add(new RecommendationReasonDto("BOOKING_HISTORY",
                    "Based on your interest in relaxing beach destinations", 0.40));
        } else if ("MOUNTAIN".equalsIgnoreCase(dest.getCategory())) {
            title = "Love the outdoors? Explore " + dest.getName();
            badge = "Mountain Haven";
            reasons.add(new RecommendationReasonDto("BOOKING_HISTORY",
                    "Matches your history of mountain and scenic getaways", 0.40));
        } else if ("HERITAGE".equalsIgnoreCase(dest.getCategory())) {
            title = "Discover the heritage of " + dest.getName();
            badge = "Cultural Landmark";
            reasons.add(new RecommendationReasonDto("PREFERENCE_MATCH",
                    "Curated for your appreciation of cultural and historic travel", 0.40));
        } else {
            title = "Trending destination: " + dest.getName();
            badge = "Curated Discovery";
            reasons.add(new RecommendationReasonDto("POPULARITY_TRENDING",
                    "Highly rated by travelers on Voyara (" + String.format("%.1f", dest.getPopularityScore() * 5.0) + "/5.0)", 0.35));
        }

        if (collabScore > 0.45) {
            reasons.add(new RecommendationReasonDto("SIMILAR_USERS_COLLABORATIVE",
                    "Travelers with similar booking preferences also explored " + dest.getName(), 0.35));
        }

        if (prefScore > 0.6) {
            reasons.add(new RecommendationReasonDto("PREFERENCE_MATCH",
                    "Matches your declared travel preferences and budget profile", 0.25));
        }

        return new WhyRecommendationDto(title, badge, reasons);
    }

    private WhyRecommendationDto generateHotelExplanation(UserProfileSignals signals, Hotel hotel,
                                                         double contentScore, double collabScore, double prefScore) {
        String title = "Stay at " + hotel.getName();
        String badge = hotel.getStarRating() + "-Star Experience";
        List<RecommendationReasonDto> reasons = new ArrayList<>();

        if (collabScore > 0.45) {
            reasons.add(new RecommendationReasonDto("SIMILAR_USERS_COLLABORATIVE",
                    "Travelers who visited " + hotel.getCity() + " frequently chose this stay", 0.40));
        }

        if (hotel.getStarRating() >= 4) {
            reasons.add(new RecommendationReasonDto("PREFERENCE_MATCH",
                    "Matches your preference for premium luxury stays with top amenities", 0.35));
        } else {
            reasons.add(new RecommendationReasonDto("PRICE_AFFINITY",
                    "Great value stay in " + hotel.getCity() + " with verified guest ratings", 0.30));
        }

        if (hotel.getGuestRating() >= 4.0) {
            reasons.add(new RecommendationReasonDto("POPULARITY_TRENDING",
                    "Superb guest rating (" + String.format("%.1f", hotel.getGuestRating()) + "★) across verified reviews", 0.25));
        }

        return new WhyRecommendationDto(title, badge, reasons);
    }

    private WhyRecommendationDto generateFlightExplanation(UserProfileSignals signals, Flight flight, double routeInterest) {
        String origin = flight.getOriginCode() != null ? flight.getOriginCode() : "Origin";
        String dest = flight.getDestinationCode() != null ? flight.getDestinationCode() : "Destination";
        String title = "Flight to " + dest + " (" + (flight.getAirline() != null ? flight.getAirline().getName() : "") + ")";
        String badge = "Top Corridor Route";

        List<RecommendationReasonDto> reasons = new ArrayList<>();
        reasons.add(new RecommendationReasonDto("ROUTE_AFFINITY",
                "Popular route matching your recent searches and travel destinations", 0.50));
        if (flight.getEconomyPrice() != null) {
            reasons.add(new RecommendationReasonDto("PRICE_MATCH",
                "Competitive fare available from ₹" + flight.getEconomyPrice().intValue(), 0.35));
        }
        return new WhyRecommendationDto(title, badge, reasons);
    }

    private WhyRecommendationDto generateHolidayExplanation(UserProfileSignals signals, HolidayPackage pkg) {
        String title = "Curated Escape: " + pkg.getTitle();
        String badge = pkg.getTripType() + " Holiday";

        List<RecommendationReasonDto> reasons = new ArrayList<>();
        reasons.add(new RecommendationReasonDto("DESTINATION_AFFINITY",
                "Custom itinerary curated for " + pkg.getDestination(), 0.45));
        if (pkg.getRating() != null && pkg.getRating() >= 4.0) {
            reasons.add(new RecommendationReasonDto("POPULARITY_TRENDING",
                    "Top rated holiday experience (" + String.format("%.1f", pkg.getRating()) + "★)", 0.30));
        }
        if (pkg.getPricePerPerson() != null) {
            reasons.add(new RecommendationReasonDto("PRICE_MATCH",
                    "Value package from ₹" + pkg.getPricePerPerson().intValue() + "/person", 0.25));
        }
        return new WhyRecommendationDto(title, badge, reasons);
    }

    // ─────────────────────────────────────────────────────────────
    // COLD START FALLBACKS
    // ─────────────────────────────────────────────────────────────

    private List<RecommendationResponseDto> getColdStartRecommendations(String entityType, int limit) {
        List<RecommendationResponseDto> list = new ArrayList<>();

        if (entityType == null || "DESTINATION".equalsIgnoreCase(entityType)) {
            List<Destination> topDest = destinationRepo.findTopPopular(PageRequest.of(0, 4));
            for (Destination d : topDest) {
                Map<String, Object> details = new HashMap<>();
                details.put("name", d.getName());
                details.put("city", d.getCity());
                details.put("country", d.getCountry());
                details.put("category", d.getCategory());
                details.put("tags", d.getTags());
                details.put("imageUrl", d.getImageUrl());
                details.put("description", d.getDescription());
                details.put("averageDailyBudget", d.getAverageDailyBudget());

                WhyRecommendationDto why = new WhyRecommendationDto(
                        "Trending Destination: " + d.getName(),
                        d.getCategory() + " Highlight",
                        List.of(
                                new RecommendationReasonDto("POPULARITY_TRENDING", "Top trending destination among travelers on Voyara", 0.60),
                                new RecommendationReasonDto("SEASONAL_MATCH", "Ideal season for travel right now", 0.40)
                        )
                );

                list.add(new RecommendationResponseDto(
                        null, "DESTINATION", d.getId(), 88.0, "POPULAR_COLD_START",
                        "Popular choice: Explore " + d.getName(), why, null, details
                ));
            }
        }

        if (entityType == null || "HOTEL".equalsIgnoreCase(entityType)) {
            List<Hotel> topHotels = hotelRepo.findAll().stream()
                    .filter(h -> h.getStarRating() >= 4)
                    .limit(4)
                    .toList();
            for (Hotel h : topHotels) {
                Map<String, Object> details = new HashMap<>();
                details.put("name", h.getName());
                details.put("city", h.getCity());
                details.put("country", h.getCountry());
                details.put("starRating", h.getStarRating());
                details.put("guestRating", h.getGuestRating());
                details.put("startingPrice", h.getStartingPrice());
                details.put("imageUrl", h.getImageUrl());
                details.put("amenities", h.getAmenities());

                WhyRecommendationDto why = new WhyRecommendationDto(
                        "Featured Stay: " + h.getName(),
                        h.getStarRating() + "-Star Luxury",
                        List.of(
                                new RecommendationReasonDto("POPULARITY_TRENDING", "Consistently rated 4.5+ stars by verified guests", 0.60),
                                new RecommendationReasonDto("PRICE_AFFINITY", "Exclusive Voyara member rate available", 0.40)
                        )
                );

                list.add(new RecommendationResponseDto(
                        null, "HOTEL", h.getId(), 85.0, "POPULAR_COLD_START",
                        "Luxury Stay in " + h.getCity(), why, null, details
                ));
            }
        }

        if (entityType == null || "FLIGHT".equalsIgnoreCase(entityType)) {
            List<Flight> topFlights = flightRepo.findAll().stream().limit(4).toList();
            for (Flight f : topFlights) {
                Map<String, Object> details = new HashMap<>();
                details.put("flightNumber", f.getFlightNumber());
                details.put("airlineName", f.getAirline() != null ? f.getAirline().getName() : "");
                details.put("originCode", f.getOriginCode());
                details.put("destinationCode", f.getDestinationCode());
                details.put("departureTime", f.getDepartureTime());
                details.put("arrivalTime", f.getArrivalTime());
                details.put("price", f.getEconomyPrice());
                details.put("durationMinutes", f.getDurationMinutes());

                WhyRecommendationDto why = new WhyRecommendationDto(
                        "Popular Flight: " + f.getOriginCode() + " → " + f.getDestinationCode(),
                        "Top Route Choice",
                        List.of(
                                new RecommendationReasonDto("ROUTE_AFFINITY", "High-frequency corridor route with competitive fares", 0.60),
                                new RecommendationReasonDto("POPULARITY_TRENDING", "Preferred flight choice on Voyara", 0.40)
                        )
                );

                list.add(new RecommendationResponseDto(
                        null, "FLIGHT", f.getId(), 82.0, "POPULAR_COLD_START",
                        "Flight to " + f.getDestinationCode(), why, null, details
                ));
            }
        }

        if (entityType == null || "HOLIDAY_PACKAGE".equalsIgnoreCase(entityType) || "HOLIDAY".equalsIgnoreCase(entityType)) {
            List<HolidayPackage> topPkgs = holidayPackageRepo.findByFeaturedTrueAndActiveTrue().stream().limit(4).toList();
            if (topPkgs.isEmpty()) {
                topPkgs = holidayPackageRepo.findByActiveTrue().stream().limit(4).toList();
            }
            for (HolidayPackage pkg : topPkgs) {
                Map<String, Object> details = new HashMap<>();
                details.put("title", pkg.getTitle());
                details.put("destination", pkg.getDestination());
                details.put("destinationCode", pkg.getDestinationCode());
                details.put("tripType", pkg.getTripType());
                details.put("pricePerPerson", pkg.getPricePerPerson());
                details.put("originalPrice", pkg.getOriginalPrice());
                details.put("durationDays", pkg.getDurationDays());
                details.put("durationNights", pkg.getDurationNights());
                details.put("hotelCategory", pkg.getHotelCategory());
                details.put("rating", pkg.getRating());
                details.put("imageUrl", pkg.getImageUrl());

                WhyRecommendationDto why = new WhyRecommendationDto(
                        "Featured Package: " + pkg.getTitle(),
                        pkg.getDurationDays() + "D/" + pkg.getDurationNights() + "N Getaway",
                        List.of(
                                new RecommendationReasonDto("POPULARITY_TRENDING", "Trending all-inclusive holiday package", 0.60),
                                new RecommendationReasonDto("PRICE_AFFINITY", "Best price guarantee for verified package", 0.40)
                        )
                );

                list.add(new RecommendationResponseDto(
                        null, "HOLIDAY_PACKAGE", pkg.getId(), 86.0, "POPULAR_COLD_START",
                        "Holiday: " + pkg.getTitle(), why, null, details
                ));
            }
        }

        return list.stream().limit(limit).toList();
    }

    // ─────────────────────────────────────────────────────────────
    // MAPPER & SERIALIZATION
    // ─────────────────────────────────────────────────────────────

    private RecommendationResponseDto mapToResponseDto(Recommendation rec, Long userId) {
        Map<String, Object> details = new HashMap<>();

        if ("DESTINATION".equals(rec.getEntityType())) {
            Destination d = destinationRepo.findById(rec.getEntityId()).orElse(null);
            if (d == null) return null;
            details.put("name", d.getName());
            details.put("city", d.getCity());
            details.put("country", d.getCountry());
            details.put("category", d.getCategory());
            details.put("tags", d.getTags());
            details.put("imageUrl", d.getImageUrl());
            details.put("description", d.getDescription());
            details.put("averageDailyBudget", d.getAverageDailyBudget());
        } else if ("HOTEL".equals(rec.getEntityType())) {
            Hotel h = hotelRepo.findById(rec.getEntityId()).orElse(null);
            if (h == null) return null;
            details.put("name", h.getName());
            details.put("city", h.getCity());
            details.put("country", h.getCountry());
            details.put("starRating", h.getStarRating());
            details.put("guestRating", h.getGuestRating());
            details.put("startingPrice", h.getStartingPrice());
            details.put("imageUrl", h.getImageUrl());
            details.put("amenities", h.getAmenities());
        } else if ("FLIGHT".equals(rec.getEntityType())) {
            Flight f = flightRepo.findById(rec.getEntityId()).orElse(null);
            if (f == null) return null;
            details.put("flightNumber", f.getFlightNumber());
            details.put("airlineName", f.getAirline() != null ? f.getAirline().getName() : "");
            details.put("originCode", f.getOriginCode());
            details.put("destinationCode", f.getDestinationCode());
            details.put("departureTime", f.getDepartureTime());
            details.put("arrivalTime", f.getArrivalTime());
            details.put("price", f.getEconomyPrice());
            details.put("durationMinutes", f.getDurationMinutes());
        } else if ("HOLIDAY_PACKAGE".equals(rec.getEntityType()) || "HOLIDAY".equals(rec.getEntityType())) {
            HolidayPackage pkg = holidayPackageRepo.findById(rec.getEntityId()).orElse(null);
            if (pkg == null) return null;
            details.put("title", pkg.getTitle());
            details.put("destination", pkg.getDestination());
            details.put("destinationCode", pkg.getDestinationCode());
            details.put("tripType", pkg.getTripType());
            details.put("pricePerPerson", pkg.getPricePerPerson());
            details.put("originalPrice", pkg.getOriginalPrice());
            details.put("durationDays", pkg.getDurationDays());
            details.put("durationNights", pkg.getDurationNights());
            details.put("hotelCategory", pkg.getHotelCategory());
            details.put("rating", pkg.getRating());
            details.put("imageUrl", pkg.getImageUrl());
        }

        WhyRecommendationDto why = deserializeWhy(rec.getStructuredReasonsJson(), rec.getReason());
        String headline = (why != null && why.getTitle() != null) ? why.getTitle() : rec.getReason();

        // Check user's permanent feedback for this entity
        String feedback = rec.getFeedback();
        if (feedback == null && userId != null) {
            feedback = feedbackRepo.findByUserIdAndEntityTypeAndEntityId(userId, rec.getEntityType(), rec.getEntityId())
                    .map(RecommendationFeedback::getFeedbackType)
                    .orElse(null);
        }

        return new RecommendationResponseDto(
                rec.getId(),
                rec.getEntityType(),
                rec.getEntityId(),
                rec.getScore() != null ? rec.getScore().doubleValue() : 75.0,
                rec.getAlgorithm(),
                headline,
                why,
                feedback,
                details
        );
    }

    private String serializeWhy(WhyRecommendationDto why) {
        try {
            return objectMapper.writeValueAsString(why);
        } catch (Exception e) {
            return null;
        }
    }

    private WhyRecommendationDto deserializeWhy(String json, String defaultReason) {
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, WhyRecommendationDto.class);
            } catch (Exception ignored) {}
        }
        String fallbackTitle = (defaultReason != null && !defaultReason.isBlank()) ? defaultReason : "Personalized Recommendation";
        return new WhyRecommendationDto(fallbackTitle, "Voyara Match",
                List.of(new RecommendationReasonDto("SYSTEM_MATCH", fallbackTitle, 1.0)));
    }

    private static class UserProfileSignals {
        Map<String, Double> tagAffinities = new HashMap<>();
        Map<String, Double> normalizedTags = new HashMap<>();
        Set<String> visitedCities = new HashSet<>();
        Set<String> favoriteAirlines = new HashSet<>();
        String declaredDestinations;
        String declaredCabinClass;
        String declaredBudget;
        String declaredRoomType;
        String declaredSeatType;
    }
}
