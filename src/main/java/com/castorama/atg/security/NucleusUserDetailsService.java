package com.castorama.atg.security;

import com.castorama.atg.domain.model.User;
import com.castorama.atg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security UserDetailsService backed by the Nucleus UserRepository.
 *
 * <p>ATG analogy: {@code /atg/userprofiling/ProfileAuthenticationPipelineServlet}
 * and the underlying {@code ProfileRepository} lookup.  In ATG, authentication
 * goes through the login pipeline; here Spring Security's DaoAuthenticationProvider
 * delegates to this service.</p>
 *
 * <p>Lookup accepts either email or login, matching ATG's flexible credential
 * resolution where customers may use either identifier.</p>
 */
@Service("userDetailsService")
@RequiredArgsConstructor
public class NucleusUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String credential) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrLoginIgnoreCase(credential)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable: " + credential));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())   // email is the canonical principal identity
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                .build();
    }
}
