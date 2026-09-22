package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_expenses")
public class TripExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    private User paidByUser;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    private String expenseType;

    @Column(nullable = false, length = 20)
    private String splitMode = "EQUAL";

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private LocalDate expenseDate;

    @JsonIgnore
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripExpenseParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public TripExpense() {}

    public Long getId() { return id; }
    public GroupTrip getGroupTrip() { return groupTrip; }
    public void setGroupTrip(GroupTrip groupTrip) { this.groupTrip = groupTrip; }
    public User getPaidByUser() { return paidByUser; }
    public void setPaidByUser(User user) { this.paidByUser = user; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String type) { this.expenseType = type; }
    public String getSplitMode() { return splitMode; }
    public void setSplitMode(String mode) { this.splitMode = mode; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate date) { this.expenseDate = date; }
    public List<TripExpenseParticipant> getParticipants() { return participants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
