package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Predefined cancellation reasons for Voyara bookings.
 * Used for backend validation, user selection, and administrative trend analysis.
 */
public enum CancellationReason {
    CHANGE_OF_PLANS("Change of plans", "Personal change in travel plans"),
    TRAVEL_DATE_CHANGED("Travel dates changed", "Dates shifted or rescheduled"),
    FOUND_BETTER_PRICE("Found a better deal", "Found better pricing elsewhere"),
    BOOKED_BY_MISTAKE("Booked by mistake", "Accidental booking or incorrect details"),
    MEDICAL_EMERGENCY("Medical emergency", "Medical issue or health concern"),
    PERSONAL_REASONS("Personal reasons", "Personal or family emergency"),
    VISA_ISSUE("Visa or travel document issue", "Visa rejection or delay"),
    FLIGHT_OR_TRANSPORT_ISSUE("Transport schedule issue", "Carrier cancellation or drastic schedule shift"),
    DUPLICATE_BOOKING("Duplicate booking", "Booked the same itinerary twice"),
    OTHER("Other", "Other unforeseen reasons");

    private final String label;
    private final String description;

    CancellationReason(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Tolerant creator mapping enum codes or user-friendly labels (including legacy strings)
     */
    @JsonCreator
    public static CancellationReason fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String clean = text.trim();
        for (CancellationReason r : CancellationReason.values()) {
            if (r.name().equalsIgnoreCase(clean)) {
                return r;
            }
            if (r.label.equalsIgnoreCase(clean)) {
                return r;
            }
        }
        // Legacy string mappings
        String lower = clean.toLowerCase();
        if (lower.contains("change") && lower.contains("plan")) return CHANGE_OF_PLANS;
        if (lower.contains("date")) return TRAVEL_DATE_CHANGED;
        if (lower.contains("deal") || lower.contains("price")) return FOUND_BETTER_PRICE;
        if (lower.contains("mistake") || lower.contains("accident")) return BOOKED_BY_MISTAKE;
        if (lower.contains("medical") || lower.contains("health") || lower.contains("emergency")) return MEDICAL_EMERGENCY;
        if (lower.contains("personal")) return PERSONAL_REASONS;
        if (lower.contains("visa") || lower.contains("passport")) return VISA_ISSUE;
        if (lower.contains("flight") || lower.contains("transport") || lower.contains("schedule")) return FLIGHT_OR_TRANSPORT_ISSUE;
        if (lower.contains("duplicate") || lower.contains("twice")) return DUPLICATE_BOOKING;
        if (lower.contains("other")) return OTHER;

        throw new IllegalArgumentException("Unknown cancellation reason: " + text);
    }
}
