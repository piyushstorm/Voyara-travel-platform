package com.travelplatform.repository;

import com.travelplatform.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {

    @Query("SELECT t FROM Train t WHERE t.active = true ORDER BY t.departureTime ASC")
    List<Train> findAllActive();

    List<Train> findByTrainNumber(String trainNumber);
}
