package com.evbooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.evbooking.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns HTTP 401 JSON instead of redirecting to a login page,
 * since this is a REST API consumed by vanilla JS clients that handle
 * navigation themselves.
 */
@Component
public class SessionAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public SessionAuthEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            response.sendRedirect("/login.html");
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponse body = ErrorResponse.of(401, "Unauthorized", "Authentication required.");
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
