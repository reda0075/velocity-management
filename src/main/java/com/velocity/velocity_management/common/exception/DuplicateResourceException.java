package com.velocity.velocity_management.common.exception;

/**
 * Thrown when a write operation would create a duplicate
 * of an existing resource (for example a velocity calculation
 * that already exists for a collaborator and month).
 * Mapped to HTTP 409 Conflict with the exception's message.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
