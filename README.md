# ✈️ Voyara — Travel Booking Platform

A production-oriented full-stack travel booking platform designed to provide a unified experience for discovering, booking, managing, and tracking travel services.

Voyara brings flights, hotels, travel planning, group trips, payments, rewards, reviews, recommendations, and travel assistance into a single platform.

---

## 🌍 Overview

Voyara is built as a modern full-stack travel platform with a React frontend and Spring Boot backend.

The platform supports:

- Flight discovery and booking
- Hotel discovery and booking
- Travel booking management
- Secure authentication and authorization
- Real-time flight tracking
- Dynamic pricing
- Interactive seat and room selection
- Cancellation and refund management
- Group trips and travel companions
- Reviews and ratings
- Personalized recommendations
- Loyalty and rewards
- Notifications
- Admin management and analytics
- Secure online payments through Razorpay

The application is designed with a backend-authoritative architecture so that critical operations such as pricing, inventory, booking, payment verification, authorization, and refunds are validated server-side.

---

# 🚀 Key Features

## 🔐 Authentication & Authorization

- Email/password registration and login
- JWT-based authentication
- Access and refresh token support
- Role-based authorization
- USER and ADMIN roles
- Password reset
- Email verification
- Google authentication
- Mobile OTP authentication
- Protected frontend routes
- Backend authorization enforcement
- Rate limiting for authentication endpoints

---

## ✈️ Flight Search & Booking

- Search flights by origin and destination
- Future-date validation
- Airline filtering
- Price filtering
- Stop filtering
- Departure/arrival time filtering
- Sorting
- Flight details
- Fare breakdown
- Seat selection
- Premium seat pricing
- Booking creation
- Booking management

---

## 🏨 Hotel Search & Booking

- Hotel discovery
- Destination-based search
- Price filtering
- Star-rating filtering
- Amenity filtering
- Sorting
- Room-type selection
- Room availability
- Room pricing
- Booking management
- Hotel reviews and ratings

---

## 💺 Interactive Seat Selection

Voyara provides an interactive seat-selection experience for supported flights.

Features include:

- Visual seat map
- Available/unavailable states
- Seat holds
- Concurrent booking protection
- Premium seat pricing
- Backend-authoritative seat availability
- Automatic fare recalculation
- Selected-seat persistence during booking

Example:

```text
Base Fare          ₹6,433
Premium Seat 12A   ₹750
-------------------------
Total              ₹7,183
