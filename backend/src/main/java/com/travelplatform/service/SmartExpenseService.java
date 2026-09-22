package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Expense Split — group travel expense tracking and settlement calculation.
 * Does NOT create fake payment transfers — purely an accounting/settlement feature.
 */
@Service
public class SmartExpenseService {

    private final TripExpenseRepository expenseRepo;
    private final TripExpenseParticipantRepository participantRepo;
    private final TripSettlementRepository settlementRepo;
    private final TravelCompanionRepository companionRepo;
    private final GroupTripRepository groupTripRepo;
    private final UserRepository userRepo;

    public SmartExpenseService(TripExpenseRepository expenseRepo,
                                TripExpenseParticipantRepository participantRepo,
                                TripSettlementRepository settlementRepo,
                                TravelCompanionRepository companionRepo,
                                GroupTripRepository groupTripRepo,
                                UserRepository userRepo) {
        this.expenseRepo = expenseRepo;
        this.participantRepo = participantRepo;
        this.settlementRepo = settlementRepo;
        this.companionRepo = companionRepo;
        this.groupTripRepo = groupTripRepo;
        this.userRepo = userRepo;
    }

    /**
     * Add an expense to a group trip.
     */
    @Transactional
    public TripExpense addExpense(Long tripId, Long paidByUserId, String description,
                                   BigDecimal amount, String expenseType,
                                   String splitMode, List<Long> participantUserIds,
                                   BigDecimal totalPercentage,
                                   List<BigDecimal> customAmounts) {
        GroupTrip trip = groupTripRepo.findById(tripId).orElseThrow();
        User paidBy = userRepo.findById(paidByUserId).orElseThrow();

        if (!companionRepo.existsByGroupTripIdAndUserId(tripId, paidByUserId)) {
            throw new RuntimeException("Only trip members can add expenses");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Expense amount must be greater than zero");
        }

        if (participantUserIds == null || participantUserIds.isEmpty()) {
            // Default to all companions
            participantUserIds = companionRepo.findByGroupTripId(tripId).stream()
                .map(c -> c.getUser().getId())
                .collect(Collectors.toList());
            if (!participantUserIds.contains(paidByUserId)) {
                participantUserIds.add(paidByUserId);
            }
        }

        participantUserIds = participantUserIds.stream().distinct().collect(Collectors.toList());
        if (participantUserIds.stream().anyMatch(userId -> !companionRepo.existsByGroupTripIdAndUserId(tripId, userId))) {
            throw new RuntimeException("All expense participants must be trip members");
        }

        TripExpense expense = new TripExpense();
        expense.setGroupTrip(trip);
        expense.setPaidByUser(paidBy);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setExpenseType(expenseType);
        expense.setSplitMode(splitMode);
        expense.setExpenseDate(LocalDate.now());
        expenseRepo.save(expense);

        // Calculate splits
        int participantCount = participantUserIds.size();

        switch (splitMode.toUpperCase()) {
            case "PERCENTAGE" -> {
                if (totalPercentage == null) totalPercentage = BigDecimal.valueOf(100);
                // Validate percentage totals to 100%
                if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
                    // When totalPercentage is not 100, use equal percentage split
                    BigDecimal perPerson = BigDecimal.valueOf(100).divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
                    for (Long userId : participantUserIds) {
                        User user = userRepo.findById(userId).orElseThrow();
                        TripExpenseParticipant p = new TripExpenseParticipant();
                        p.setExpense(expense);
                        p.setUser(user);
                        p.setSharePercentage(perPerson);
                        p.setShareAmount(amount.multiply(perPerson).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                        participantRepo.save(p);
                    }
                } else {
                    BigDecimal perPerson = totalPercentage.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
                    for (Long userId : participantUserIds) {
                        User user = userRepo.findById(userId).orElseThrow();
                        TripExpenseParticipant p = new TripExpenseParticipant();
                        p.setExpense(expense);
                        p.setUser(user);
                        p.setSharePercentage(perPerson);
                        p.setShareAmount(amount.multiply(perPerson).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                        participantRepo.save(p);
                    }
                }
            }
            case "CUSTOM_AMOUNT" -> {
                if (customAmounts != null && customAmounts.size() == participantCount) {
                    // Validate total matches expense amount
                    BigDecimal totalCustom = customAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (totalCustom.compareTo(amount) != 0) {
                        throw new RuntimeException("Custom amounts total (" + totalCustom + ") does not match expense amount (" + amount + ")");
                    }
                    for (int i = 0; i < participantCount; i++) {
                        Long userId = participantUserIds.get(i);
                        User user = userRepo.findById(userId).orElseThrow();
                        TripExpenseParticipant p = new TripExpenseParticipant();
                        p.setExpense(expense);
                        p.setUser(user);
                        p.setShareAmount(customAmounts.get(i));
                        p.setSharePercentage(customAmounts.get(i).multiply(BigDecimal.valueOf(100)).divide(amount, 2, RoundingMode.HALF_UP));
                        participantRepo.save(p);
                    }
                } else {
                    // Fallback to equal split
                    BigDecimal perPerson = amount.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
                    for (Long userId : participantUserIds) {
                        User user = userRepo.findById(userId).orElseThrow();
                        TripExpenseParticipant p = new TripExpenseParticipant();
                        p.setExpense(expense);
                        p.setUser(user);
                        p.setShareAmount(perPerson);
                        p.setSharePercentage(BigDecimal.valueOf(100).divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP));
                        participantRepo.save(p);
                    }
                }
            }
            default -> { // EQUAL
                BigDecimal perPerson = amount.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
                for (Long userId : participantUserIds) {
                    User user = userRepo.findById(userId).orElseThrow();
                    TripExpenseParticipant p = new TripExpenseParticipant();
                    p.setExpense(expense);
                    p.setUser(user);
                    p.setShareAmount(perPerson);
                    p.setSharePercentage(BigDecimal.valueOf(100).divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP));
                    participantRepo.save(p);
                }
            }
        }

