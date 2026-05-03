package com.quertimizer.community.domain.model;

import java.time.Duration;

public final class CommunityViewConstant {

    public static final Duration DUPLICATE_VIEW_WINDOW = Duration.ofMinutes(10);

    private CommunityViewConstant() {
    }
}
