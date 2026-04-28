package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
public class UpdateProblemGeneratorPermissionsInput {

    private final String handle;
    private final List<String> permissionKeys;

    public static UpdateProblemGeneratorPermissionsInput of(String handle, List<String> permissionKeys) {
        return new UpdateProblemGeneratorPermissionsInput(
                normalizeHandle(handle), Optional.ofNullable(permissionKeys).orElse(List.of())
        );
    }

    private static String normalizeHandle(String handle) {
        return Optional.ofNullable(handle)
                .map(String::trim)
                .orElse("");
    }
}
