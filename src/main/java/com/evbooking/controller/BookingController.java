package com.evbooking.controller;

import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.RescheduleRequest;
import com.evbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /*
     * openapi: POST /api/bookings
     * tags: [Bookings]
     * summary: Create a booking
     * security: [cookieAuth]
     * description: Books a connector time slot for the authenticated driver.
     *   Returns 409 if the slot overlaps with an existing active booking.
     * requestBody: BookingRequest (connectorId, bookingDate, startTime, endTime)
     * responses:
     *   201: BookingResponse
     *   400: ErrorResponse — validation failure
     *   401: ErrorResponse — not authenticated
     *   409: ErrorResponse — time slot conflict
     */
    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest req,
                                                   Authentication auth) {
        BookingResponse created = bookingService.createBooking(auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /*
     * openapi: GET /api/bookings
     * tags: [Bookings]
     * summary: List my bookings
     * security: [cookieAuth]
     * description: Returns all bookings owned by the currently authenticated driver.
     * responses:
     *   200: BookingResponse[]
     *   401: ErrorResponse — not authenticated
     */
    @GetMapping
    public ResponseEntity<List<BookingResponse>> listMine(Authentication auth) {
        return ResponseEntity.ok(bookingService.findForUser(auth.getName()));
    }

    /*
     * openapi: GET /api/bookings/{id}
     * tags: [Bookings]
     * summary: Get a booking
     * security: [cookieAuth]
     * description: Returns a single booking by ID. Ownership is validated in the service.
     * parameters:
     *   - id (path, int64): booking ID
     * responses:
     *   200: BookingResponse
     *   401: ErrorResponse — not authenticated
     *   404: ErrorResponse — booking not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    /*
     * openapi: DELETE /api/bookings/{id}
     * tags: [Bookings]
     * summary: Cancel a booking
     * security: [cookieAuth]
     * description: Cancels a booking. Drivers may only cancel their own future bookings.
     *   Admins may cancel any booking regardless of owner or status.
     * parameters:
     *   - id (path, int64): booking ID
     * responses:
     *   200: BookingResponse — status set to CANCELLED
     *   401: ErrorResponse — not authenticated
     *   403: ErrorResponse — not the owner and not admin
     *   404: ErrorResponse — booking not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id,
                                                   Authentication auth) {
        boolean isAdmin = auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return ResponseEntity.ok(bookingService.cancelBooking(auth.getName(), id, isAdmin));
    }

    /*
     * openapi: PUT /api/bookings/{id}
     * tags: [Bookings]
     * summary: Reschedule a booking
     * security: [cookieAuth]
     * description: Moves a booking to a new date and time on the same connector.
     *   Returns 409 if the new slot overlaps with another active booking.
     * parameters:
     *   - id (path, int64): booking ID
     * requestBody: RescheduleRequest (bookingDate, startTime, endTime)
     * responses:
     *   200: BookingResponse — updated booking
     *   400: ErrorResponse — validation failure
     *   401: ErrorResponse — not authenticated
     *   404: ErrorResponse — booking not found
     *   409: ErrorResponse — new time slot conflict
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> reschedule(@PathVariable Long id,
                                                       @Valid @RequestBody RescheduleRequest req,
                                                       Authentication auth) {
        return ResponseEntity.ok(bookingService.rescheduleBooking(auth.getName(), id, req));
    }
}
