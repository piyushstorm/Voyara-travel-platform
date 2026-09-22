package com.travelplatform.repository;

import com.travelplatform.entity.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {
    Optional<RewardAccount> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
