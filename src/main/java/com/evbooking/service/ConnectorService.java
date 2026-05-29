package com.evbooking.service;

import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.repository.ConnectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConnectorService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorService.class);

    private final ConnectorRepository connectorRepository;
    private final StationService stationService;

    public ConnectorService(ConnectorRepository connectorRepository, StationService stationService) {
        this.connectorRepository = connectorRepository;
        this.stationService = stationService;
    }

    @Transactional(readOnly = true)
    public List<ConnectorResponse> findByStation(Long stationId) {
        return connectorRepository.findByStationId(stationId).stream()
                .map(ConnectorResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConnectorResponse findById(Long id) {
        return connectorRepository.findById(id)
                .map(ConnectorResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Connector not found: " + id));
    }

    @Transactional(readOnly = true)
    public Connector getEntity(Long id) {
        return connectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connector not found: " + id));
    }

    @Transactional
    public ConnectorResponse create(Long stationId, ConnectorRequest req) {
        ChargingStation station = stationService.getEntity(stationId);
        Connector connector = new Connector(station, req.connectorType(), req.powerKw());
        Connector saved = connectorRepository.save(connector);
        log.info("Created connector id={} for station id={}", saved.getId(), stationId);
        return ConnectorResponse.from(saved);
    }

    @Transactional
    public ConnectorResponse update(Long id, ConnectorRequest req) {
        Connector connector = getEntity(id);
        connector.setConnectorType(req.connectorType());
        connector.setPowerKw(req.powerKw());
        log.info("Updated connector id={}", id);
        return ConnectorResponse.from(connectorRepository.save(connector));
    }

    @Transactional
    public void delete(Long id) {
        Connector connector = getEntity(id);
        connectorRepository.delete(connector);
        log.info("Deleted connector id={}", id);
    }
}
