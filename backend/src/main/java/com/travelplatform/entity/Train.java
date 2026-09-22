package com.travelplatform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String trainNumber;

    @Column(nullable = false)
    private String trainName;

    private String trainType; // SUPERFAST, EXPRESS, RAJDHANI, SHATABDI, DURONTO, MAIL, PASSENGER
    private String originCode;
    private String originName;
    private String originCity;
    private String destinationCode;
    private String destinationName;
    private String destinationCity;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    private Integer durationMinutes;
    private String runningDays; // MON,TUE,WED,THU,FRI,SAT,SUN
    private Boolean active;

    @Column(columnDefinition = "TEXT")
    private String intermediateStations; // JSON array

    @Column(columnDefinition = "TEXT")
    private String classes; // JSON array of {classCode, className, available, fare}

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("train")
    private List<TrainSeat> seats = new ArrayList<>();

    public Train() {
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }
    public String getTrainType() { return trainType; }
    public void setTrainType(String trainType) { this.trainType = trainType; }
    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }
    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }
    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }
    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getRunningDays() { return runningDays; }
    public void setRunningDays(String runningDays) { this.runningDays = runningDays; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getIntermediateStations() { return intermediateStations; }
    public void setIntermediateStations(String intermediateStations) { this.intermediateStations = intermediateStations; }
    public String getClasses() { return classes; }
    public void setClasses(String classes) { this.classes = classes; }
    public List<TrainSeat> getSeats() { return seats; }
    public void setSeats(List<TrainSeat> seats) { this.seats = seats; }
}
