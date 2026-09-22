package com.travelplatform.config.seed;

import com.travelplatform.entity.Airline;
import com.travelplatform.entity.Airport;
import com.travelplatform.repository.AirlineRepository;
import com.travelplatform.repository.AirportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class AirportAndAirlineSeeder {

    private static final Logger log = LoggerFactory.getLogger(AirportAndAirlineSeeder.class);

    private final AirportRepository airportRepo;
    private final AirlineRepository airlineRepo;

    public AirportAndAirlineSeeder(AirportRepository airportRepo, AirlineRepository airlineRepo) {
        this.airportRepo = airportRepo;
        this.airlineRepo = airlineRepo;
    }

    @Transactional
    public Map<String, Airport> seedAirports() {
        Map<String, Airport> airportMap = new HashMap<>();
        List<Airport> existingAirports = airportRepo.findAll();
        for (Airport a : existingAirports) {
            airportMap.put(a.getCode(), a);
        }

        List<Airport> toSave = new ArrayList<>();
        for (SeedConstants.AirportDef def : SeedConstants.AIRPORTS) {
            if (!airportMap.containsKey(def.code())) {
                Airport airport = new Airport(def.code(), def.name(), def.city(), def.country(), def.latitude(), def.longitude());
                toSave.add(airport);
            }
        }

        if (!toSave.isEmpty()) {
            List<Airport> saved = airportRepo.saveAll(toSave);
            for (Airport a : saved) {
                airportMap.put(a.getCode(), a);
            }
            log.info("Seeded {} new airports (total: {})", toSave.size(), airportMap.size());
        } else {
            log.info("Airports already up to date ({})", airportMap.size());
        }

        return airportMap;
    }

    @Transactional
    public List<Airline> seedAirlines() {
        Map<String, Airline> airlineMap = new HashMap<>();
        for (Airline a : airlineRepo.findAll()) {
            airlineMap.put(a.getCode(), a);
        }

        List<Airline> toSave = new ArrayList<>();
        for (SeedConstants.AirlineDef def : SeedConstants.AIRLINES) {
            if (!airlineMap.containsKey(def.code())) {
                Airline airline = new Airline(def.code(), def.name(), def.logoUrl(), def.rating());
                toSave.add(airline);
            }
        }

        if (!toSave.isEmpty()) {
            List<Airline> saved = airlineRepo.saveAll(toSave);
            for (Airline a : saved) {
                airlineMap.put(a.getCode(), a);
            }
            log.info("Seeded {} new airlines (total: {})", toSave.size(), airlineMap.size());
        } else {
            log.info("Airlines already up to date ({})", airlineMap.size());
        }

        return new ArrayList<>(airlineMap.values());
    }
}
