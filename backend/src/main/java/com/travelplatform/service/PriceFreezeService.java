package com.travelplatform.service;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.PriceFreeze;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.PriceFreezeRepository;
import com.travelplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Price freeze: users pay a small fee to lock the current price.
 * If they book within the window, they get the frozen price.
 * Freeze fee: 5% of price or ₹99 minimum, whichever is higher.
 * Freeze window: 15 minutes.
 */
@Service
public class PriceFreezeService {

    private static final Logger logger = LoggerFactory.getLogger(PriceFreezeService.class);

    private static final BigDecimal FREEZE_FEE_PERCENT = BigDecimal.valueOf(0.05);
    private static final BigDecimal MIN_FREEZE_FEE = BigDecimal.valueOf(99);
    private static final int FREEZE_WINDOW_MINUTES = 15;

    private final PriceFreezeRepository freezeRepo;
    private final FlightRepository flightRepo;
    private final UserRepository userRepo;

    public PriceFreezeService(PriceFreezeRepository freezeRepo,
                               FlightRepository flightRepo,
                               UserRepository userRepo) {
        this.freezeRepo = freezeRepo;
        this.flightRepo = flightRepo;
        this.userRepo = userRepo;
    }

    /** Create a price freeze for a flight */
    @Transactional
    public PriceFreeze createFlightFreeze(Long flightId, String cabinClass, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Flight flight = flightRepo.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", flightId));

        // Check for existing active freeze for THIS user
        List<PriceFreeze> userActiveFreezes = freezeRepo.findByUserIdAndStatus(userId, "ACTIVE");
        for (PriceFreeze f : userActiveFreezes) {
            if ("FLIGHT".equals(f.getEntityType()) && f.getEntityId().equals(flightId)) {
                if (!f.isExpired()) {
                    throw new BadRequestException("You already have an active price freeze for this flight");
                } else {
                    f.setStatus("EXPIRED");
                    freezeRepo.save(f);
                }
            }
        }

        BigDecimal currentPrice = flight.getPriceForClass(cabinClass);
        BigDecimal freezeFee = currentPrice.multiply(FREEZE_FEE_PERCENT).max(MIN_FREEZE_FEE)
                .setScale(2, RoundingMode.HALF_UP);

        PriceFreeze freeze = new PriceFreeze();
        freeze.setUser(user);
        freeze.setEntityType("FLIGHT");
        freeze.setEntityId(flightId);
        freeze.setCabinClass(cabinClass);
        freeze.setFrozenPrice(currentPrice);
        freeze.setFreezeFee(freezeFee);
        freeze.setExpiresAt(LocalDateTime.now().plusMinutes(FREEZE_WINDOW_MINUTES));
        freeze.setStatus("ACTIVE");

        freeze = freezeRepo.save(freeze);
        logger.info("Price freeze created: user={}, flight={}, cabin={}, price={}, fee={}, expires={}",
                userId, flightId, cabinClass, currentPrice, freezeFee, freeze.getExpiresAt());
        return freeze;
    }

    /** Get active freeze for a flight + cabin class */
    @Transactional(readOnly = true)
    public PriceFreeze getActiveFreeze(String entityType, Long entityId, Long userId) {
        List<PriceFreeze> freezes = freezeRepo.findByUserIdAndStatus(userId, "ACTIVE");
        return freezes.stream()
                .filter(f -> entityType.equals(f.getEntityType()) && entityId.equals(f.getEntityId()))
                .filter(f -> !f.isExpired())
                .findFirst()
                .orElse(null);
    }

    /** Get all price freezes for a user */
    @Transactional(readOnly = true)
    public List<PriceFreeze> getUserFreezes(Long userId) {
        return freezeRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Get a freeze by ID */
    @Transactional(readOnly = true)
    public PriceFreeze getFreezeById(Long freezeId) {
        return freezeRepo.findById(freezeId)
                .orElseThrow(() -> new ResourceNotFoundException("PriceFreeze", "id", freezeId));
    }

    /** Use a freeze when creating a booking — returns the frozen price */
    @Transactional
    public BigDecimal useFreeze(Long freezeId, Long bookingId, Long userId, Long flightId, String cabinClass) {
        PriceFreeze freeze = freezeRepo.findById(freezeId)
                .orElseThrow(() -> new ResourceNotFoundException("PriceFreeze", "id", freezeId));

        if (!freeze.getUser().getId().equals(userId)) {
            throw new BadRequestException("Price freeze does not belong to this user");
        }
        
        if (!"FLIGHT".equals(freeze.getEntityType()) || !freeze.getEntityId().equals(flightId)) {
            throw new BadRequestException("Price freeze does not match this flight");
        }
        
        if (!freeze.getCabinClass().equalsIgnoreCase(cabinClass)) {
            throw new BadRequestException("Price freeze cabin class does not match");
        }

        if (freeze.isExpired()) {
            freeze.setStatus("EXPIRED");
            freezeRepo.save(freeze);
            throw new BadRequestException("Price freeze has expired");
        }

        if (!"ACTIVE".equals(freeze.getStatus())) {
            throw new BadRequestException("Price freeze is not active (status: " + freeze.getStatus() + ")");
        }

        // Atomic row-level update to guarantee double-consumption and concurrency protection
        int updated = freezeRepo.consumeFreezeAtomically(freezeId, userId, LocalDateTime.now());
        if (updated == 0) {
            throw new BadRequestException("Price freeze is no longer active, has expired, or was already consumed");
        }

        freeze.setStatus("USED");
        freezeRepo.save(freeze);

        logger.info("Price freeze {} atomically consumed for booking {}, locked price: {}",
                freezeId, bookingId, freeze.getFrozenPrice());
        return freeze.getFrozenPrice();
    }

    /** Expire stale freezes */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void expireStaleFreezes() {
        List<PriceFreeze> allActive = freezeRepo.findAll().stream()
                .filter(f -> "ACTIVE".equals(f.getStatus()))
                .toList();

        for (PriceFreeze freeze : allActive) {
            if (freeze.isExpired()) {
                freeze.setStatus("EXPIRED");
                freezeRepo.save(freeze);
                logger.info("Price freeze {} expired (was for {} {})",
                        freeze.getId(), freeze.getEntityType(), freeze.getEntityId());
            }
        }
    }
}
