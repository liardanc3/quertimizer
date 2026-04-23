package com.quertimizer.user.domain.model;

public final class UserValidationMessage {

    public static final String BIO_LENGTH_EXCEEDED = "소개글은 최대 1000자까지 입력할 수 있습니다.";
    public static final String PROFILE_LINK_LIMIT_EXCEEDED = "프로필 링크는 최대 10개까지 추가할 수 있습니다.";
    public static final String DEFAULT_DBMS_REQUIRED = "기본 DBMS를 선택해 주세요.";
    public static final String LINK_TYPE_REQUIRED = "링크 타입을 입력해 주세요.";
    public static final String LINK_TYPE_LENGTH_EXCEEDED = "링크 타입은 최대 30자까지 입력할 수 있습니다.";
    public static final String LINK_TYPE_PIPE_UNAVAILABLE = "링크 타입에는 '|' 문자를 사용할 수 없습니다.";
    public static final String LINK_VALUE_REQUIRED = "링크 값을 입력해 주세요.";
    public static final String LINK_VALUE_LENGTH_EXCEEDED = "링크 값은 최대 255자까지 입력할 수 있습니다.";
    public static final String LINK_VALUE_PIPE_UNAVAILABLE = "링크 값에는 '|' 문자를 사용할 수 없습니다.";

    private UserValidationMessage() {
    }

}
