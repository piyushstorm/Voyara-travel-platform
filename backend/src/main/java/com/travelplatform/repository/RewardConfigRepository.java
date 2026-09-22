package com.travelplatform.repository;

import com.travelplatform.entity.RewardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardConfigRepository extends JpaRepository<RewardConfig, Long> {
    Optional<RewardConfig> findFirstByOrderByIdAsc();
}
