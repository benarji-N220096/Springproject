package com.employeeportal.securityapi.security;

import com.employeeportal.securityapi.entity.User;
import com.employeeportal.securityapi.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 1. Why: This service is used to fetch user-specific data from the database.
 * 2. When: Spring calls this during the authentication process when a user
 * tries to login.
 * 3. Who: DaoAuthenticationProvider (part of AuthenticationManager) calls this.
 * 4. What: It looks up the user in the DB and wraps it in a CustomUserDetails
 * object.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new CustomUserDetails(user);
    }
}
