package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class RewardService {

    private static final Logger logger = LoggerFactory.getLogger(RewardService.class);

    private final RewardAccountRepository accountRepo;
    private final RewardTransactionRepository transactionRepo;
    private final RewardConfigRepository configRepo;
    private final UserRepository userRepo;

    public RewardService(RewardAccountRepository accountRepo,
                         RewardTransactionRepository transactionRepo,
                         RewardConfigRepository configRepo,
                         UserRepository userRepo) {
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
        this.configRepo = configRepo;
        this.userRepo = userRepo;
    }

    // ─── Config ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RewardConfig getConfig() {
        return configRepo.findFirstByOrderByIdAsc()
                .orElseGet(() -> configRepo.save(new RewardConfig()));
    }

    @Transactional
    public RewardConfig updateConfig(RewardConfig updates) {
        RewardConfig config = getConfig();
        if (updates.getPointsPerHundredRupees() > 0) config.setPointsPerHundredRupees(updates.getPointsPerHundredRupees());
        if (updates.getPointsPerRupeeRedemption() > 0) config.setPointsPerRupeeRedemption(updates.getPointsPerRupeeRedemption());
        if (updates.getGoldTierThreshold() > 0) config.setGoldTierThreshold(updates.getGoldTierThreshold());
        if (updates.getPlatinumTierThreshold() > 0) config.setPlatinumTierThreshold(updates.getPlatinumTierThreshold());
        if (updates.getGoldEarningMultiplier() > 0) config.setGoldEarningMultiplier(updates.getGoldEarningMultiplier());
        if (updates.getPlatinumEarningMultiplier() > 0) config.setPlatinumEarningMultiplier(updates.getPlatinumEarningMultiplier());
        if (updates.getPointsExpiryDays() >= 0) config.setPointsExpiryDays(updates.getPointsExpiryDays());
        if (updates.getMinRedemptionPoints() > 0) config.setMinRedemptionPoints(updates.getMinRedemptionPoints());
        if (updates.getMaxRedemptionPercent() > 0) config.setMaxRedemptionPercent(updates.getMaxRedemptionPercent());
        return configRepo.save(config);
    }

    // ─── Account ────────────────────────────────────────────

    @Transactional
    public RewardAccount getOrCreateAccount(Long userId) {
        return accountRepo.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepo.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return accountRepo.save(new RewardAccount(user));
                });
    }

    @Transactional(readOnly = true)
    public RewardAccount getAccount(Long userId) {
        return getOrCreateAccount(userId);
    }

    // ─── Earning ────────────────────────────────────────────

    /**
     * Award points for a confirmed, paid booking.
     * Idempotent: same bookingReference cannot earn twice.
     * Points are calculated from totalAmount (after coupon discount).
     */
    @Transactional
    public RewardTransaction awardBookingPoints(Booking booking) {
        if (booking == null || booking.getUser() == null) {
            throw new BadRequestException("Invalid booking for reward earning");
        }

        String bookingRef = booking.getBookingReference();
        RewardConfig config = getConfig();

        // Idempotency: check if already earned
        if (transactionRepo.existsByTransactionTypeAndBookingReference("BOOKING_EARN", bookingRef)) {
            logger.info("Points already awarded for booking {}, skipping", bookingRef);
            return transactionRepo.findByTransactionTypeAndBookingReference("BOOKING_EARN", bookingRef)
                    .orElse(null);
        }

        // Only award for CONFIRMED bookings with positive amount
        if (!"CONFIRMED".equals(booking.getStatus())) {
            logger.info("Booking {} status is {}, not awarding points", bookingRef, booking.getStatus());
            return null;
        }

        BigDecimal eligibleAmount = booking.getTotalAmount();
        if (eligibleAmount == null || eligibleAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Booking {} has no eligible amount, not awarding points", bookingRef);
            return null;
        }

        RewardAccount account = getOrCreateAccount(booking.getUser().getId());

        // Calculate points: eligibleAmount / 100 * pointsPerHundredRupees
        long basePoints = eligibleAmount
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .longValue() * config.getPointsPerHundredRupees();

        // Apply tier multiplier
        double multiplier = getTierMultiplier(account.getTier(), config);
        long points = Math.max(1, Math.round(basePoints * multiplier));

        // Update account
        account.setPointsBalance(account.getPointsBalance() + points);
        account.setLifetimePointsEarned(account.getLifetimePointsEarned() + points);
        account.setQualifyingPoints(account.getQualifyingPoints() + points);

        // Recalculate tier
        account.setTier(calculateTier(account.getQualifyingPoints(), config));
        account = accountRepo.save(account);

        // Create immutable ledger entry
        RewardTransaction txn = new RewardTransaction();
        txn.setUser(booking.getUser());
        txn.setPoints(points);
        txn.setTransactionType("BOOKING_EARN");
        txn.setBalanceAfter(account.getPointsBalance());
        txn.setBookingReference(bookingRef);
        txn.setEligibleAmount(eligibleAmount);
        txn.setDescription("Points earned for " + booking.getBookingType() + " booking " + bookingRef);
        txn.setIdempotencyKey("EARN_" + bookingRef);
        txn = transactionRepo.save(txn);

        logger.info("Awarded {} points for booking {} (eligible amount: {}, tier: {})",
                points, bookingRef, eligibleAmount, account.getTier());
        return txn;
    }

    // ─── Redemption ─────────────────────────────────────────

    /**
     * Validate and calculate redemption value for a booking.
     * Returns the backend-calculated redemption details.
     */
    @Transactional
    public Map<String, Object> validateRedemption(Long userId, long requestedPoints, BigDecimal bookingAmount) {
        RewardConfig config = getConfig();
        RewardAccount account = getOrCreateAccount(userId);

        Map<String, Object> result = new HashMap<>();

        if (requestedPoints <= 0) {
            result.put("valid", false);
            result.put("message", "Points must be greater than zero");
            return result;
        }

        if (requestedPoints < config.getMinRedemptionPoints()) {
            result.put("valid", false);
            result.put("message", "Minimum " + config.getMinRedemptionPoints() + " points required for redemption");
            return result;
        }

        if (requestedPoints > account.getPointsBalance()) {
            result.put("valid", false);
            result.put("message", "Insufficient points balance. You have " + account.getPointsBalance() + " points");
            return result;
        }

        // Max redemption: maxRedemptionPercent of booking amount
        BigDecimal maxRedeemableAmount = bookingAmount
                .multiply(BigDecimal.valueOf(config.getMaxRedemptionPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.FLOOR);
        long maxPoints = maxRedeemableAmount
                .multiply(BigDecimal.valueOf(config.getPointsPerRupeeRedemption()))
                .longValue();

        long actualPoints = Math.min(requestedPoints, maxPoints);

        // Points to INR: points / pointsPerRupeeRedemption
        BigDecimal discountAmount = BigDecimal.valueOf(actualPoints)
                .divide(BigDecimal.valueOf(config.getPointsPerRupeeRedemption()), 2, RoundingMode.FLOOR);
        discountAmount = discountAmount.min(bookingAmount); // Never exceed booking amount

        BigDecimal finalAmount = bookingAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
            discountAmount = bookingAmount;
        }

        result.put("valid", true);
        result.put("pointsToRedeem", actualPoints);
        result.put("discountAmount", discountAmount);
        result.put("originalAmount", bookingAmount);
        result.put("finalAmount", finalAmount);
        result.put("availablePoints", account.getPointsBalance());
        result.put("pointsValue", discountAmount + " INR");

        return result;
    }

    /**
     * Process a redemption: deduct points and create ledger entry.
     * Called after successful booking/payment.
     */
    @Transactional
    public RewardTransaction processRedemption(Long userId, long pointsToRedeem,
                                                BigDecimal discountAmount, String bookingReference) {
        RewardAccount account = getOrCreateAccount(userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Re-validate balance (concurrent safety)
        if (pointsToRedeem > account.getPointsBalance()) {
            throw new BadRequestException("Insufficient points balance for redemption");
        }

        // Idempotency
        String idempotencyKey = "REDEEM_" + bookingReference;
        if (transactionRepo.existsByIdempotencyKey(idempotencyKey)) {
            logger.info("Redemption already processed for booking {}", bookingReference);
            return transactionRepo.findByIdempotencyKey(idempotencyKey).orElse(null);
        }

        // Deduct points
        account.setPointsBalance(account.getPointsBalance() - pointsToRedeem);
        account.setLifetimePointsRedeemed(account.getLifetimePointsRedeemed() + pointsToRedeem);
        account = accountRepo.save(account);

        // Create ledger entry
        RewardTransaction txn = new RewardTransaction();
        txn.setUser(user);
        txn.setPoints(-pointsToRedeem);
        txn.setTransactionType("REDEMPTION");
        txn.setBalanceAfter(account.getPointsBalance());
        txn.setBookingReference(bookingReference);
        txn.setEligibleAmount(discountAmount);
        txn.setDescription("Redeemed " + pointsToRedeem + " points (₹" + discountAmount + " discount)");
        txn.setIdempotencyKey(idempotencyKey);
        txn = transactionRepo.save(txn);

        logger.info("Redeemed {} points for booking {} (discount: ₹{})", pointsToRedeem, bookingReference, discountAmount);
        return txn;
    }

    // ─── Expiry ─────────────────────────────────────────────

    /**
     * Process expired points. Runs daily at 2 AM.
     * Finds earning transactions older than expiry period and expires unspent points.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public int processExpiredPoints() {
        RewardConfig config = getConfig();
        if (config.getPointsExpiryDays() <= 0) return 0; // Expiry disabled

        LocalDateTime expiryCutoff = LocalDateTime.now().minusDays(config.getPointsExpiryDays());
        int expired = 0;

        // For each user with a reward account, check for expiring points
        for (RewardAccount account : accountRepo.findAll()) {
            long userBalance = account.getPointsBalance();
            if (userBalance <= 0) continue;

            var expiringTxns = transactionRepo.findExpiringPoints(account.getUser().getId(), expiryCutoff);
            long totalExpiring = expiringTxns.stream().mapToLong(RewardTransaction::getPoints).sum();

            if (totalExpiring <= 0) continue;

            long pointsToExpire = Math.min(totalExpiring, userBalance);
            if (pointsToExpire <= 0) continue;

            // Deduct from balance
            account.setPointsBalance(account.getPointsBalance() - pointsToExpire);
            account.setLifetimePointsExpired(account.getLifetimePointsExpired() + pointsToExpire);
            accountRepo.save(account);

            // Create expiry ledger entry
            RewardTransaction txn = new RewardTransaction();
            txn.setUser(account.getUser());
            txn.setPoints(-pointsToExpire);
            txn.setTransactionType("EXPIRY");
            txn.setBalanceAfter(account.getPointsBalance());
            txn.setDescription(pointsToExpire + " points expired after " + config.getPointsExpiryDays() + " days");
            txn.setIdempotencyKey("EXPIRY_" + account.getUser().getId() + "_" + System.currentTimeMillis());
            transactionRepo.save(txn);

            // Recalculate tier
            account.setTier(calculateTier(account.getQualifyingPoints(), config));
            accountRepo.save(account);

            expired++;
            logger.info("Expired {} points for user {}", pointsToExpire, account.getUser().getId());
        }
        return expired;
    }

    // ─── Admin Adjustment ───────────────────────────────────

    @Transactional
    public RewardTransaction adminAdjustPoints(Long adminId, Long targetUserId, long points,
                                                String reason, boolean isAdd) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required for admin point adjustment");
        }

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", adminId));
        RewardAccount account = getOrCreateAccount(targetUserId);

        long adjustedPoints = isAdd ? Math.abs(points) : -Math.abs(points);

        if (!isAdd && Math.abs(adjustedPoints) > account.getPointsBalance()) {
            throw new BadRequestException("Cannot deduct more points than available balance (" +
                    account.getPointsBalance() + ")");
        }

        // Update account
        account.setPointsBalance(account.getPointsBalance() + adjustedPoints);
        if (isAdd) {
            account.setLifetimePointsEarned(account.getLifetimePointsEarned() + Math.abs(adjustedPoints));
        } else {
            account.setLifetimePointsRedeemed(account.getLifetimePointsRedeemed() + Math.abs(adjustedPoints));
        }

        RewardConfig config = getConfig();
        account.setTier(calculateTier(account.getQualifyingPoints(), config));
        account = accountRepo.save(account);

        // Create ledger entry
        RewardTransaction txn = new RewardTransaction();
        txn.setUser(account.getUser());
        txn.setPoints(adjustedPoints);
        txn.setTransactionType("ADMIN_ADJUSTMENT");
        txn.setBalanceAfter(account.getPointsBalance());
        txn.setDescription(reason);
        txn.setPerformedBy(admin.getEmail());
        txn.setIdempotencyKey("ADMIN_" + targetUserId + "_" + System.currentTimeMillis());
        txn = transactionRepo.save(txn);

        logger.info("Admin {} adjusted {} points for user {}: {}",
                admin.getEmail(), adjustedPoints, targetUserId, reason);
        return txn;
    }

    // ─── Transactions ───────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RewardTransaction> getTransactions(Long userId, String type, int page, int size) {
        if (type != null && !type.isBlank()) {
            return transactionRepo.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(page, size));
        }
        return transactionRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    // ─── Tier Calculation ───────────────────────────────────

    private String calculateTier(long qualifyingPoints, RewardConfig config) {
        if (qualifyingPoints >= config.getPlatinumTierThreshold()) return "PLATINUM";
        if (qualifyingPoints >= config.getGoldTierThreshold()) return "GOLD";
        return "SILVER";
    }

    private double getTierMultiplier(String tier, RewardConfig config) {
        return switch (tier) {
            case "PLATINUM" -> config.getPlatinumEarningMultiplier();
            case "GOLD" -> config.getGoldEarningMultiplier();
            default -> 1.0;
        };
    }

    // ─── DTOs ───────────────────────────────────────────────

    public Map<String, Object> toAccountDto(RewardAccount account) {
        RewardConfig config = getConfig();
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", account.getId());
        dto.put("pointsBalance", account.getPointsBalance());
        dto.put("lifetimePointsEarned", account.getLifetimePointsEarned());
        dto.put("lifetimePointsRedeemed", account.getLifetimePointsRedeemed());
        dto.put("lifetimePointsExpired", account.getLifetimePointsExpired());
        dto.put("tier", account.getTier());
        dto.put("qualifyingPoints", account.getQualifyingPoints());
        dto.put("nextTier", getNextTier(account.getTier()));
        dto.put("nextTierThreshold", getNextTierThreshold(account.getTier(), config));
        dto.put("pointsToNextTier", Math.max(0, getNextTierThreshold(account.getTier(), config) - account.getQualifyingPoints()));
        dto.put("tierMultiplier", getTierMultiplier(account.getTier(), config));
        dto.put("createdAt", account.getCreatedAt());
        return dto;
    }

    private String getNextTier(String current) {
        return switch (current) {
            case "SILVER" -> "GOLD";
            case "GOLD" -> "PLATINUM";
            default -> null;
        };
    }

    private long getNextTierThreshold(String current, RewardConfig config) {
        return switch (current) {
            case "SILVER" -> config.getGoldTierThreshold();
            case "GOLD" -> config.getPlatinumTierThreshold();
            default -> Long.MAX_VALUE;
        };
    }

    // ─── ResourceNotFoundException inner class ──────────────
    // Reuse the project's existing exception

    private static class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String resource, String field, Object value) {
            super(String.format("%s not found with %s: '%s'", resource, field, value));
        }
    }
}
