package com.travelplatform.repository;

import com.travelplatform.entity.Cab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CabRepository extends JpaRepository<Cab, Long> {
    List<Cab> findByActiveTrue();
}
