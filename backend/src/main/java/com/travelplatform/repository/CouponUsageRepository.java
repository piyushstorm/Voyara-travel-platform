package com.travelplatform.repository;

import com.travelplatform.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponIdAndStatus(Long couponId, String status);
    long countByCouponIdAndUserIdAndStatus(Long couponId, Long userId, String status);
    
    @Query("SELECT COUNT(cu) FROM CouponUsage cu WHERE cu.coupon.id = :couponId AND cu.user.id = :userId AND cu.status IN ('PENDING', 'CONFIRMED')")
    long countActiveUsage(@Param("couponId") Long couponId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(cu) FROM CouponUsage cu WHERE cu.coupon.id = :couponId AND cu.status IN ('PENDING', 'CONFIRMED')")
    long countActiveGlobalUsage(@Param("couponId") Long couponId);
}
