package com.evbooking.service;

import com.evbooking.model.Booking;
import com.evbooking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class BookingCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingCompletionScheduler.class);

    private final BookingRepository bookingRepository;

    public BookingCompletionScheduler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        completePastBookings();
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void completePastBookings() {
        List<Booking> past = bookingRepository.findPastConfirmedBookings(LocalDate.now());
        if (past.isEmpty()) return;
        past.forEach(b -> b.setStatus(Booking.Status.COMPLETED));
        bookingRepository.saveAll(past);
        log.info("Marked {} past booking(s) as COMPLETED", past.size());
    }
}
