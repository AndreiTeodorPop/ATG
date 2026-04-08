package com.castorama.atg.service;

import com.castorama.atg.domain.model.User;
import com.castorama.atg.dto.request.LoginRequest;
import com.castorama.atg.dto.request.RegisterRequest;
import com.castorama.atg.dto.response.AuthResponse;
import com.castorama.atg.dto.response.UserResponse;
import com.castorama.atg.exception.BusinessException;
import com.castorama.atg.exception.ResourceNotFoundException;
import com.castorama.atg.repository.UserRepository;
import com.castorama.atg.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nucleus-style component: User Profile Service.
 *
 * <p>ATG analogy: a global-scope Nucleus component wrapping
 * {@code /atg/userprofiling/ProfileTools} and
 * {@code /atg/userprofiling/ProfileFormHandler}.
 * Manages account creation, login, and profile retrieval.</p>
 *
 * <p>In ATG, this logic lives in form handlers (servlet pipeline layer) and
 * profile tools (service layer).  Here we consolidate them in one service
 * for clarity, separating concerns with the controller layer.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    /**
     * Register a new customer account.
     * ATG: ProfileFormHandler.create() → ProfileTools.createUser().
     *
     * @throws BusinessException if email or login is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL",
                    "Un compte existe déjà avec l'adresse e-mail: " + request.getEmail());
        }
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new BusinessException("DUPLICATE_LOGIN",
                    "Ce nom d'utilisateur est déjà pris: " + request.getLogin());
        }

        User user = User.builder()
                .login(request.getLogin())
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role("ROLE_CUSTOMER")
                .build();

        user = userRepository.save(user);
        log.info("New customer registered: login={} email={}", user.getLogin(), user.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenService.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getExpirationSeconds())
                .user(toResponse(user))
                .build();
    }

    /**
     * Authenticate an existing customer.
     * ATG: LoginFormHandler → ProfileAuthenticationPipelineServlet → JWT issue.
     *
     * @throws org.springframework.security.core.AuthenticationException on bad credentials
     */
    public AuthResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider → NucleusUserDetailsService
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getCredential(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getCredential());
        String token = jwtTokenService.generateToken(userDetails);

        User user = userRepository.findByEmailOrLoginIgnoreCase(request.getCredential())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "credential",
                                                                 request.getCredential()));

        log.info("Customer logged in: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getExpirationSeconds())
                .user(toResponse(user))
                .build();
    }

    /**
     * Retrieve the authenticated user's profile.
     * ATG: profile.getProperty("*") via the Profile session component.
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
        return toResponse(user);
    }

    /**
     * Find the User entity by email — used internally by other services.
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
    }

    // ---- Mapping ----

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
