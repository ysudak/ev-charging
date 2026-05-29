package com.evbooking.controller;

import com.evbooking.dto.StationRequest;
import com.evbooking.dto.StationResponse;
import com.evbooking.service.StationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * public GET endpoints for station info, used by the map.
 * anything that modifies data requires admin role.
 */
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    /*
     * openapi: GET /api/stations
     * tags: [Stations]
     * summary: List all stations
     * description: Returns every charging station with connector counts and today's
     *   booking count. No authentication required.
     * responses:
     *   200: StationResponse[]
     */
    @GetMapping
    public ResponseEntity<List<StationResponse>> listAll() {
        return ResponseEntity.ok(stationService.findAll());
    }

    /*
     * openapi: GET /api/stations/{id}
     * tags: [Stations]
     * summary: Get a station
     * description: Returns a single charging station by ID. No authentication required.
     * parameters:
     *   - id (path, int64): station ID
     * responses:
     *   200: StationResponse
     *   404: ErrorResponse — station not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<StationResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.findById(id));
    }

    /*
     * openapi: POST /api/stations
     * tags: [Stations]
     * summary: Create a station
     * security: [cookieAuth]
     * description: Adds a new charging station. Requires ADMIN role.
     * requestBody: StationRequest (name, address, latitude, longitude, description?)
     * responses:
     *   201: StationResponse
     *   400: ErrorResponse — validation failure
     *   403: ErrorResponse — not admin
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponse> create(@Valid @RequestBody StationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationService.create(req));
    }

    /*
     * openapi: PUT /api/stations/{id}
     * tags: [Stations]
     * summary: Update a station
     * security: [cookieAuth]
     * description: Updates name, address, coordinates, or description. Requires ADMIN role.
     * parameters:
     *   - id (path, int64): station ID
     * requestBody: StationRequest
     * responses:
     *   200: StationResponse
     *   400: ErrorResponse — validation failure
     *   403: ErrorResponse — not admin
     *   404: ErrorResponse — station not found
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody StationRequest req) {
        return ResponseEntity.ok(stationService.update(id, req));
    }

    /*
     * openapi: DELETE /api/stations/{id}
     * tags: [Stations]
     * summary: Delete a station
     * security: [cookieAuth]
     * description: Permanently deletes a station and all its connectors. Requires ADMIN role.
     * parameters:
     *   - id (path, int64): station ID
     * responses:
     *   204: no content
     *   403: ErrorResponse — not admin
     *   404: ErrorResponse — station not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
