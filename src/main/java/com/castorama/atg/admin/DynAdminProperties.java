package com.castorama.atg.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Dyn/Admin interface.
 *
 * <p>ATG analogy: the Nucleus component properties file for the ATG Dynamo
 * Administration UI, typically {@code /atg/dynamo/admin/DynAdmin.properties},
 * which holds the admin password and access-control settings.</p>
 *
 * <p>Credentials are loaded from {@code application.properties} under the
 * {@code castorama.admin} prefix.  In production, override via environment
 * variables ({@code CASTORAMA_ADMIN_USERNAME} / {@code CASTORAMA_ADMIN_PASSWORD})
 * and never commit plain-text admin credentials to source control.</p>
 */
@Component
@ConfigurationProperties(prefix = "castorama.admin")
public class DynAdminProperties {

    /**
     * Admin username for HTTP Basic Auth.
     * ATG analogy: {@code /atg/dynamo/admin/DynAdmin.properties#adminUsername}.
     */
    private String username = "admin";

    /**
     * Admin password for HTTP Basic Auth (plain-text; Spring Security will
     * encode it with BCrypt before storing in the InMemoryUserDetailsManager).
     * ATG analogy: {@code /atg/dynamo/admin/DynAdmin.properties#adminPassword}.
     */
    private String password = "admin123";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
