package com.quertimizer.auth.application.input;

import lombok.Data;

import java.util.Optional;

@Data
public class UpdateUserRoleInput {

    private final String handle;
    private final String role;
    private final String actorEmail;
    private final String confirmationText;

    public static UpdateUserRoleInput of(String handle, String role, String actorEmail, String confirmationText) {
        return new UpdateUserRoleInput(
                normalizeHandle(handle), normalizeRole(role), normalizeActorEmail(actorEmail), normalizeConfirmationText(confirmationText)
        );
    }

    private static String normalizeHandle(String handle) {
        return Optional.ofNullable(handle)
                .map(String::trim)
                .orElse("");
    }

    private static String normalizeRole(String role) {
        return Optional.ofNullable(role)
                .map(String::trim)
                .orElse("");
    }

    private static String normalizeActorEmail(String actorEmail) {
        return Optional.ofNullable(actorEmail)
                .map(String::trim)
                .orElse("");
    }

    private static String normalizeConfirmationText(String confirmationText) {
        return Optional.ofNullable(confirmationText)
                .map(String::trim)
                .orElse("");
    }
}
