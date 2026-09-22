package com.travelplatform.repository;

import com.travelplatform.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {
    List<CancellationPolicy> findByEntityTypeOrderByMaxHoursBeforeDesc(String entityType);
}
