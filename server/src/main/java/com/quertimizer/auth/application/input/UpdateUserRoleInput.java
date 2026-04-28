package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
public class UpdateUserRoleInput {

    private final String handle;
    private final String role;

    public static UpdateUserRoleInput of(String handle, String role) {
        return new UpdateUserRoleInput(normalizeHandle(handle), normalizeRole(role));
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
}
