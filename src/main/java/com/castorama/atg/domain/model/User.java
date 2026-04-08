package com.castorama.atg.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ATG analogy: the {@code /atg/userprofiling/Profile} item descriptor stored in
 * the {@code dps_user} / {@code dps_usr_addr} GSA tables.
 *
 * <p>In ATG, ProfileTool and ProfileFormHandler manage create/update operations.
 * Here, {@link com.castorama.atg.service.UserService} plays that role, and the
 * JPA repository maps 1-to-1 to the GSA repository layer.</p>
 *
 * <p>Fields deliberately mirror ATG profile property names where possible so that
 * a future migration to real ATG is straightforward.</p>
 */
@Entity
@Table(name = "dps_user",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** ATG: profile id (generated UUID in Nucleus). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ATG: profile property "login". */
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String login;

    /** ATG: profile property "email". */
    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * ATG: profile property "password" (hashed by Dynamo's PasswordHasher).
     * Stored as BCrypt hash — never plain-text.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** ATG: profile property "firstName". */
    @Column(name = "first_name", length = 80)
    private String firstName;

    /** ATG: profile property "lastName". */
    @Column(name = "last_name", length = 80)
    private String lastName;

    /** ATG: profile property "phoneNumber". */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * ATG: profile property "role" — simplified single-role model.
     * In ATG this would be managed through the UserDirectory and access-control
     * groups via {@code /atg/userprofiling/ProfileGroup}.
     */
    @Column(length = 30)
    @Builder.Default
    private String role = "ROLE_CUSTOMER";

    /** ATG: profile property "creationDate". */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * ATG analogy: the ShoppingCart session-scoped component is linked to a profile.
     * Here we persist the cart directly (simplification).
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
