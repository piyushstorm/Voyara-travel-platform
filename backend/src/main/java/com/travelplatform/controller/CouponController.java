package com.travelplatform.controller;

import com.travelplatform.entity.Coupon;
import com.travelplatform.entity.AuditLog;
import com.travelplatform.repository.CouponRepository;
import com.travelplatform.repository.AuditLogRepository;
import com.travelplatform.service.CouponService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CouponController {

    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final AuditLogRepository auditLogRepository;

    public CouponController(CouponRepository couponRepository, CouponService couponService, AuditLogRepository auditLogRepository) {
        this.couponRepository = couponRepository;
        this.couponService = couponService;
        this.auditLogRepository = auditLogRepository;
    }

    // ==================== PUBLIC: Validate coupon ====================

    @PostMapping("/coupons/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody Map<String, Object> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        String code = body.get("code") != null ? body.get("code").toString().toUpperCase().trim() : null;
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String module = (String) body.getOrDefault("module", "ALL");
        String userEmail = principal != null ? principal.getUsername() : null;

        Map<String, Object> result = couponService.validateAndApplyCoupon(code, amount, module, userEmail);
        int status = (boolean) result.get("valid") ? 200 : 400;
        return ResponseEntity.status(status).body(result);
    }

    // ==================== ADMIN CRUD ====================

    @GetMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        Page<Coupon> coupons = couponRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(Map.of("success", true, "data", coupons));
    }

    @GetMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        return ResponseEntity.ok(Map.of("success", true, "data", coupon));
    }

    @PostMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createCoupon(@RequestBody Coupon coupon,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        // Normalize code to uppercase
        coupon.setCode(coupon.getCode().toUpperCase().trim());
        if (couponRepository.existsByCode(coupon.getCode())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Coupon code already exists"));
        }
        Coupon saved = couponRepository.save(coupon);
        auditLogRepository.save(new AuditLog("COUPON_CREATED", principal.getUsername(), saved.getId(), saved.getCode(), "N/A", "ACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PatchMapping("/admin/coupons/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleCouponStatus(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setActive(!coupon.isActive());
        Coupon saved = couponRepository.save(coupon);
        auditLogRepository.save(new AuditLog("COUPON_TOGGLED", principal.getUsername(), saved.getId(), saved.getCode(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", saved.getId(), "active", saved.isActive())));
    }

    @PutMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCoupon(@PathVariable Long id, @RequestBody Coupon updates,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setCode(updates.getCode());
        coupon.setDescription(updates.getDescription());
        coupon.setDiscountType(updates.getDiscountType());
        coupon.setDiscountValue(updates.getDiscountValue());
        coupon.setMaxDiscount(updates.getMaxDiscount());
        coupon.setMinBookingAmount(updates.getMinBookingAmount());
        coupon.setStartDate(updates.getStartDate());
        coupon.setExpiryDate(updates.getExpiryDate());
        coupon.setUsageLimit(updates.getUsageLimit());
        coupon.setPerUserLimit(updates.getPerUserLimit());
        coupon.setActive(updates.isActive());
        coupon.setApplicableModule(updates.getApplicableModule());
        Coupon saved = couponRepository.save(coupon);
        auditLogRepository.save(new AuditLog("COUPON_UPDATED", principal.getUsername(), saved.getId(), saved.getCode(), "N/A", saved.isActive() ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @DeleteMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCoupon(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        if (coupon.getUsedCount() > 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot delete a coupon that has been used. Deactivate it instead."));
        }
        couponRepository.delete(coupon);
        auditLogRepository.save(new AuditLog("COUPON_DELETED", principal.getUsername(), id, coupon.getCode(), "ACTIVE", "DELETED"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Coupon deleted"));
    }
}
