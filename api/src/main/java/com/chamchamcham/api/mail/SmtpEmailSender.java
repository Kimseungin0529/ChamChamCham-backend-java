package com.chamchamcham.api.mail;

import com.chamchamcham.application.auth.local.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);
    public SmtpEmailSender(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromAddress,
            @Value("${app.mail.from-name:Example}") String fromName) {
        this.mailSender = mailSender; this.fromAddress = fromAddress; this.fromName = fromName;
    }
    @Override
    public void sendVerificationCode(String email, String code) {
        java.util.Objects.requireNonNull(email, "email");
        java.util.Objects.requireNonNull(code, "code");
        var message = createEmailForm(email, fromName + " 이메일 인증 번호", code);
        try { mailSender.send(message); }
        catch (Exception exception) {
            logger.error("Failed to send verification email. emailHash={}", email.hashCode(), exception);
            throw new IllegalStateException("Failed to send verification email", exception);
        }
    }
    private MimeMessage createEmailForm(String toEmail, String title, String authCode) {
        var mimeMessage = mailSender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(toEmail); helper.setFrom(fromAddress, fromName); helper.setSubject(title);
            helper.setText("""
                <html><body style='background-color: #000000 !important; margin: 0 auto; max-width: 600px; word-break: break-all; padding-top: 50px; color: #ffffff;'>
                <h1 style='padding-top: 50px; font-size: 30px;'>이메일 주소 인증</h1>
                <p style='padding-top: 20px; font-size: 18px; opacity: 0.6; line-height: 30px; font-weight: 400;'>안녕하세요? %s입니다.<br />
                %s 서비스 사용을 위해 회원가입시 고객님께서 입력하신 이메일 주소의 인증이 필요합니다.<br />
                하단의 인증 번호로 이메일 인증을 완료하시면, 정상적으로 %s 서비스를 이용하실 수 있습니다.<br />
                항상 최선의 노력을 다하는 %s 되겠습니다.<br />
                감사합니다.</p>
                <div class='code-box' style='margin-top: 50px; padding-top: 20px; color: #000000; padding-bottom: 20px; font-size: 25px; text-align: center; background-color: #f4f4f4; border-radius: 10px;'>%s</div>
                </body></html>
                """.stripTrailing().formatted(fromName, fromName, fromName, fromName, authCode), true);
        } catch (Exception exception) { throw new IllegalStateException("Failed to create verification email form", exception); }
        return mimeMessage;
    }
}
