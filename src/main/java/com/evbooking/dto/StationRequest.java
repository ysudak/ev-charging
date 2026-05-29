package com.evbooking.dto;

import jakarta.validation.constraints.*;

public record StationRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 200) String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 500) String description
) {}