        return expense;
    }

    /**
     * Get all expenses for a group trip.
     */
    @Transactional(readOnly = true)
    public List<TripExpense> getExpenses(Long tripId) {
        return expenseRepo.findByGroupTripIdOrderByCreatedAtDesc(tripId);
    }

    /**
     * Calculate settlements — who owes whom.
     * Uses greedy algorithm for minimal transfers.
     */
    @Transactional
    public List<TripSettlement> calculateSettlements(Long tripId) {
        List<TripExpense> expenses = expenseRepo.findByGroupTripIdOrderByCreatedAtDesc(tripId);

        settlementRepo.deleteByGroupTripIdAndStatus(tripId, "PENDING");

        // Calculate net balance for each user
        Map<Long, BigDecimal> balances = new HashMap<>();

        for (TripExpense expense : expenses) {
            Long payerId = expense.getPaidByUser().getId();
            List<TripExpenseParticipant> participants = participantRepo.findByExpenseId(expense.getId());

            // Credit payer once with the full expense amount they fronted
            balances.merge(payerId, expense.getAmount(), BigDecimal::add);

            // Deduct each participant's share (including the payer if they are a participant)
            for (TripExpenseParticipant p : participants) {
                balances.merge(p.getUser().getId(), p.getShareAmount().negate(), BigDecimal::add);
            }
        }

        // Separate into debtors and creditors
        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry);
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry);
            }
        }

        // Sort for greedy algorithm
        debtors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<TripSettlement> settlements = new ArrayList<>();
        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            Long debtorId = debtors.get(i).getKey();
            BigDecimal debtorAmount = debtors.get(i).getValue().abs();
            Long creditorId = creditors.get(j).getKey();
            BigDecimal creditorAmount = creditors.get(j).getValue();

            BigDecimal settleAmount = debtorAmount.min(creditorAmount);

            if (settleAmount.compareTo(BigDecimal.ZERO) > 0) {
                TripSettlement settlement = new TripSettlement();
                settlement.setGroupTrip(groupTripRepo.findById(tripId).orElseThrow());
                settlement.setFromUser(userRepo.findById(debtorId).orElseThrow());
                settlement.setToUser(userRepo.findById(creditorId).orElseThrow());
                settlement.setAmount(settleAmount);
                settlement.setStatus("PENDING");
                settlementRepo.save(settlement);
                settlements.add(settlement);
            }

            // Update remaining amounts
            debtors.set(i, Map.entry(debtorId, debtorAmount.subtract(settleAmount).negate()));
            creditors.set(j, Map.entry(creditorId, creditorAmount.subtract(settleAmount)));

            if (debtors.get(i).getValue().abs().compareTo(BigDecimal.ZERO) <= 0) i++;
            if (creditors.get(j).getValue().compareTo(BigDecimal.ZERO) <= 0) j++;
        }

        return settlements;
    }

    /**
     * Get summary for a group trip.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExpenseSummary(Long tripId) {
        List<TripExpense> expenses = expenseRepo.findByGroupTripIdOrderByCreatedAtDesc(tripId);
        List<TripSettlement> settlements = settlementRepo.findByGroupTripIdAndStatusOrderByCreatedAtDesc(tripId, "PENDING");

        BigDecimal totalExpenses = expenses.stream()
            .map(TripExpense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate per-user totals
        Map<Long, BigDecimal> paidByUser = new HashMap<>();
        for (TripExpense e : expenses) {
            paidByUser.merge(e.getPaidByUser().getId(), e.getAmount(), BigDecimal::add);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalExpenses", totalExpenses);
        summary.put("expenseCount", expenses.size());
        summary.put("pendingSettlements", settlements.size());
        summary.put("paidByUser", paidByUser);
        summary.put("settlements", settlements);

        return summary;
    }

    /**
     * Mark a settlement as settled.
     */
    @Transactional
    public TripSettlement markSettled(Long settlementId, Long userId) {
        TripSettlement settlement = settlementRepo.findById(settlementId).orElseThrow();
        // Only the person who owes can mark as settled
        if (!settlement.getFromUser().getId().equals(userId)) {
            throw new RuntimeException("Only the debtor can mark a settlement as settled");
        }
        settlement.setStatus("SETTLED");
        settlement.setSettledAt(LocalDateTime.now());
        return settlementRepo.save(settlement);
    }

    /**
     * Delete an expense (only by the person who added it).
     */
    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        TripExpense expense = expenseRepo.findById(expenseId).orElseThrow();
        if (!expense.getPaidByUser().getId().equals(userId)) {
            throw new RuntimeException("Only the person who added the expense can delete it");
        }
        expenseRepo.delete(expense);
    }
}
