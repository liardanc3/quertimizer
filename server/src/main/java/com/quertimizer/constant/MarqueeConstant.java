package com.quertimizer.constant;

import java.util.Set;

public final class MarqueeConstant {

    public static final String DEFAULT_MESSAGE = "Quertimizer에 오신걸 환영합니다";

    public static final String TARGET_ALL = "all";
    public static final String TARGET_GUEST = "guest";
    public static final String TARGET_USER = "user";
    public static final String TARGET_ADMIN = "admin";
    public static final String TARGET_PROBLEM_GENERATOR = "problemGenerator";
    public static final Set<String> AVAILABLE_TARGETS = Set.of(
            TARGET_ALL,
            TARGET_GUEST,
            TARGET_USER,
            TARGET_ADMIN,
            TARGET_PROBLEM_GENERATOR
    );

    public static final String MODE_REPEAT = "repeat";
    public static final String MODE_SCHEDULE = "schedule";
    public static final Set<String> AVAILABLE_MODES = Set.of(MODE_REPEAT, MODE_SCHEDULE);

    public static final String SCHEDULE_ALWAYS = "always";
    public static final String SCHEDULE_DAILY = "daily";
    public static final String SCHEDULE_WEEKDAYS = "weekdays";
    public static final String SCHEDULE_WEEKEND = "weekend";
    public static final Set<String> AVAILABLE_SCHEDULE_PATTERNS = Set.of(
            SCHEDULE_ALWAYS,
            SCHEDULE_DAILY,
            SCHEDULE_WEEKDAYS,
            SCHEDULE_WEEKEND
    );

    public static final long MARQUEE_LOOP_SECONDS = 18L;

    public static final String TARGET_REQUIRED_MESSAGE = "보여지는 대상이 필요하다.";
    public static final String MESSAGE_REQUIRED_MESSAGE = "문구가 필요하다.";
    public static final String MODE_REQUIRED_MESSAGE = "노출 방식이 필요하다.";
    public static final String STARTED_AT_REQUIRED_MESSAGE = "시작 시간이 필요하다.";
    public static final String REPEAT_COUNT_REQUIRED_MESSAGE = "반복 횟수가 필요하다.";
    public static final String SCHEDULE_PATTERN_REQUIRED_MESSAGE = "스케줄 기준이 필요하다.";
    public static final String SCHEDULE_TIME_REQUIRED_MESSAGE = "스케줄 시간이 필요하다.";
    public static final String INVALID_TARGET_MESSAGE = "보여지는 대상이 올바르지 않다.";
    public static final String INVALID_MODE_MESSAGE = "노출 방식이 올바르지 않다.";
    public static final String INVALID_STARTED_AT_MESSAGE = "시작 시간이 올바르지 않다.";
    public static final String INVALID_REPEAT_COUNT_MESSAGE = "반복 횟수는 1 이상이어야 한다.";
    public static final String INVALID_SCHEDULE_PATTERN_MESSAGE = "스케줄 기준이 올바르지 않다.";
    public static final String INVALID_SCHEDULE_TIME_MESSAGE = "스케줄 시간이 올바르지 않다.";
    public static final String MARQUEE_NOT_FOUND_MESSAGE = "존재하지 않는 전광판 설정이다.";

    private MarqueeConstant() {
    }

}
