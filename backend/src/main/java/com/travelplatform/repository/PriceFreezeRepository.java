package com.travelplatform.repository;

import com.travelplatform.entity.PriceFreeze;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceFreezeRepository extends JpaRepository<PriceFreeze, Long> {
    List<PriceFreeze> findByUserIdAndStatus(Long userId, String status);
    List<PriceFreeze> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PriceFreeze> findByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, String status);
    Optional<PriceFreeze> findByUserIdAndEntityTypeAndEntityIdAndStatus(Long userId, String entityType, Long entityId, String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PriceFreeze f SET f.status = 'USED' WHERE f.id = :freezeId AND f.user.id = :userId AND f.status = 'ACTIVE' AND f.expiresAt > :now")
    int consumeFreezeAtomically(@Param("freezeId") Long freezeId,
                                @Param("userId") Long userId,
                                @Param("now") LocalDateTime now);
}
