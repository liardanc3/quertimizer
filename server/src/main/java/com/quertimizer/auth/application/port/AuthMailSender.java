package com.quertimizer.auth.application.port;

public interface AuthMailSender {

    void sendAuthCodeMail(String to, String subject, String title, String description, String code);
}
