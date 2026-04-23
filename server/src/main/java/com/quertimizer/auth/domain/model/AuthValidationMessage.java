package com.quertimizer.auth.domain.model;

public final class AuthValidationMessage {

    public static final String EMAIL_REQUIRED = "이메일을 입력해 주세요";
    public static final String EMAIL_REQUIRED_WITH_PERIOD = "이메일을 입력해 주세요.";
    public static final String EMAIL_FORMAT_INVALID = "올바른 이메일 형식으로 입력해 주세요";
    public static final String EMAIL_FORMAT_INVALID_WITH_PERIOD = "올바른 이메일 형식으로 입력해 주세요.";
    public static final String PASSWORD_REQUIRED = "비밀번호를 입력해 주세요";
    public static final String PASSWORD_REQUIRED_WITH_PERIOD = "비밀번호를 입력해 주세요.";
    public static final String HANDLE_REQUIRED_WITH_PERIOD = "Handle을 입력해 주세요.";
    public static final String HANDLE_FORMAT_INVALID = "영문, 숫자, 언더스코어(_)와 하이픈(-)만 사용할 수 있으며 최대 15자까지 입력할 수 있습니다.";
    public static final String CODE_REQUIRED = "인증코드를 입력해 주세요";
    public static final String CODE_FORMAT_INVALID = "인증코드 6자를 정확히 입력해 주세요";

    private AuthValidationMessage() {
    }

}
