package com.quertimizer.auth.application.port;

public interface AuthMailSender {

    /**
     * 인증코드 메일을 외부 메일 시스템으로 전송한다.
     *
     * @param to 수신자 이메일
     * @param subject 메일 제목
     * @param title 메일 본문 제목
     * @param description 인증코드 안내 문구
     * @param code 전송할 인증코드
     */
    void sendAuthCodeMail(String to, String subject, String title, String description, String code);
}
