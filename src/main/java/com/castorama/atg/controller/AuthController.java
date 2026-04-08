package com.castorama.atg.controller;

import com.castorama.atg.dto.request.LoginRequest;
import com.castorama.atg.dto.request.RegisterRequest;
import com.castorama.atg.dto.response.AuthResponse;
import com.castorama.atg.dto.response.UserResponse;
import com.castorama.atg.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication and profile endpoints.
 *
 * <p>ATG analogy:
 * <ul>
 *   <li>POST /register → ProfileFormHandler.create() form submission</li>
 *   <li>POST /login   → LoginFormHandler form submission</li>
 *   <li>GET  /me      → Profile session component property access</li>
 * </ul>
 * </p>
 *
 * <p>Base path: {@code /api/v1/auth}</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Register a new customer account.
     *
     * <pre>
     * POST /api/v1/auth/register
     * {
     *   "login": "jean.dupont",
     *   "email": "jean.dupont@example.fr",
     *   "password": "MonMotDePasse1!",
     *   "firstName": "Jean",
     *   "lastName": "Dupont"
     * }
     * </pre>
     *
     * @return 201 Created with JWT token and user profile
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate an existing customer.
     *
     * <pre>
     * POST /api/v1/auth/login
     * {
     *   "credential": "jean.dupont@example.fr",
     *   "password": "MonMotDePasse1!"
     * }
     * </pre>
     *
     * @return 200 OK with JWT token and user profile
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    /**
     * Get the authenticated user's profile.
     *
     * <pre>
     * GET /api/v1/auth/me
     * Authorization: Bearer {token}
     * </pre>
     *
     * @return 200 OK with user profile (no sensitive fields)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }
}
