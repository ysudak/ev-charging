package com.evbooking.service;

import com.evbooking.dto.BookedTimeResponse;
import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.RescheduleRequest;
import com.evbooking.exception.BookingConflictException;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.Booking;
import com.evbooking.model.Connector;
import com.evbooking.model.User;
import com.evbooking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * core booking business logic.
 *
 * concurrency: createBooking uses a pessimistic write lock (SELECT FOR UPDATE)
 * via findConflictingBookingsForLock. this blocks other transactions from reading
 * the same rows until we commit, so two simultanious requests cant both see
 * "no conflict" and create overlapping bookings at the same time.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final ConnectorService connectorService;
    private final UserService userService;

    public BookingService(BookingRepository bookingRepository,
                          ConnectorService connectorService,
                          UserService userService) {
        this.bookingRepository = bookingRepository;
        this.connectorService = connectorService;
        this.userService = userService;
    }

    /** returns all booked time slots for a connector on given date, doesnt expose any user info */
    @Transactional(readOnly = true)
    public List<BookedTimeResponse> getBookedTimes(Long connectorId, LocalDate date) {
        return bookingRepository.findConfirmedByConnectorAndDate(connectorId, date)
                .stream().map(BookedTimeResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse createBooking(String username, BookingRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (req.bookingDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book a slot in the past.");
        }

        User user = userService.getEntityByUsername(username);

        // admins manage the system, they dont make bookings
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Administrators cannot make reservations.");
        }
        Connector connector = connectorService.getEntity(req.connectorId());

        // pessimistic lock so two concurrent requests dont both sneak through
        List<Booking> connectorConflicts = bookingRepository.findConflictingBookingsForLock(
                connector.getId(), req.bookingDate(), req.startTime(), req.endTime());
        if (!connectorConflicts.isEmpty()) {
            throw new BookingConflictException(
                    "This connector already has a booking during the requested time slot.");
        }

        // also make sure the driver doesnt already have an overlapping booking at another station
        List<Booking> driverConflicts = bookingRepository.findDriverOverlappingBookings(
                user.getId(), req.bookingDate(), req.startTime(), req.endTime());
        if (!driverConflicts.isEmpty()) {
            throw new BookingConflictException(
                    "You already have a booking that overlaps with the requested time slot.");
        }

        Booking booking = new Booking(user, connector, req.bookingDate(),
                                      req.startTime(), req.endTime());
        Booking saved = bookingRepository.save(booking);
        log.info("Booking created id={} user={} connector={} date={} {}–{}",
                saved.getId(), username, connector.getId(),
                req.bookingDate(), req.startTime(), req.endTime());
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(String username, Long bookingId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // drivers can only cancel their own bookings
        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only cancel your own bookings.");
        }

        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot cancel a booking that is in the past.");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled.");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking cancelled id={} by user={}", bookingId, username);
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findForUser(String username) {
        User user = userService.getEntityByUsername(username);
        return bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(user.getId())
                .stream().map(BookingResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAll() {
        return bookingRepository.findAllByOrderByBookingDateDescStartTimeDesc()
                .stream().map(BookingResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return bookingRepository.findById(id)
                .map(BookingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    @Transactional
    public BookingResponse rescheduleBooking(String username, Long bookingId, RescheduleRequest newSlot) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only reschedule your own bookings.");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new IllegalArgumentException("Cannot reschedule a cancelled booking.");
        }
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot reschedule a past booking.");
        }

        // mark the old one cancelled then create a new booking on the same connector
        Long connectorId = booking.getConnector().getId();
        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);

        BookingRequest req = new BookingRequest(
                connectorId,
                newSlot.bookingDate(),
                newSlot.startTime(),
                newSlot.endTime());
        log.info("Rescheduling booking id={} for user={}", bookingId, username);
        return createBooking(username, req);
    }
}
