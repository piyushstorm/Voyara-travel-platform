package com.travelplatform.dto.booking;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BookingRequest {

    @NotNull(message = "Booking type is required")
    private String bookingType;  // FLIGHT, HOTEL, HOLIDAY

    // Flight booking
    private Long flightId;
    private String cabinClass = "ECONOMY";
    private int passengerCount = 1;
    private String seatNumbers;  // comma-separated
    private List<Long> seatIds;  // list of seat IDs to confirm
    private Long fareOptionId;  // Optional: selected fare tier (Saver/Standard/Flex)
    private List<Long> addOnIds;  // Optional: selected add-on IDs
    private List<PassengerInfo> passengers;  // Optional: per-passenger details

    // Hotel booking
    private Long hotelId;
    private Long roomId;
    private String checkInDate;
    private String checkOutDate;
    private int numberOfNights = 1;
    private String specialRequests;

    // Holiday booking
    private Long holidayPackageId;
    private int numberOfTravellers = 1;

    // Train booking
    private Long trainId;
    private String trainClass = "SL";
    private String boardingPoint;
    private String droppingPoint;

    // Bus booking
    private Long busId;

    // Cab booking
    private Long cabId;
    private String cabType = "LOCAL";
    private String pickup;
    private String dropLocation;

    // Price freeze
    private Long freezeId;  // Optional: apply a price freeze to this booking

    // Payment
    @NotNull(message = "Payment method is required")
    private String paymentMethod;  // CARD, UPI, NET_BANKING
    private String couponCode;  // Optional: applied promo code
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private boolean simulateFailure = false;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String paymentId;

    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }
    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public String getSeatNumbers() { return seatNumbers; }
    public void setSeatNumbers(String seatNumbers) { this.seatNumbers = seatNumbers; }
    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }
    public Long getFareOptionId() { return fareOptionId; }
    public void setFareOptionId(Long fareOptionId) { this.fareOptionId = fareOptionId; }
    public List<Long> getAddOnIds() { return addOnIds; }
    public void setAddOnIds(List<Long> addOnIds) { this.addOnIds = addOnIds; }
    public List<PassengerInfo> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerInfo> passengers) { this.passengers = passengers; }
    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }
    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }
    public int getNumberOfNights() { return numberOfNights; }
    public void setNumberOfNights(int numberOfNights) { this.numberOfNights = numberOfNights; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    public Long getHolidayPackageId() { return holidayPackageId; }
    public void setHolidayPackageId(Long id) { this.holidayPackageId = id; }
    public int getNumberOfTravellers() { return numberOfTravellers; }
    public void setNumberOfTravellers(int n) { this.numberOfTravellers = n; }
    public Long getTrainId() { return trainId; }
    public void setTrainId(Long trainId) { this.trainId = trainId; }
    public String getTrainClass() { return trainClass; }
    public void setTrainClass(String trainClass) { this.trainClass = trainClass; }
    public String getBoardingPoint() { return boardingPoint; }
    public void setBoardingPoint(String boardingPoint) { this.boardingPoint = boardingPoint; }
    public String getDroppingPoint() { return droppingPoint; }
    public void setDroppingPoint(String droppingPoint) { this.droppingPoint = droppingPoint; }
    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public Long getCabId() { return cabId; }
    public void setCabId(Long cabId) { this.cabId = cabId; }
    public String getCabType() { return cabType; }
    public void setCabType(String cabType) { this.cabType = cabType; }
    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }
    public String getDropLocation() { return dropLocation; }
    public void setDropLocation(String dropLocation) { this.dropLocation = dropLocation; }
    public Long getFreezeId() { return freezeId; }
    public void setFreezeId(Long freezeId) { this.freezeId = freezeId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }
    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
    public boolean isSimulateFailure() { return simulateFailure; }
    public void setSimulateFailure(boolean simulateFailure) { this.simulateFailure = simulateFailure; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    /** Nested passenger info class */
    public static class PassengerInfo {
        private String title;  // Mr, Mrs, Ms, Dr
        private String firstName;
        private String middleName;
        private String lastName;
        private String gender;  // MALE, FEMALE, OTHER
        private String dateOfBirth;  // YYYY-MM-DD
        private String nationality;
        private String email;
        private String phone;
        private String passportNumber;
        private String passportExpiry;
        private String passportCountry;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getMiddleName() { return middleName; }
        public void setMiddleName(String middleName) { this.middleName = middleName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassportNumber() { return passportNumber; }
        public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
        public String getPassportExpiry() { return passportExpiry; }
        public void setPassportExpiry(String passportExpiry) { this.passportExpiry = passportExpiry; }
        public String getPassportCountry() { return passportCountry; }
        public void setPassportCountry(String passportCountry) { this.passportCountry = passportCountry; }
    }
}
