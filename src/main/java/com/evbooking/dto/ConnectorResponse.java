package com.evbooking.dto;

import com.evbooking.model.Connector;

public record ConnectorResponse(
        Long id,
        Long stationId,
        String connectorType,
        Integer powerKw
) {
    public static ConnectorResponse from(Connector c) {
        return new ConnectorResponse(
                c.getId(),
                c.getStation().getId(),
                c.getConnectorType(),
                c.getPowerKw()
        );
    }
}
