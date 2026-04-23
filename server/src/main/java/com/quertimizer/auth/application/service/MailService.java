package com.quertimizer.auth.application.service;

import com.quertimizer.auth.domain.model.AuthFailReason;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final Environment environment;

    @Async
    public void send(String to, String subject, String text) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            // 발신자 주소가 설정되어 있으면 명시적으로 적용
            String mailFrom = environment.getProperty("app.mail.from", environment.getProperty("spring.mail.username", ""));
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }

            // 수신자, 제목, 본문 설정
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
        } catch (MessagingException exception) {
            throw new IllegalStateException(AuthFailReason.MAIL_CREATION_FAILED.getMessage(), exception);
        }

        javaMailSender.send(message);
    }

}
