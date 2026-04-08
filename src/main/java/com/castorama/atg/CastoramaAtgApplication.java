package com.castorama.atg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Castorama ATG Commerce Application entry point.
 *
 * <p>In a real ATG deployment this would be bootstrapped by the Nucleus IoC container
 * via an EAR/WAR descriptor.  Here Spring Boot's application context serves the same
 * role: it wires all "Nucleus-style" components (annotated with @NucleusComponent or
 * plain @Service/@Repository) and starts the embedded servlet container.</p>
 *
 * <p>ATG analogy: {@code DYNAMO_HOME/localconfig/atg/dynamo/service/Initial.properties}
 * triggering component resolution on startup.</p>
 */
@SpringBootApplication
public class CastoramaAtgApplication {

    public static void main(String[] args) {
        SpringApplication.run(CastoramaAtgApplication.class, args);
    }
}
