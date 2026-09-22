package com.travelplatform.repository;

import com.travelplatform.entity.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, Long> {

    List<AddOn> findByActiveTrue();

    List<AddOn> findByActiveTrueAndCategory(String category);

    List<AddOn> findByActiveTrueAndCabinClassIsNullOrCabinClass(String cabinClass);
}
