package com.velocity.velocity_management.auth.controller;

import com.velocity.velocity_management.auth.dto.AuthRequest;
import com.velocity.velocity_management.auth.dto.AuthResponse;
import com.velocity.velocity_management.auth.entity.User;
import com.velocity.velocity_management.auth.repository.UserRepository;
import com.velocity.velocity_management.auth.security.JwtUtil;
import com.velocity.velocity_management.common.exception.ApiError;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserRepository userRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtil.generateToken(user);

            AuthResponse response = new AuthResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    token
            );

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for user '{}': bad credentials", request.getUsername());
            ApiError error = new ApiError(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Invalid username or password",
                    Collections.emptyMap()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for user '{}'", request.getUsername(), ex);
            ApiError error = new ApiError(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Invalid username or password",
                    Collections.emptyMap()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception ex) {
            log.error("Unexpected error during login for user '{}'", request.getUsername(), ex);
            ApiError error = new ApiError(
                    LocalDateTime.now(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Login failed due to a server error",
                    Collections.emptyMap()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
