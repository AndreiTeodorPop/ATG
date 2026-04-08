package com.castorama.atg.config;

import com.castorama.atg.admin.DynAdminProperties;
import com.castorama.atg.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — two separate filter chains:
 *
 * <ol>
 *   <li>{@link #adminSecurityFilterChain} {@code @Order(1)} — HTTP Basic Auth for
 *       {@code /dyn/admin/**}.  Uses an {@link InMemoryUserDetailsManager} with
 *       credentials from {@code castorama.admin.*} properties so it is completely
 *       isolated from the JWT user chain.</li>
 *   <li>{@link #apiSecurityFilterChain} {@code @Order(2)} — stateless JWT chain for
 *       all {@code /api/**} and remaining URLs.</li>
 * </ol>
 *
 * <p>ATG analogy: ATG Dynamo Administration is protected by its own servlet pipeline
 * branch ({@code /atg/dynamo/admin/}) with a separate credential store, while the
 * REST API layer is protected by the Dynamo Security Service JWT/token validator.
 * Spring Security's ordered filter chain mechanism mirrors this split exactly.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final DynAdminProperties dynAdminProperties;

    // ------------------------------------------------------------------
    // Chain 1 — Dyn/Admin HTTP Basic Auth  (@Order(1) — evaluated first)
    // ------------------------------------------------------------------

    /**
     * Security filter chain for the Dyn/Admin interface.
     *
     * <p>ATG analogy: the {@code /atg/dynamo/admin/} Nucleus security configuration
     * that requires an admin password (set in {@code /dyn/admin/nucleus/atg/dynamo/admin/
     * AdminPasswordHasher.properties}) before any admin page is served.</p>
     *
     * <p>Key design decisions:
     * <ul>
     *   <li>Uses {@code securityMatcher} so this chain only handles {@code /dyn/admin/**}
     *       — it yields all other requests to the JWT chain.</li>
     *   <li>{@code InMemoryUserDetailsManager} is deliberately separate from
     *       {@link com.castorama.atg.security.NucleusUserDetailsService} to avoid
     *       admin credentials leaking into the customer JWT flow.</li>
     *   <li>Session is kept stateful (Spring Security default) so the browser
     *       does not re-prompt on every admin page navigation, matching real
     *       ATG dyn/admin browser behaviour.</li>
     * </ul></p>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        // Build an isolated InMemoryUserDetailsManager — NOT a Spring bean so it
        // does not create a second UserDetailsService in the context (which would
        // conflict with NucleusUserDetailsService and break the global AuthManager).
        var adminUser = User.withUsername(dynAdminProperties.getUsername())
            .password(passwordEncoder().encode(dynAdminProperties.getPassword()))
            .roles("ADMIN")
            .build();
        var adminStore = new InMemoryUserDetailsManager(adminUser);

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider();
        adminProvider.setUserDetailsService(adminStore);
        adminProvider.setPasswordEncoder(passwordEncoder());

        http
            .securityMatcher("/dyn/admin", "/dyn/admin/**")
            .authenticationProvider(adminProvider)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN")
            )
            .httpBasic(basic -> basic.realmName("Castorama Dyn/Admin"))
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // ------------------------------------------------------------------
    // Chain 2 — JWT API security  (@Order(2) — evaluated for everything else)
    // ------------------------------------------------------------------

    /**
     * Stateless JWT security chain for all API and remaining URLs.
     *
     * <p>ATG analogy: the Dynamo Servlet Pipeline with a
     * {@code JWTValidationPipelineServlet} that validates Bearer tokens
     * on {@code /rest/} endpoints and allows anonymous access to the
     * public catalogue and auth endpoints.</p>
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: authentication endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public: catalogue browse (ATG: anonymous users can browse catalogue)
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                // Public: Swagger UI and OpenAPI docs
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                // Public: SPA static assets
                .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                // Public: H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // Public: actuator health
                .requestMatchers("/actuator/health").permitAll()
                // Allow Spring Boot's error endpoint (needed for Basic Auth 401 rendering)
                .requestMatchers("/error").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            // Needed for H2 console iframe rendering in dev
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder — ATG uses its own PasswordHasher component
     * ({@code /atg/userprofiling/PasswordHasher}) which also defaults to bcrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
