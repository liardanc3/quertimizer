package com.quertimizer.community.domain.model;

import java.util.Set;

public final class CommunityImageConstant {

    public static final long MAX_SIZE = 10 * 1024 * 1024L;
    public static final long MAX_PIXELS = 20_000_000L;
    public static final Set<String> ALLOWED_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private CommunityImageConstant() {
    }
}
