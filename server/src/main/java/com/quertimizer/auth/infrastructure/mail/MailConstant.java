package com.quertimizer.auth.infrastructure.mail;

public final class MailConstant {

    public static final String VERIFICATION_LABEL = "Verification";
    public static final String AUTH_CODE_LABEL = "인증코드";
    public static final String AUTH_CODE_VALIDITY = "유효시간: 5분";
    public static final String AUTH_CODE_VALIDITY_HTML = "이 코드는 <strong style=\"color:#0f172a;\">5분 동안만 유효</strong>합니다. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.";
    public static final String IGNORE_MAIL_MESSAGE = "본인이 요청하지 않았다면 이 메일을 무시해 주세요.";
    public static final String FOOTER_NOTICE = "이 메일은 quertimizer 계정 보안 확인을 위해 자동 발송되었습니다.";
    public static final String SIGNUP_CODE_SUBJECT = "[quertimizer] 이메일 가입 인증코드";
    public static final String SIGNUP_CODE_TITLE = "이메일 가입 인증코드";
    public static final String SIGNUP_CODE_DESCRIPTION = "quertimizer 계정 생성을 계속하려면 아래 인증코드를 입력해 주세요.";
    public static final String FIND_PASSWORD_CODE_SUBJECT = "[quertimizer] 비밀번호 찾기 인증코드";
    public static final String FIND_PASSWORD_CODE_TITLE = "비밀번호 재설정 인증코드";
    public static final String FIND_PASSWORD_CODE_DESCRIPTION = "비밀번호 재설정을 계속하려면 아래 인증코드를 입력해 주세요.";

    private MailConstant() {
    }
}
