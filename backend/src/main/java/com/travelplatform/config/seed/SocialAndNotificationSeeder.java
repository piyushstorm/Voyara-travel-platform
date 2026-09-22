package com.travelplatform.config.seed;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class SocialAndNotificationSeeder {

    private static final Logger log = LoggerFactory.getLogger(SocialAndNotificationSeeder.class);

    private final NotificationRepository notificationRepo;
    private final UserTravelPreferenceRepository prefRepo;
    private final GroupTripRepository groupTripRepo;
    private final TravelCompanionRepository companionRepo;
    private final TripInvitationRepository invitationRepo;
    private final GroupTripBookingRepository groupTripBookingRepo;
    private final TripExpenseRepository expenseRepo;
    private final TripExpenseParticipantRepository participantRepo;
    private final TripSettlementRepository settlementRepo;

    public SocialAndNotificationSeeder(NotificationRepository notificationRepo,
                                       UserTravelPreferenceRepository prefRepo,
                                       GroupTripRepository groupTripRepo,
                                       TravelCompanionRepository companionRepo,
                                       TripInvitationRepository invitationRepo,
                                       GroupTripBookingRepository groupTripBookingRepo,
                                       TripExpenseRepository expenseRepo,
                                       TripExpenseParticipantRepository participantRepo,
                                       TripSettlementRepository settlementRepo) {
        this.notificationRepo = notificationRepo;
        this.prefRepo = prefRepo;
        this.groupTripRepo = groupTripRepo;
        this.companionRepo = companionRepo;
        this.invitationRepo = invitationRepo;
        this.groupTripBookingRepo = groupTripBookingRepo;
        this.expenseRepo = expenseRepo;
        this.participantRepo = participantRepo;
        this.settlementRepo = settlementRepo;
    }

    @Transactional
    public void seedSocialAndNotifications(Map<String, User> userMap, List<Booking> bookings) {
        User demoUser = userMap.get("user@travel.com");
        User traveler = userMap.get("traveler@travel.com");
        User tripLead = userMap.get("trip.lead@travel.com");
        User comp1 = userMap.get("companion1@travel.com");
        User comp2 = userMap.get("companion2@travel.com");
        User comp3 = userMap.get("companion3@travel.com");

        if (demoUser == null) return;

        // 1. User Travel Preferences
        if (prefRepo.count() == 0) {
            log.info("Seeding user travel preferences...");
            List<UserTravelPreference> prefs = new ArrayList<>();

            UserTravelPreference p1 = new UserTravelPreference();
            p1.setUser(demoUser);
            p1.setPreferredSeatPosition("WINDOW");
            p1.setPreferredSeatType("EXTRA_LEGROOM");
            p1.setPreferredRoomType("DELUXE");
            p1.setPreferredBedType("KING");
            p1.setPreferredRoomFeatures("CITY_VIEW,HIGH_FLOOR");
            prefs.add(p1);

            if (traveler != null) {
                UserTravelPreference p2 = new UserTravelPreference();
                p2.setUser(traveler);
                p2.setPreferredSeatPosition("AISLE");
                p2.setPreferredSeatType("STANDARD");
                p2.setPreferredRoomType("SUITE");
                p2.setPreferredBedType("KING");
                p2.setPreferredRoomFeatures("QUIET_ROOM,BALCONY");
                prefs.add(p2);
            }

            prefRepo.saveAll(prefs);
            log.info("Seeded user travel preferences.");
        }

        // 2. Realistic Notifications
        if (notificationRepo.count() >= 6) {
            log.info("Notifications already seeded, skipping.");
        } else {
            log.info("Seeding realistic notifications for test users...");
            List<Notification> notifs = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // Flight notifications for user@travel.com
            Notification n1 = new Notification();
            n1.setUser(demoUser);
            n1.setType("BOOKING_CONFIRMED");
            n1.setTitle("Flight Booking Confirmed");
            n1.setMessage("Your flight to Delhi (DEL) is confirmed. Reference: VOY-FL-2000. Have a wonderful journey!");
            n1.setRead(true);
            n1.setReadAt(now.minusHours(3));
            n1.setChannel("BOTH");
            n1.setDeliveryStatus("SENT");
            n1.setIdempotencyKey("notif_seed_001");
            notifs.add(n1);

            Notification n2 = new Notification();
            n2.setUser(demoUser);
            n2.setType("GATE_CHANGE");
            n2.setTitle("Gate Update for Your Flight");
            n2.setMessage("Departure gate changed to Gate B6, Terminal 2. Boarding will commence 40 minutes prior to departure.");
            n2.setRead(false);
            n2.setChannel("IN_APP");
            n2.setDeliveryStatus("SENT");
            n2.setIdempotencyKey("notif_seed_002");
            notifs.add(n2);

            Notification n3 = new Notification();
            n3.setUser(demoUser);
            n3.setType("PRICE_FREEZE_EXPIRING");
            n3.setTitle("Price Freeze Expiring Soon");
            n3.setMessage("Your locked flight price of ₹4,800 expires in 45 minutes. Complete your booking to secure this rate.");
            n3.setRead(false);
            n3.setChannel("BOTH");
            n3.setDeliveryStatus("SENT");
            n3.setIdempotencyKey("notif_seed_003");
            notifs.add(n3);

            Notification n4 = new Notification();
            n4.setUser(demoUser);
            n4.setType("REFUND_PROCESSED");
            n4.setTitle("Refund Initiated Successfully");
            n4.setMessage("Refund of ₹3,750 for cancelled hotel booking has been approved and sent to your original payment method.");
            n4.setRead(true);
            n4.setReadAt(now.minusDays(1));
            n4.setChannel("BOTH");
            n4.setDeliveryStatus("SENT");
            n4.setIdempotencyKey("notif_seed_004");
            notifs.add(n4);

            if (traveler != null) {
                Notification n5 = new Notification();
                n5.setUser(traveler);
                n5.setType("FLIGHT_DELAYED");
                n5.setTitle("Flight Delay Notice");
                n5.setMessage("Flight AI-1002 has been delayed by 35 minutes due to air traffic control congestion. Revised departure: 10:20 AM.");
                n5.setRead(false);
                n5.setChannel("BOTH");
                n5.setDeliveryStatus("SENT");
                n5.setIdempotencyKey("notif_seed_005");
                notifs.add(n5);

                Notification n6 = new Notification();
                n6.setUser(traveler);
                n6.setType("REWARD_CREDITED");
                n6.setTitle("Voyara Coins Credited");
                n6.setMessage("Congratulations! 250 Voyara travel rewards have been credited to your account for your recent trip.");
                n6.setRead(true);
                n6.setReadAt(now.minusDays(2));
                n6.setChannel("IN_APP");
                n6.setDeliveryStatus("SENT");
                n6.setIdempotencyKey("notif_seed_006");
                notifs.add(n6);
            }

            notificationRepo.saveAll(notifs);
            log.info("Seeded {} notifications.", notifs.size());
        }

        // 3. Group Trips, Companions, Expenses & Settlements
        if (groupTripRepo.count() == 0 && tripLead != null && comp1 != null && comp2 != null && comp3 != null) {
            log.info("Seeding realistic group trip and shared expense split scenario...");

            GroupTrip trip = new GroupTrip();
            trip.setName("Goa Reunion & Sunburn 2026");
            trip.setOwner(tripLead);
            trip.setDescription("Annual college friends holiday getaway to North Goa. Beach shacks, watersports, and music festival.");
            trip.setStartDate(LocalDate.now().plusDays(20));
            trip.setEndDate(LocalDate.now().plusDays(25));
            trip.setActive(true);
            trip = groupTripRepo.save(trip);

            // Travel Companions
            TravelCompanion cLead = new TravelCompanion();
            cLead.setGroupTrip(trip);
            cLead.setUser(tripLead);
            cLead.setRole("ORGANIZER");
            cLead.setStatus("ACCEPTED");
            cLead.setJoinedAt(LocalDateTime.now().minusDays(5));
            companionRepo.save(cLead);

            TravelCompanion c1 = new TravelCompanion();
            c1.setGroupTrip(trip);
            c1.setUser(comp1);
            c1.setRole("COMPANION");
            c1.setStatus("ACCEPTED");
            c1.setJoinedAt(LocalDateTime.now().minusDays(4));
            companionRepo.save(c1);

            TravelCompanion c2 = new TravelCompanion();
            c2.setGroupTrip(trip);
            c2.setUser(comp2);
            c2.setRole("COMPANION");
            c2.setStatus("ACCEPTED");
            c2.setJoinedAt(LocalDateTime.now().minusDays(3));
            companionRepo.save(c2);

            TravelCompanion c3 = new TravelCompanion();
            c3.setGroupTrip(trip);
            c3.setUser(comp3);
            c3.setRole("COMPANION");
            c3.setStatus("ACCEPTED");
            c3.setJoinedAt(LocalDateTime.now().minusDays(2));
            companionRepo.save(c3);

            // Pending invitation for demoUser
            TripInvitation inv = new TripInvitation();
            inv.setGroupTrip(trip);
            inv.setInviterUser(tripLead);
            inv.setInviteeEmail("user@travel.com");
            inv.setInviteeUser(demoUser);
            inv.setToken("inv_token_goa_" + System.currentTimeMillis());
            inv.setStatus("PENDING");
            inv.setMessage("Join our Goa trip! We booked beachfront villas in Calangute.");
            inv.setExpiresAt(LocalDateTime.now().plusDays(15));
            invitationRepo.save(inv);

            // Link booking to group trip if available
            if (!bookings.isEmpty()) {
                GroupTripBooking gtb = new GroupTripBooking();
                gtb.setGroupTrip(trip);
                gtb.setBookingId(bookings.get(0).getId());
                gtb.setAddedByUserId(tripLead.getId());
                groupTripBookingRepo.save(gtb);
            }

            // Shared Expenses
            // Expense 1: Beachside Dinner ₹8,000 paid by tripLead, equal 4-way split (₹2,000 each)
            TripExpense exp1 = new TripExpense();
            exp1.setGroupTrip(trip);
            exp1.setPaidByUser(tripLead);
            exp1.setDescription("Seafood dinner & refreshments at Thalassa Vagator");
            exp1.setAmount(BigDecimal.valueOf(8000.00));
            exp1.setCurrency("INR");
            exp1.setExpenseType("FOOD");
            exp1.setSplitMode("EQUAL");
            exp1.setExpenseDate(LocalDate.now().minusDays(1));
            exp1 = expenseRepo.save(exp1);

            TripExpenseParticipant epLead = new TripExpenseParticipant();
            epLead.setExpense(exp1);
            epLead.setUser(tripLead);
            epLead.setShareAmount(BigDecimal.valueOf(2000.00));
            epLead.setSettled(true);
            epLead.setSettledAt(LocalDateTime.now().minusDays(1));
            participantRepo.save(epLead);

            TripExpenseParticipant ep1 = new TripExpenseParticipant();
            ep1.setExpense(exp1);
            ep1.setUser(comp1);
            ep1.setShareAmount(BigDecimal.valueOf(2000.00));
            ep1.setSettled(true);
            ep1.setSettledAt(LocalDateTime.now().minusHours(12));
            participantRepo.save(ep1);

            TripExpenseParticipant ep2 = new TripExpenseParticipant();
            ep2.setExpense(exp1);
            ep2.setUser(comp2);
            ep2.setShareAmount(BigDecimal.valueOf(2000.00));
            ep2.setSettled(true);
            ep2.setSettledAt(LocalDateTime.now().minusHours(6));
            participantRepo.save(ep2);

            TripExpenseParticipant ep3 = new TripExpenseParticipant();
            ep3.setExpense(exp1);
            ep3.setUser(comp3);
            ep3.setShareAmount(BigDecimal.valueOf(2000.00));
            ep3.setSettled(false); // pending
            participantRepo.save(ep3);

            // Settlement record: comp2 paid back tripLead ₹2,000
            TripSettlement s1 = new TripSettlement();
            s1.setGroupTrip(trip);
            s1.setFromUser(comp2);
            s1.setToUser(tripLead);
            s1.setAmount(BigDecimal.valueOf(2000.00));
            s1.setCurrency("INR");
            s1.setStatus("SETTLED");
            s1.setSettledAt(LocalDateTime.now().minusHours(6));
            s1.setNotes("Settled via UPI for Thalassa dinner");
            settlementRepo.save(s1);

            log.info("Seeded group trip, companions, shared split expenses, and settlements.");
        }
    }
}
