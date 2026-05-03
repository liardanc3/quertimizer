package com.quertimizer.auth.application.port.out;

public interface AuthMailSenderPort {

    void sendAuthCodeMail(String to, String subject, String title, String description, String code);
}
