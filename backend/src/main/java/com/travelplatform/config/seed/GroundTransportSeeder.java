package com.travelplatform.config.seed;

import com.travelplatform.entity.Bus;
import com.travelplatform.entity.Cab;
import com.travelplatform.entity.Train;
import com.travelplatform.entity.TrainSeat;
import com.travelplatform.repository.BusRepository;
import com.travelplatform.repository.CabRepository;
import com.travelplatform.repository.TrainRepository;
import com.travelplatform.repository.TrainSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

@Component
public class GroundTransportSeeder {

    private static final Logger log = LoggerFactory.getLogger(GroundTransportSeeder.class);

    private final TrainRepository trainRepo;
    private final TrainSeatRepository trainSeatRepo;
    private final BusRepository busRepo;
    private final CabRepository cabRepo;

    public GroundTransportSeeder(TrainRepository trainRepo, TrainSeatRepository trainSeatRepo,
                                 BusRepository busRepo, CabRepository cabRepo) {
        this.trainRepo = trainRepo;
        this.trainSeatRepo = trainSeatRepo;
        this.busRepo = busRepo;
        this.cabRepo = cabRepo;
    }

    @Transactional
    public void seedTrains() {
        long currentCount = trainRepo.count();
        if (currentCount >= 200) {
            log.info("Trains already seeded ({}), skipping.", currentCount);
            return;
        }

        log.info("Seeding realistic Indian Railways network across 200+ trains...");

        record TrainRoute(String originCode, String originName, String originCity,
                          String destCode, String destName, String destCity,
                          int durationMins, String intermediate) {}

        List<TrainRoute> routes = List.of(
            new TrainRoute("NDLS", "New Delhi", "Delhi", "BCT", "Mumbai Central", "Mumbai", 940, "Mathura,Kota,Vadodara,Surat"),
            new TrainRoute("BCT", "Mumbai Central", "Mumbai", "NDLS", "New Delhi", "Delhi", 940, "Surat,Vadodara,Kota,Mathura"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "HWH", "Howrah", "Kolkata", 955, "Kanpur,Prayagraj,Mughal Sarai,Dhanbad"),
            new TrainRoute("HWH", "Howrah", "Kolkata", "NDLS", "New Delhi", "Delhi", 955, "Dhanbad,Mughal Sarai,Prayagraj,Kanpur"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "SBC", "KSR Bengaluru", "Bangalore", 1420, "Bhopal,Nagpur,Secunderabad"),
            new TrainRoute("SBC", "KSR Bengaluru", "Bangalore", "NDLS", "New Delhi", "Delhi", 1420, "Secunderabad,Nagpur,Bhopal"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "MAS", "Chennai Central", "Chennai", 1680, "Gwalior,Bhopal,Nagpur,Vijayawada"),
            new TrainRoute("MAS", "Chennai Central", "Chennai", "NDLS", "New Delhi", "Delhi", 1680, "Vijayawada,Nagpur,Bhopal,Gwalior"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "JP", "Jaipur Junction", "Jaipur", 270, "Gurgaon,Rewari,Alwar"),
            new TrainRoute("JP", "Jaipur Junction", "Jaipur", "NDLS", "New Delhi", "Delhi", 270, "Alwar,Rewari,Gurgaon"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "BSB", "Varanasi Junction", "Varanasi", 480, "Kanpur Central,Prayagraj"),
            new TrainRoute("BSB", "Varanasi Junction", "Varanasi", "NDLS", "New Delhi", "Delhi", 480, "Prayagraj,Kanpur Central"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "CDG", "Chandigarh", "Chandigarh", 210, "Panipat,Ambala Cantt"),
            new TrainRoute("CDG", "Chandigarh", "Chandigarh", "NDLS", "New Delhi", "Delhi", 210, "Ambala Cantt,Panipat"),
            new TrainRoute("NDLS", "New Delhi", "Delhi", "ASR", "Amritsar Junction", "Amritsar", 360, "Ambala,Ludhiana,Jalandhar"),
            new TrainRoute("ASR", "Amritsar Junction", "Amritsar", "NDLS", "New Delhi", "Delhi", 360, "Jalandhar,Ludhiana,Ambala"),
            new TrainRoute("CSMT", "Mumbai CSMT", "Mumbai", "PUNE", "Pune Junction", "Pune", 195, "Dadar,Thane,Kalyan,Lonavala"),
            new TrainRoute("PUNE", "Pune Junction", "Pune", "CSMT", "Mumbai CSMT", "Mumbai", 195, "Lonavala,Kalyan,Thane,Dadar"),
            new TrainRoute("BCT", "Mumbai Central", "Mumbai", "ADI", "Ahmedabad Junction", "Ahmedabad", 380, "Borivali,Surat,Vadodara"),
            new TrainRoute("ADI", "Ahmedabad Junction", "Ahmedabad", "BCT", "Mumbai Central", "Mumbai", 380, "Vadodara,Surat,Borivali"),
            new TrainRoute("CSMT", "Mumbai CSMT", "Mumbai", "MAO", "Madgaon Junction", "Goa", 540, "Panvel,Ratnagiri,Kankavli"),
            new TrainRoute("MAO", "Madgaon Junction", "Goa", "CSMT", "Mumbai CSMT", "Mumbai", 540, "Kankavli,Ratnagiri,Panvel"),
            new TrainRoute("SBC", "KSR Bengaluru", "Bangalore", "MAS", "Chennai Central", "Chennai", 300, "Bengaluru Cantt,Katpadi"),
            new TrainRoute("MAS", "Chennai Central", "Chennai", "SBC", "KSR Bengaluru", "Bangalore", 300, "Katpadi,Bengaluru Cantt"),
            new TrainRoute("SBC", "KSR Bengaluru", "Bangalore", "SC", "Secunderabad", "Hyderabad", 660, "Hindupur,Dharmavaram,Kurnool"),
            new TrainRoute("SC", "Secunderabad", "Hyderabad", "SBC", "KSR Bengaluru", "Bangalore", 660, "Kurnool,Dharmavaram,Hindupur"),
            new TrainRoute("MAS", "Chennai Central", "Chennai", "CBE", "Coimbatore Junction", "Coimbatore", 450, "Katpadi,Salem,Erode"),
            new TrainRoute("CBE", "Coimbatore Junction", "Coimbatore", "MAS", "Chennai Central", "Chennai", 450, "Erode,Salem,Katpadi")
        );

        String[] trainTypeTemplates = {"VANDE_BHARAT", "RAJDHANI", "SHATABDI", "DURONTO", "EXPRESS", "SUPERFAST", "GARIB_RATH", "MAIL"};
        Random rng = new Random(42);

        List<Train> trainsToSave = new ArrayList<>();
        int trainNumSeed = 12000;

        for (TrainRoute r : routes) {
            // Seed 8 trains per major route to exceed 220 trains
            for (int i = 0; i < 8; i++) {
                String type = trainTypeTemplates[(i + r.originCity().hashCode()) & 0x7FFFFFFF % trainTypeTemplates.length];
                String trainNumber = String.valueOf(trainNumSeed++);
                String trainName = r.originCity() + " " + r.destCity() + " " + type.replace('_', ' ');

                int depHour = (6 + (i * 2)) % 24;
                int depMin = (i * 15) % 60;
                LocalTime depTime = LocalTime.of(depHour, depMin);
                LocalTime arrTime = depTime.plusMinutes(r.durationMins());

                Train train = new Train();
                train.setTrainNumber(trainNumber);
                train.setTrainName(trainName);
                train.setTrainType(type);
                train.setOriginCode(r.originCode());
                train.setOriginName(r.originName());
                train.setOriginCity(r.originCity());
                train.setDestinationCode(r.destCode());
                train.setDestinationName(r.destName());
                train.setDestinationCity(r.destCity());
                train.setDepartureTime(depTime);
                train.setArrivalTime(arrTime);
                train.setDurationMinutes(r.durationMins());
                train.setRunningDays("MON,TUE,WED,THU,FRI,SAT,SUN");
                train.setIntermediateStations(r.intermediate());
                train.setActive(true);

                boolean isChair = type.equals("SHATABDI") || type.equals("VANDE_BHARAT");
                String classesJson = isChair ?
                    "[{\"code\":\"CC\",\"name\":\"AC Chair Car\",\"fare\":850},{\"code\":\"EC\",\"name\":\"Executive Chair Car\",\"fare\":1650}]" :
                    "[{\"code\":\"SL\",\"name\":\"Sleeper\",\"fare\":480},{\"code\":\"3A\",\"name\":\"3-Tier AC\",\"fare\":1350},{\"code\":\"2A\",\"name\":\"2-Tier AC\",\"fare\":1950},{\"code\":\"1A\",\"name\":\"First AC\",\"fare\":3400}]";
                train.setClasses(classesJson);

                trainsToSave.add(train);
            }
        }

        List<Train> savedTrains = trainRepo.saveAll(trainsToSave);
        log.info("Persisted {} trains. Now populating seat classes...", savedTrains.size());

        List<TrainSeat> seatBatch = new ArrayList<>();
        for (Train t : savedTrains) {
            boolean isChair = t.getTrainType().equals("SHATABDI") || t.getTrainType().equals("VANDE_BHARAT");
            if (isChair) {
                seatBatch.add(buildTrainSeat(t, "CC", "AC Chair Car", 78, 850, rng));
                seatBatch.add(buildTrainSeat(t, "EC", "Executive Chair Car", 28, 1650, rng));
            } else {
                seatBatch.add(buildTrainSeat(t, "SL", "Sleeper", 120, 480, rng));
                seatBatch.add(buildTrainSeat(t, "3A", "3-Tier AC", 64, 1350, rng));
                seatBatch.add(buildTrainSeat(t, "2A", "2-Tier AC", 46, 1950, rng));
                seatBatch.add(buildTrainSeat(t, "1A", "First AC", 24, 3400, rng));
            }

            if (seatBatch.size() >= 500) {
                trainSeatRepo.saveAll(seatBatch);
                seatBatch.clear();
            }
        }

        if (!seatBatch.isEmpty()) {
            trainSeatRepo.saveAll(seatBatch);
            seatBatch.clear();
        }

        log.info("Total train seats persisted: {}", trainSeatRepo.count());
    }

