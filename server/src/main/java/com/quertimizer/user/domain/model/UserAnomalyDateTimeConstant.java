package com.quertimizer.user.domain.model;

import java.time.format.DateTimeFormatter;

public final class UserAnomalyDateTimeConstant {

    public static final DateTimeFormatter CUSTOM_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private UserAnomalyDateTimeConstant() {
    }
}
