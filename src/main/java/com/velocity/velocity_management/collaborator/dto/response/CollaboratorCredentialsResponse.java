package com.velocity.velocity_management.collaborator.dto.response;

/**
 * Login an administrator may display again for an existing collaborator.
 * Carries nothing else: no identifier, no role, no BCrypt hash, no encrypted value.
 */
public record CollaboratorCredentialsResponse(String username, String temporaryPassword) {
}