package com.evbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EvBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvBookingApplication.class, args);
    }
}
