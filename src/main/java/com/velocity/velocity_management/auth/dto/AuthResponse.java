package com.velocity.velocity_management.auth.dto;

import com.velocity.velocity_management.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    private Long id;
    private String username;
    private Role role;
    private String token;
}