    private TrainSeat buildTrainSeat(Train train, String code, String name, int total, int fare, Random rng) {
        TrainSeat ts = new TrainSeat();
        ts.setTrain(train);
        ts.setClassCode(code);
        ts.setClassName(name);
        ts.setTotalSeats(total);
        int avail = rng.nextInt(total);
        ts.setAvailableSeats(avail);
        ts.setRacSeats(avail == 0 ? rng.nextInt(8) : 0);
        ts.setWaitlistCount(avail == 0 && ts.getRacSeats() == 0 ? rng.nextInt(15) : 0);
        ts.setFare(BigDecimal.valueOf(fare));
        ts.setStatus(avail > 0 ? "AVAILABLE" : (ts.getRacSeats() > 0 ? "RAC" : "WL"));
        return ts;
    }

    @Transactional
    public void seedBuses() {
        long currentCount = busRepo.count();
        if (currentCount >= 300) {
            log.info("Buses already seeded ({}), skipping.", currentCount);
            return;
        }

        log.info("Seeding realistic intercity bus network across 320+ buses...");

        record BusCorridor(String origin, String dest, int durationMins, int baseFare) {}

        List<BusCorridor> corridors = List.of(
            new BusCorridor("Mumbai", "Pune", 210, 450),
            new BusCorridor("Pune", "Mumbai", 210, 450),
            new BusCorridor("Mumbai", "Goa", 660, 1400),
            new BusCorridor("Goa", "Mumbai", 660, 1400),
            new BusCorridor("Delhi", "Jaipur", 330, 650),
            new BusCorridor("Jaipur", "Delhi", 330, 650),
            new BusCorridor("Delhi", "Chandigarh", 270, 550),
            new BusCorridor("Chandigarh", "Delhi", 270, 550),
            new BusCorridor("Bangalore", "Chennai", 360, 750),
            new BusCorridor("Chennai", "Bangalore", 360, 750),
            new BusCorridor("Bangalore", "Hyderabad", 540, 1100),
            new BusCorridor("Hyderabad", "Bangalore", 540, 1100),
            new BusCorridor("Pune", "Goa", 540, 1200),
            new BusCorridor("Goa", "Pune", 540, 1200),
            new BusCorridor("Delhi", "Manali", 720, 1600),
            new BusCorridor("Manali", "Delhi", 720, 1600),
            new BusCorridor("Delhi", "Shimla", 480, 950),
            new BusCorridor("Shimla", "Delhi", 480, 950),
            new BusCorridor("Chennai", "Coimbatore", 480, 850),
            new BusCorridor("Coimbatore", "Chennai", 480, 850),
            new BusCorridor("Kochi", "Bangalore", 600, 1300),
            new BusCorridor("Bangalore", "Kochi", 600, 1300),
            new BusCorridor("Ahmedabad", "Surat", 270, 480),
            new BusCorridor("Surat", "Ahmedabad", 270, 480),
            new BusCorridor("Jaipur", "Jodhpur", 330, 650),
            new BusCorridor("Jodhpur", "Jaipur", 330, 650),
            new BusCorridor("Lucknow", "Delhi", 510, 950),
            new BusCorridor("Delhi", "Lucknow", 510, 950)
        );

        String[] operators = {"KSRTC", "MSRTC", "RSRTC", "UPSRTC", "HRTC", "SETC", "Neeta Travels", "VRL Travels", "Orange Tours", "SRS Travels", "Zingbus", "IntrCity SmartBus"};
        String[] types = {"AC_SLEEPER", "AC_SEATER", "VOLVO", "LUXURY", "SEMI_SLEEPER"};
        String[] boardingPoints = {"Central Bus Station", "Metro Station Gate 1", "Express Highway Junction", "Airport Road Terminal", "Ring Road Flyover"};
        String[] droppingPoints = {"City Center Stand", "Railway Station Circle", "Outer Ring Road Exit", "Main Market Hub", "Tech Park Junction"};

        List<Bus> busBatch = new ArrayList<>();
        Random rng = new Random(42);
        int busSeq = 1000;

        for (BusCorridor c : corridors) {
            // Seed 12 buses per corridor across timings to achieve ~330 buses
            for (int i = 0; i < 12; i++) {
                String operator = operators[(i + c.origin().hashCode()) & 0x7FFFFFFF % operators.length];
                String busType = types[i % types.length];
                String busNumber = "IN-" + (10 + rng.nextInt(90)) + "-TR-" + (busSeq++);

                int depHour = (6 + (i * 2)) % 24;
                int depMin = (i * 10) % 60;
                LocalTime depTime = LocalTime.of(depHour, depMin);
                LocalTime arrTime = depTime.plusMinutes(c.durationMins());

                int totalSeats = busType.contains("SLEEPER") ? 36 : 48;
                int booked = rng.nextInt(totalSeats - 5);

                Bus bus = new Bus();
                bus.setBusNumber(busNumber);
                bus.setOperatorName(operator);
                bus.setBusType(busType);
                bus.setOriginCode(c.origin());
                bus.setOriginCity(c.origin());
                bus.setOriginName(c.origin() + " Terminal");
                bus.setDestinationCode(c.dest());
                bus.setDestinationCity(c.dest());
                bus.setDestinationName(c.dest() + " Central");
                bus.setDepartureTime(depTime);
                bus.setArrivalTime(arrTime);
                bus.setDurationMinutes(c.durationMins());
                bus.setTotalSeats(totalSeats);
                bus.setAvailableSeats(totalSeats - booked);
                bus.setBasePrice(BigDecimal.valueOf(c.baseFare() + (rng.nextInt(5) * 50)));
                bus.setRating(3.8 + (rng.nextDouble() * 1.0));
                bus.setAmenities("WiFi,Charging Point,Water Bottle,Reading Light,Blanket");
                bus.setBoardingPoints("[\"" + boardingPoints[i % boardingPoints.length] + "\",\"" + boardingPoints[(i + 1) % boardingPoints.length] + "\"]");
                bus.setDroppingPoints("[\"" + droppingPoints[i % droppingPoints.length] + "\",\"" + droppingPoints[(i + 1) % droppingPoints.length] + "\"]");
                bus.setRunningDays("MON,TUE,WED,THU,FRI,SAT,SUN");
                bus.setActive(true);

                busBatch.add(bus);
            }
        }

        busRepo.saveAll(busBatch);
        log.info("Total buses persisted in database: {}", busRepo.count());
    }

