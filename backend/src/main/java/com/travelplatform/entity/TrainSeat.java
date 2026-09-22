package com.travelplatform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "train_seats")
public class TrainSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false)
    private String classCode; // SL, 3A, 2A, 1A, CC, EC

    @Column(nullable = false)
    private String className; // Sleeper, 3-Tier AC, 2-Tier AC, 1st AC, Chair Car, Executive Chair Car

    private Integer totalSeats;
    private Integer availableSeats;
    private Integer racSeats;
    private Integer waitlistCount;

    @Column(nullable = false)
    private BigDecimal fare;

    private String status; // AVAILABLE, RAC, WAITLIST, NOT_AVAILABLE

    public TrainSeat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    public Integer getRacSeats() { return racSeats; }
    public void setRacSeats(Integer racSeats) { this.racSeats = racSeats; }
    public Integer getWaitlistCount() { return waitlistCount; }
    public void setWaitlistCount(Integer waitlistCount) { this.waitlistCount = waitlistCount; }
    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
