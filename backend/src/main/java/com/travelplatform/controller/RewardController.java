package com.travelplatform.controller;

import com.travelplatform.entity.RewardAccount;
import com.travelplatform.entity.RewardConfig;
import com.travelplatform.entity.RewardTransaction;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.RewardService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardService rewardService;
    private final UserRepository userRepository;

    public RewardController(RewardService rewardService, UserRepository userRepository) {
        this.rewardService = rewardService;
        this.userRepository = userRepository;
    }

    // ─── Customer Endpoints ─────────────────────────────────

    /** Get current user's rewards account summary */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRewards(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        RewardAccount account = rewardService.getAccount(userId);
        return ResponseEntity.ok(rewardService.toAccountDto(account));
    }

    /** Get points balance */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        RewardAccount account = rewardService.getAccount(userId);
        return ResponseEntity.ok(Map.of(
            "pointsBalance", account.getPointsBalance(),
            "tier", account.getTier()
        ));
    }

    /** Get current tier info */
    @GetMapping("/tier")
    public ResponseEntity<Map<String, Object>> getTier(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        RewardAccount account = rewardService.getAccount(userId);
        RewardConfig config = rewardService.getConfig();
        long nextThreshold = account.getTier().equals("PLATINUM")
                ? config.getPlatinumTierThreshold()
                : (account.getTier().equals("GOLD") ? config.getPlatinumTierThreshold() : config.getGoldTierThreshold());
        return ResponseEntity.ok(Map.of(
            "tier", account.getTier(),
            "qualifyingPoints", account.getQualifyingPoints(),
            "nextTier", account.getTier().equals("PLATINUM") ? "PLATINUM" :
                (account.getTier().equals("GOLD") ? "PLATINUM" : "GOLD"),
            "nextTierThreshold", nextThreshold,
            "pointsToNextTier", Math.max(0, nextThreshold - account.getQualifyingPoints())
        ));
    }

    /** Get tier benefits */
    @GetMapping("/benefits")
    public ResponseEntity<Map<String, Object>> getBenefits() {
        RewardConfig config = rewardService.getConfig();
        Map<String, Object> benefits = new HashMap<>();
        benefits.put("SILVER", Map.of(
            "name", "Silver",
            "earningRate", "1 point per ₹100",
            "multiplier", "1x",
            "benefits", new String[]{"Earn points on every booking", "Basic member support", "Points expiry in " + config.getPointsExpiryDays() + " days"}
        ));
        benefits.put("GOLD", Map.of(
            "name", "Gold",
            "threshold", config.getGoldTierThreshold() + " qualifying points",
            "earningRate", "1 point per ₹100",
            "multiplier", config.getGoldEarningMultiplier() + "x",
            "benefits", new String[]{"1.5x points on every booking", "Priority customer support", "Early access to deals"}
        ));
        benefits.put("PLATINUM", Map.of(
            "name", "Platinum",
            "threshold", config.getPlatinumTierThreshold() + " qualifying points",
            "earningRate", "1 point per ₹100",
            "multiplier", config.getPlatinumEarningMultiplier() + "x",
            "benefits", new String[]{"2x points on every booking", "Premium customer support", "Exclusive offers", "Free upgrades where available"}
        ));
        return ResponseEntity.ok(benefits);
    }

    /** Get transaction history */
    @GetMapping("/transactions")
    public ResponseEntity<Page<Map<String, Object>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        Page<RewardTransaction> txns = rewardService.getTransactions(userId, type, page, size);
        return ResponseEntity.ok(txns.map(this::toTransactionDto));
    }

    /** Validate redemption during checkout */
    @PostMapping("/redeem/validate")
    public ResponseEntity<Map<String, Object>> validateRedemption(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        long points = ((Number) body.get("points")).longValue();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return ResponseEntity.ok(rewardService.validateRedemption(userId, points, amount));
    }

    /** Get rewards config */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        RewardConfig config = rewardService.getConfig();
        return ResponseEntity.ok(Map.of(
            "pointsPerHundredRupees", config.getPointsPerHundredRupees(),
            "pointsPerRupeeRedemption", config.getPointsPerRupeeRedemption(),
            "goldTierThreshold", config.getGoldTierThreshold(),
            "platinumTierThreshold", config.getPlatinumTierThreshold(),
            "pointsExpiryDays", config.getPointsExpiryDays(),
            "minRedemptionPoints", config.getMinRedemptionPoints(),
            "maxRedemptionPercent", config.getMaxRedemptionPercent()
        ));
    }

    // ─── Admin Endpoints ────────────────────────────────────

    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<Map<String, Object>> adminGetUserRewards(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        RewardAccount account = rewardService.getAccount(userId);
        return ResponseEntity.ok(rewardService.toAccountDto(account));
    }

    @GetMapping("/admin/user/{userId}/transactions")
    public ResponseEntity<Page<Map<String, Object>>> adminGetUserTransactions(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RewardTransaction> txns = rewardService.getTransactions(userId, type, page, size);
        return ResponseEntity.ok(txns.map(this::toTransactionDto));
    }

    @PostMapping("/admin/user/{userId}/adjust")
    public ResponseEntity<Map<String, Object>> adminAdjustPoints(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        Long adminId = getUserId(userDetails);
        long points = ((Number) body.get("points")).longValue();
        String reason = (String) body.get("reason");
        Boolean isAdd = (Boolean) body.getOrDefault("isAdd", true);
        RewardTransaction txn = rewardService.adminAdjustPoints(adminId, userId, points, reason, isAdd);
        return ResponseEntity.ok(toTransactionDto(txn));
    }

    @PutMapping("/admin/config")
    public ResponseEntity<Map<String, Object>> adminUpdateConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RewardConfig updates) {
        RewardConfig config = rewardService.updateConfig(updates);
        return ResponseEntity.ok(Map.of(
            "message", "Configuration updated",
            "pointsPerHundredRupees", config.getPointsPerHundredRupees(),
            "goldTierThreshold", config.getGoldTierThreshold(),
            "platinumTierThreshold", config.getPlatinumTierThreshold()
        ));
    }

    // ─── Helpers ────────────────────────────────────────────

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> toTransactionDto(RewardTransaction txn) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", txn.getId());
        dto.put("points", txn.getPoints());
        dto.put("transactionType", txn.getTransactionType());
        dto.put("balanceAfter", txn.getBalanceAfter());
        dto.put("bookingReference", txn.getBookingReference() != null ? txn.getBookingReference() : "");
        dto.put("eligibleAmount", txn.getEligibleAmount() != null ? txn.getEligibleAmount() : BigDecimal.ZERO);
        dto.put("description", txn.getDescription() != null ? txn.getDescription() : "");
        dto.put("performedBy", txn.getPerformedBy() != null ? txn.getPerformedBy() : "");
        dto.put("createdAt", txn.getCreatedAt());
        return dto;
    }
}
