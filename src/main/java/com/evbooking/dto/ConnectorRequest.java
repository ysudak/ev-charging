package com.evbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConnectorRequest(
        @NotBlank String connectorType,
        @NotNull @Min(1) Integer powerKw
) {}
