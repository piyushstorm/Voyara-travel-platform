package com.travelplatform.service;

import com.travelplatform.entity.Coupon;
import com.travelplatform.entity.CouponUsage;
import com.travelplatform.entity.User;
import com.travelplatform.repository.CouponRepository;
import com.travelplatform.repository.CouponUsageRepository;
import com.travelplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;

    public CouponService(CouponRepository couponRepository, CouponUsageRepository couponUsageRepository, UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Validate and apply a coupon to a booking amount.
     * Returns validation result with discount amount.
     */
    public Map<String, Object> validateAndApplyCoupon(String code, BigDecimal bookingAmount, String module, String userEmail) {
        Map<String, Object> result = new HashMap<>();

        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        if (coupon == null) {
            result.put("valid", false);
            result.put("message", "Coupon code not found");
            return result;
        }

        if (!coupon.isActive()) {
            result.put("valid", false);
            result.put("message", "This coupon is no longer active");
            return result;
        }

        if (LocalDateTime.now().isBefore(coupon.getStartDate())) {
            result.put("valid", false);
            result.put("message", "This coupon is not yet valid");
            return result;
        }

        if (LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            result.put("valid", false);
            result.put("message", "This coupon has expired");
            return result;
        }

        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            result.put("valid", false);
            result.put("message", "This coupon has reached its usage limit");
            return result;
        }

        // Check per-user usage
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                long userUsage = couponUsageRepository.countActiveUsage(coupon.getId(), user.getId());
                if (userUsage >= coupon.getPerUserLimit()) {
                    result.put("valid", false);
                    result.put("message", "You have reached the per-user usage limit for this coupon");
                    return result;
                }
            }
        }

        if (coupon.getMinBookingAmount() != null && bookingAmount.compareTo(coupon.getMinBookingAmount()) < 0) {
            result.put("valid", false);
            result.put("message", "Minimum booking amount is ₹" + coupon.getMinBookingAmount());
            return result;
        }

        if (!"ALL".equals(coupon.getApplicableModule()) && !coupon.getApplicableModule().equalsIgnoreCase(module)) {
            result.put("valid", false);
            result.put("message", "This coupon is not applicable for " + module + " bookings");
            return result;
        }

        BigDecimal discount = calculateDiscount(coupon, bookingAmount);

        // Prevent negative final amount
        BigDecimal finalAmount = bookingAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            discount = bookingAmount;
            finalAmount = BigDecimal.ZERO;
        }

        result.put("valid", true);
        result.put("couponId", coupon.getId());
        result.put("code", coupon.getCode());
        result.put("description", coupon.getDescription());
        result.put("discountType", coupon.getDiscountType());
        result.put("discountAmount", discount.setScale(2, RoundingMode.HALF_UP));
        result.put("originalAmount", bookingAmount);
        result.put("finalAmount", finalAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("message", "Coupon applied successfully");

        return result;
    }

    /**
     * Increment usage count after successful booking.
     */
    @Transactional
    public CouponUsage recordUsage(String code, String userEmail, Long bookingId, BigDecimal discountAmount) {
        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (coupon == null || user == null) return null;
        CouponUsage usage = new CouponUsage(coupon, user, null, discountAmount);
        usage.setStatus("CONFIRMED");
        couponUsageRepository.save(usage);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        return usage;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            BigDecimal discount = amount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
            return discount;
        } else {
            // FIXED discount
            BigDecimal discount = coupon.getDiscountValue();
            if (discount.compareTo(amount) > 0) {
                discount = amount;
            }
            return discount;
        }
    }
}
