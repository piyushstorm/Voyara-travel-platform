package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_companions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_trip_id", "user_id"})
})
public class TravelCompanion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String role = "COMPANION";

    @Column(nullable = false, length = 20)
    private String status = "ACCEPTED";

    private LocalDateTime joinedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public TravelCompanion() {}

    public Long getId() { return id; }
    public GroupTrip getGroupTrip() { return groupTrip; }
    public void setGroupTrip(GroupTrip groupTrip) { this.groupTrip = groupTrip; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
