package com.evbooking.controller;

import com.evbooking.dto.BookedTimeResponse;
import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.service.BookingService;
import com.evbooking.service.ConnectorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ConnectorController {

    private final ConnectorService connectorService;
    private final BookingService bookingService;

    public ConnectorController(ConnectorService connectorService, BookingService bookingService) {
        this.connectorService = connectorService;
        this.bookingService = bookingService;
    }

    /*
     * openapi: GET /api/stations/{stationId}/connectors
     * tags: [Connectors]
     * summary: List connectors for a station
     * description: Returns all connectors belonging to the specified station. No authentication required.
     * parameters:
     *   - stationId (path, int64): parent station ID
     * responses:
     *   200: ConnectorResponse[]
     *   404: ErrorResponse — station not found
     */
    @GetMapping("/stations/{stationId}/connectors")
    public ResponseEntity<List<ConnectorResponse>> listByStation(@PathVariable Long stationId) {
        return ResponseEntity.ok(connectorService.findByStation(stationId));
    }

    /*
     * openapi: POST /api/stations/{stationId}/connectors
     * tags: [Connectors]
     * summary: Add a connector to a station
     * security: [cookieAuth]
     * description: Creates a new connector for the given station. Requires ADMIN role.
     * parameters:
     *   - stationId (path, int64): parent station ID
     * requestBody: ConnectorRequest (connectorType, powerKw)
     * responses:
     *   201: ConnectorResponse
     *   400: ErrorResponse — validation failure
     *   403: ErrorResponse — not admin
     *   404: ErrorResponse — station not found
     */
    @PostMapping("/stations/{stationId}/connectors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConnectorResponse> create(@PathVariable Long stationId,
                                                     @Valid @RequestBody ConnectorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(connectorService.create(stationId, req));
    }

    /*
     * openapi: GET /api/connectors/{id}
     * tags: [Connectors]
     * summary: Get a connector
     * description: Returns a single connector by ID. No authentication required.
     * parameters:
     *   - id (path, int64): connector ID
     * responses:
     *   200: ConnectorResponse
     *   404: ErrorResponse — connector not found
     */
    @GetMapping("/connectors/{id}")
    public ResponseEntity<ConnectorResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(connectorService.findById(id));
    }

    /*
     * openapi: PUT /api/connectors/{id}
     * tags: [Connectors]
     * summary: Update a connector
     * security: [cookieAuth]
     * description: Updates the type and power rating of an existing connector. Requires ADMIN role.
     * parameters:
     *   - id (path, int64): connector ID
     * requestBody: ConnectorRequest (connectorType, powerKw)
     * responses:
     *   200: ConnectorResponse
     *   400: ErrorResponse — validation failure
     *   403: ErrorResponse — not admin
     *   404: ErrorResponse — connector not found
     */
    @PutMapping("/connectors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConnectorResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ConnectorRequest req) {
        return ResponseEntity.ok(connectorService.update(id, req));
    }

    /*
     * openapi: DELETE /api/connectors/{id}
     * tags: [Connectors]
     * summary: Delete a connector
     * security: [cookieAuth]
     * description: Permanently removes a connector. Requires ADMIN role.
     * parameters:
     *   - id (path, int64): connector ID
     * responses:
     *   204: no content
     *   403: ErrorResponse — not admin
     *   404: ErrorResponse — connector not found
     */
    @DeleteMapping("/connectors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        connectorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /*
     * openapi: GET /api/connectors/{id}/booked-times
     * tags: [Connectors]
     * summary: Get booked time windows
     * description: Returns all occupied time slots for a connector on a given calendar
     *   date. Used by the booking UI to visualise availability. No authentication required.
     * parameters:
     *   - id   (path,  int64):  connector ID
     *   - date (query, string): ISO-8601 date, e.g. 2025-06-15
     * responses:
     *   200: BookedTimeResponse[] — list of {startTime, endTime} windows
     *   404: ErrorResponse — connector not found
     */
    @GetMapping("/connectors/{id}/booked-times")
    public ResponseEntity<List<BookedTimeResponse>> bookedTimes(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getBookedTimes(id, date));
    }

}
