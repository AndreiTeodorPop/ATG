package com.castorama.atg.repository;

import com.castorama.atg.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ATG analogy: a GSA (Generic SQL Adapter) repository for the
 * {@code /atg/userprofiling/ProfileRepository} item descriptor.
 *
 * <p>In ATG you would use {@code RepositoryItem} and query via RQL:
 * <pre>
 *   repository.executeRQLQuery(
 *       "email = :0", new Object[]{ email }, "user", queryOptions);
 * </pre>
 * Spring Data JPA provides the same capability through derived query methods
 * and JPQL, which maps cleanly to ATG RQL semantics for simple lookups.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ATG RQL equivalent:
     * {@code SELECT * FROM dps_user WHERE email = :email}.
     */
    Optional<User> findByEmail(String email);

    /**
     * ATG RQL equivalent:
     * {@code SELECT * FROM dps_user WHERE login = :login}.
     */
    Optional<User> findByLogin(String login);

    /**
     * Case-insensitive login-or-email lookup used by the authentication pipeline.
     * ATG equivalent: ProfileAuthenticationPipelineServlet credential check.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:credential) " +
           "OR LOWER(u.login) = LOWER(:credential)")
    Optional<User> findByEmailOrLoginIgnoreCase(String credential);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);
}
