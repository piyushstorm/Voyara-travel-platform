package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_expense_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"expense_id", "user_id"})
})
public class TripExpenseParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private TripExpense expense;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal sharePercentage;

    @Column(nullable = false)
    private boolean isSettled = false;

    private LocalDateTime settledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public TripExpenseParticipant() {}

    public Long getId() { return id; }
    public TripExpense getExpense() { return expense; }
    public void setExpense(TripExpense expense) { this.expense = expense; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal amount) { this.shareAmount = amount; }
    public BigDecimal getSharePercentage() { return sharePercentage; }
    public void setSharePercentage(BigDecimal pct) { this.sharePercentage = pct; }
    public boolean isSettled() { return isSettled; }
    public void setSettled(boolean settled) { isSettled = settled; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime t) { this.settledAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
