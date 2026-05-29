package com.evbooking.controller;

import com.evbooking.dto.LoginRequest;
import com.evbooking.dto.RegisterRequest;
import com.evbooking.dto.UserResponse;
import com.evbooking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * handles login, logout, registration and checking whos currently logged in.
 * everything works via http session cookies, no jwt at all.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    /*
     * openapi: POST /api/auth/login
     * tags: [Auth]
     * summary: Login
     * description: Authenticates the user and opens an HTTP session. The response
     *   sets a JSESSIONID cookie required by all subsequent protected requests.
     * requestBody: LoginRequest (username, password)
     * responses:
     *   200: UserResponse
     *   400: ErrorResponse — validation failure
     *   401: InlineError  — invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // save the security context into the new session
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            UserResponse user = userService.findById(
                    userService.getEntityByUsername(req.username()).getId());
            log.info("User logged in: {}", req.username());
            return ResponseEntity.ok(user);
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password."));
        }
    }

    /*
     * openapi: POST /api/auth/logout
     * tags: [Auth]
     * summary: Logout
     * security: [cookieAuth]
     * description: Invalidates the current session. Subsequent requests with the
     *   same cookie will be treated as unauthenticated.
     * responses:
     *   200: InlineMessage — "Logged out successfully."
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = request.getUserPrincipal() != null
                    ? request.getUserPrincipal().getName() : "unknown";
            session.invalidate();
            log.info("User logged out: {}", username);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    /*
     * openapi: GET /api/auth/me
     * tags: [Auth]
     * summary: Get current user
     * security: [cookieAuth]
     * description: Returns the profile of the currently authenticated user,
     *   or 401 if there is no active session.
     * responses:
     *   200: UserResponse
     *   401: InlineError — "Not authenticated."
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated."));
        }
        UserResponse user = userService.findById(
                userService.getEntityByUsername(authentication.getName()).getId());
        return ResponseEntity.ok(user);
    }

    /*
     * openapi: POST /api/auth/register
     * tags: [Auth]
     * summary: Register
     * description: Creates a new DRIVER account. Self-registration always assigns
     *   the DRIVER role — admin accounts must be created via the database directly.
     * requestBody: RegisterRequest (username, password)
     * responses:
     *   201: UserResponse
     *   400: ErrorResponse — validation failure
     *   409: ErrorResponse — username already taken
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserResponse created = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
