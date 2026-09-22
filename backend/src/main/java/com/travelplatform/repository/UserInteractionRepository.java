package com.travelplatform.repository;

import com.travelplatform.entity.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
    List<UserInteraction> findByUserId(Long userId);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.user.id = :userId AND ui.interactionType IN ('BOOKED', 'RATED', 'REVIEWED')")
    List<UserInteraction> findPositiveInteractions(@Param("userId") Long userId);

    @Query("SELECT ui.entityId FROM UserInteraction ui WHERE ui.user.id = :userId AND ui.entityType = :entityType AND ui.interactionType IN ('BOOKED', 'RATED', 'SAVED')")
    List<Long> findEntityIdsByUserIdAndType(@Param("userId") Long userId, @Param("entityType") String entityType);

    @Query("SELECT ui.user.id FROM UserInteraction ui WHERE ui.entityId = :entityId AND ui.entityType = :entityType AND ui.interactionType = 'BOOKED' AND ui.user.id <> :excludeUserId")
    List<Long> findUserIdsWhoBookedSameEntity(@Param("entityId") Long entityId, @Param("entityType") String entityType, @Param("excludeUserId") Long excludeUserId);
}
