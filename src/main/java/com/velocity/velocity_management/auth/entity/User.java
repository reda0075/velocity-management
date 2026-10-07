package com.velocity.velocity_management.auth.entity;

import com.velocity.velocity_management.auth.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    /**
     * AES-GCM encrypted copy of the initial password issued when this user was created,
     * kept only so an administrator can display it again. Null for users predating this
     * field. Never used to authenticate: {@link #password} stays the only secret that is
     * compared, and it still holds only the BCrypt hash.
     */
    @Column(name = "encrypted_initial_password", length = 255)
    private String encryptedInitialPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
