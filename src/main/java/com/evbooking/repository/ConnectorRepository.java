package com.evbooking.repository;

import com.evbooking.model.Connector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {

    List<Connector> findByStationId(Long stationId);
}
