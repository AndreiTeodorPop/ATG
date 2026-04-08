package com.castorama.atg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot smoke test — verifies the application context loads cleanly.
 *
 * <p>ATG analogy: a Nucleus component resolution test that verifies all
 * {@code $class} references resolve and {@code doStartService()} completes
 * without error on startup.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class CastoramaAtgApplicationTests {

    @Test
    void contextLoads() {
        // Verifies: all Spring beans wire correctly, H2 schema creates cleanly,
        // security config initialises, JWT service starts, pipeline assembles.
    }
}
