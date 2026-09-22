package com.travelplatform.repository;

import com.travelplatform.entity.UserTravelPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTravelPreferenceRepository extends JpaRepository<UserTravelPreference, Long> {

    Optional<UserTravelPreference> findByUserId(Long userId);
}
