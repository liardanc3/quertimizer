package com.quertimizer.auth.infrastructure.mail;

import com.quertimizer.auth.application.port.AuthMailSender;
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

import static com.quertimizer.auth.infrastructure.mail.MailConstant.AUTH_CODE_LABEL;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.AUTH_CODE_VALIDITY;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.AUTH_CODE_VALIDITY_HTML;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FOOTER_NOTICE;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.IGNORE_MAIL_MESSAGE;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.VERIFICATION_LABEL;

@Component
@RequiredArgsConstructor
public class SmtpAuthMailSender implements AuthMailSender {

    private final JavaMailSender javaMailSender;
    private final Environment environment;

    /**
     * 인증코드 메일을 생성해 JavaMailSender로 전송한다.
     *
     * <ol>
     *   <li>메일 메시지 생성
     *   <li>수신자와 본문 설정
     *   <li>메일 전송
     * </ol>
     *
     * @param to 수신자 이메일
     * @param subject 메일 제목
     * @param title 메일 본문 제목
     * @param description 인증코드 안내 문구
     * @param code 전송할 인증코드
     * @throws IllegalStateException 메일 메시지를 생성하지 못한 경우
     */
    @Async
    @Override
    public void sendAuthCodeMail(String to, String subject, String title, String description, String code) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = createMailMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildAuthCodeMailText(title, description, code), buildAuthCodeMailHtml(title, description, code));
        } catch (MessagingException exception) {
            throw new IllegalStateException(AuthFailReason.MAIL_CREATION_FAILED.getMessage(), exception);
        }

        javaMailSender.send(message);
    }

    private MimeMessageHelper createMailMessageHelper(MimeMessage message, boolean multipart) throws MessagingException {
        // 메일 메시지 helper 생성
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());

        // 발신자 주소가 설정되어 있으면 명시적으로 적용
        String mailFrom = environment.getProperty("app.mail.from", environment.getProperty("spring.mail.username", ""));
        if (!mailFrom.isBlank()) {
            helper.setFrom(mailFrom);
        }

        return helper;
    }

    private String buildAuthCodeMailText(String title, String description, String code) {
        // Auth 인증코드 Mail 텍스트 구성
        return """
                %s

                %s

                %s: %s
                %s

                %s
                """.formatted(title, description, AUTH_CODE_LABEL, code, AUTH_CODE_VALIDITY, IGNORE_MAIL_MESSAGE);
    }

    private String buildAuthCodeMailHtml(String title, String description, String code) {
        // Auth 인증코드 Mail HTML 구성
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="margin:0;padding:0;background:#f3f6fb;font-family:'Pretendard Variable','Pretendard','SUIT Variable','Noto Sans KR',Arial,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6fb;padding:32px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #dbe6f2;">
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 10px;color:#2563eb;font-size:12px;font-weight:800;letter-spacing:0.08em;text-transform:uppercase;">%s</p>
                              <h1 style="margin:0 0 12px;color:#0f172a;font-size:26px;line-height:1.32;font-weight:800;letter-spacing:-0.03em;">%s</h1>
                              <p style="margin:0 0 24px;color:#475569;font-size:15px;line-height:1.7;">%s</p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:0 0 20px;background:#f8fafc;border:1px solid #dbe6f2;">
                                <tr>
                                  <td style="padding:18px 20px;">
                                    <p style="margin:0 0 8px;color:#64748b;font-size:12px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase;">%s</p>
                                    <p style="margin:0;color:#0f172a;font-size:32px;line-height:1.2;font-weight:800;letter-spacing:0.28em;">%s</p>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 24px;color:#64748b;font-size:13px;line-height:1.7;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 32px 24px;background:#f8fafc;border-top:1px solid #e2e8f0;">
                              <p style="margin:0;color:#64748b;font-size:12px;line-height:1.7;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        VERIFICATION_LABEL, title, description, AUTH_CODE_LABEL, code, AUTH_CODE_VALIDITY_HTML, FOOTER_NOTICE
                    );
    }
}
