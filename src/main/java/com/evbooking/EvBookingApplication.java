package com.evbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the EV Charging Station Booking System.
 *
 * {@code @SpringBootApplication} is equivalent to:
 *   {@code @ComponentScan} — scans all sub-packages (com.evbooking.*) for beans
 *   {@code @Configuration} — marks this as a configuration source
 *   {@code @EnableAutoConfiguration} — activates Spring Boot auto-configuration
 *
 * This follows the IoC pattern from the course theory: all beans (services,
 * repositories, controllers) are auto-detected by component scan and managed
 * by the Spring IoC container — no manual wiring in the main class.
 */
@SpringBootApplication
@EnableScheduling
public class EvBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvBookingApplication.class, args);
    }
}
