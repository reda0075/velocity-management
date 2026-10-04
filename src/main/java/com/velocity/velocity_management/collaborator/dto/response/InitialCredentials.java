package com.velocity.velocity_management.collaborator.dto.response;

/**
 * One-time login handed to the administrator when a collaborator is created.
 * It is returned by the creation response only and is never stored in clear text.
 */
public record InitialCredentials(String username, String temporaryPassword) {
}
