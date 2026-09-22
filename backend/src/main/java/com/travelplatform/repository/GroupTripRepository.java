package com.travelplatform.repository;

import com.travelplatform.entity.GroupTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupTripRepository extends JpaRepository<GroupTrip, Long> {
    List<GroupTrip> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<GroupTrip> findByCompanionsUserIdOrderByCreatedAtDesc(Long userId);
}
