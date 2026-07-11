package com.employeeportal.securityapi.service;

import com.employeeportal.securityapi.dto.RegisterRequest;
import com.employeeportal.securityapi.entity.User;
import com.employeeportal.securityapi.repository.UserRepository;
import com.employeeportal.securityapi.security.CustomUserDetailsService;
import com.employeeportal.securityapi.security.JwtService;
import com.employeeportal.securityapi.dto.AuthResponse;
import com.employeeportal.securityapi.dto.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);
        return "User registered successfully!";
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate the user. If credentials are bad, BadCredentialsException is thrown automatically.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Fetch user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Generate tokens
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        // Extract username from the refresh token
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate the refresh token mathematically
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                // Generate a new access token
                String newAccessToken = jwtService.generateToken(userDetails);
                
                // Return the new access token along with the same refresh token
                return new AuthResponse(newAccessToken, refreshToken);
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }
}
