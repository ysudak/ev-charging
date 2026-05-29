package com.evbooking.service;

import com.evbooking.dto.StationRequest;
import com.evbooking.dto.StationResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.ChargingStation;
import com.evbooking.repository.BookingRepository;
import com.evbooking.repository.ChargingStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StationService {

    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private final ChargingStationRepository stationRepository;
    private final BookingRepository bookingRepository;

    public StationService(ChargingStationRepository stationRepository,
                          BookingRepository bookingRepository) {
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> findAll() {
        Map<Long, Long> todayCounts = bookingRepository
                .countTodayGroupedByStation(LocalDate.now())
                .stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        return stationRepository.findAllByOrderByNameAsc().stream()
                .map(s -> StationResponse.from(s,
                        todayCounts.getOrDefault(s.getId(), 0L).intValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StationResponse findById(Long id) {
        return stationRepository.findById(id)
                .map(StationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + id));
    }

    @Transactional(readOnly = true)
    public ChargingStation getEntity(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + id));
    }

    @Transactional
    public StationResponse create(StationRequest req) {
        ChargingStation station = new ChargingStation(
                req.name(), req.address(), req.latitude(), req.longitude(), req.description());
        ChargingStation saved = stationRepository.save(station);
        log.info("Created station id={} name={}", saved.getId(), saved.getName());
        return StationResponse.from(saved);
    }

    @Transactional
    public StationResponse update(Long id, StationRequest req) {
        ChargingStation station = getEntity(id);
        station.setName(req.name());
        station.setAddress(req.address());
        station.setLatitude(req.latitude());
        station.setLongitude(req.longitude());
        station.setDescription(req.description());
        log.info("Updated station id={}", id);
        return StationResponse.from(stationRepository.save(station));
    }

    @Transactional
    public void delete(Long id) {
        ChargingStation station = getEntity(id);
        stationRepository.delete(station);
        log.info("Deleted station id={}", id);
    }
}
