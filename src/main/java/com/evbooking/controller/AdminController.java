package com.evbooking.controller;

import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.UserResponse;
import com.evbooking.service.BookingService;
import com.evbooking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * admin only endpoints for managing the whole system.
 * all methods are protected with @PreAuthorize at class level.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final BookingService bookingService;

    public AdminController(UserService userService, BookingService bookingService) {
        this.userService = userService;
        this.bookingService = bookingService;
    }

    /*
     * openapi: GET /api/admin/users
     * tags: [Admin]
     * summary: List all users
     * security: [cookieAuth]
     * description: Returns every registered user in the system. Requires ADMIN role.
     * responses:
     *   200: UserResponse[]
     *   403: ErrorResponse — not admin
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    /*
     * openapi: GET /api/admin/bookings
     * tags: [Admin]
     * summary: List all bookings
     * security: [cookieAuth]
     * description: Returns every booking in the system regardless of owner. Requires ADMIN role.
     * responses:
     *   200: BookingResponse[]
     *   403: ErrorResponse — not admin
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> listAllBookings() {
        return ResponseEntity.ok(bookingService.findAll());
    }

    /*
     * openapi: DELETE /api/admin/bookings/{id}
     * tags: [Admin]
     * summary: Cancel any booking
     * security: [cookieAuth]
     * description: Allows an admin to cancel any booking regardless of its owner or status.
     *   Requires ADMIN role.
     * parameters:
     *   - id (path, int64): booking ID
     * responses:
     *   200: BookingResponse — status set to CANCELLED
     *   403: ErrorResponse — not admin
     *   404: ErrorResponse — booking not found
     */
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id,
                                                          Authentication auth) {
        return ResponseEntity.ok(bookingService.cancelBooking(auth.getName(), id, true));
    }
}