    @Transactional
    public void seedCabs() {
        long currentCount = cabRepo.count();
        if (currentCount >= 100) {
            log.info("Cabs already seeded ({}), skipping.", currentCount);
            return;
        }

        log.info("Seeding realistic cab fleet across major Indian cities...");

        String[] cities = {"Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune", "Goa", "Jaipur", "Ahmedabad", "Kochi", "Chandigarh", "Varanasi"};
        record CabModel(String type, String name, int capacity, int luggage, int base, int perKm, int minFare, String features) {}

        List<CabModel> models = List.of(
            new CabModel("HATCHBACK", "Maruti Suzuki WagonR", 4, 2, 140, 11, 100, "AC,GPS,Compact City Drive"),
            new CabModel("HATCHBACK", "Hyundai Grand i10", 4, 2, 150, 12, 110, "AC,GPS,Music System"),
            new CabModel("SEDAN", "Maruti Suzuki Dzire", 4, 3, 190, 13, 140, "AC,GPS,Boot Space,Phone Charger"),
            new CabModel("SEDAN", "Honda Amaze", 4, 3, 200, 14, 150, "AC,GPS,Ergonomic Seating,Water Bottle"),
            new CabModel("SEDAN", "Hyundai Aura", 4, 3, 190, 13, 140, "AC,GPS,Clean Cabin"),
            new CabModel("SUV", "Toyota Innova Crysta", 7, 5, 320, 19, 250, "AC,GPS,Extra Legroom,Water Bottle,First Aid"),
            new CabModel("SUV", "Mahindra XUV700", 7, 5, 310, 18, 240, "AC,GPS,Panoramic Roof,Fast Charger"),
            new CabModel("SUV", "Kia Carens", 6, 4, 280, 17, 220, "AC,GPS,Family Cabin,Child Seat on Request"),
            new CabModel("LUXURY", "Mercedes-Benz E-Class", 4, 3, 650, 35, 500, "Chauffeur,WiFi,Premium Sound,Mineral Water,Leather Seating"),
            new CabModel("LUXURY", "BMW 5 Series", 4, 3, 650, 35, 500, "Chauffeur,WiFi,Climate Control,Refreshments"),
            new CabModel("TEMPO", "Force Urbania / Traveller", 12, 8, 450, 22, 350, "AC,Group Seating,Ample Luggage Rack,First Aid")
        );

        List<Cab> cabBatch = new ArrayList<>();
        Random rng = new Random(42);

        for (String city : cities) {
            for (CabModel m : models) {
                Cab cab = new Cab();
                cab.setVehicleType(m.type());
                cab.setVehicleName(m.name());
                cab.setCapacity(m.capacity());
                cab.setLuggageCapacity(m.luggage());
                cab.setAc(true);
                cab.setBaseFare(BigDecimal.valueOf(m.base()));
                cab.setPerKmRate(BigDecimal.valueOf(m.perKm()));
                cab.setMinimumFare(BigDecimal.valueOf(m.minFare()));
                cab.setBookingFee(BigDecimal.valueOf(35));
                cab.setRating(4.1 + (rng.nextDouble() * 0.8));
                cab.setFeatures(m.features() + ", Available in " + city);
                cab.setActive(true);

                cabBatch.add(cab);
            }
        }

        cabRepo.saveAll(cabBatch);
        log.info("Total cabs persisted in database: {}", cabRepo.count());
    }
}
