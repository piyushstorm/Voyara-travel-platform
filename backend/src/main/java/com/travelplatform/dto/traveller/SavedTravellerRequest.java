package com.travelplatform.dto.traveller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SavedTravellerRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100)
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100)
    private String lastName;

    private String gender;
    private String dateOfBirth;
    private String nationality;
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
    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public String getPassportExpiry() { return passportExpiry; }
    public void setPassportExpiry(String passportExpiry) { this.passportExpiry = passportExpiry; }
    public String getPassportCountry() { return passportCountry; }
    public void setPassportCountry(String passportCountry) { this.passportCountry = passportCountry; }
}
