package com.evbooking.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;

/**
 * logs each http request with method, uri, status code and how long it took.
 * also includes wich instance handled it - uses the $DYNO env var on heroku
 * (e.g. "web.1") or falls back to the local hostname when running locally.
 * makes it easy to trace requests across multiple dynos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /** resolved once on startup so we dont do a dns/env lookup on every single request */
    private final String instanceId = resolveInstanceId();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri    = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int  status  = response.getStatus();
            // format: [instance] METHOD URI -> STATUS (Xms) timestamp=<epoch>
            log.info("[{}] {} {} -> {} ({}ms)  timestamp={}",
                    instanceId, method, uri, status, elapsed, Instant.now().toEpochMilli());
        }
    }

    /** returns the heroku dyno name if deployed, falls back to hostname when running locally */
    private static String resolveInstanceId() {
        String dyno = System.getenv("DYNO");
        if (dyno != null && !dyno.isBlank()) {
            return dyno;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
