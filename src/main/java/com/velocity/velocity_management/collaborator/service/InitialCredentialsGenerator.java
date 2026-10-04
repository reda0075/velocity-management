package com.velocity.velocity_management.collaborator.service;

import com.velocity.velocity_management.collaborator.dto.response.InitialCredentials;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Builds the initial login of a freshly created collaborator.
 *
 * <p>The username is the generated matricule, so the password always starts with it:
 * {@code <matricule>_<4 random digits>}. Digits may include leading zeroes.
 */
@Component
public class InitialCredentialsGenerator {

    private static final int DIGIT_COUNT = 4;
    private static final int DIGIT_RANGE = 10_000;
    private static final char SEPARATOR = '_';

    private final SecureRandom secureRandom = new SecureRandom();

    public InitialCredentials generate(String matricule) {
        return new InitialCredentials(matricule, matricule + SEPARATOR + randomDigits());
    }

    String randomDigits() {
        return String.format("%0" + DIGIT_COUNT + "d", secureRandom.nextInt(DIGIT_RANGE));
    }
}
