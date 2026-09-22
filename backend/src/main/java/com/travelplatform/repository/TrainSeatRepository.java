package com.travelplatform.repository;

import com.travelplatform.entity.TrainSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainSeatRepository extends JpaRepository<TrainSeat, Long> {
    List<TrainSeat> findByTrainId(Long trainId);
}
