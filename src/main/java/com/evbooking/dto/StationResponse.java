package com.evbooking.dto;

import com.evbooking.model.ChargingStation;

public record StationResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String description,
        int connectorCount,
        int todayBookingCount
) {
    public static StationResponse from(ChargingStation s) {
        return new StationResponse(
                s.getId(), s.getName(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getDescription(),
                s.getConnectors().size(), 0);
    }

    public static StationResponse from(ChargingStation s, int todayBookingCount) {
        return new StationResponse(
                s.getId(), s.getName(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getDescription(),
                s.getConnectors().size(), todayBookingCount);
    }
}
