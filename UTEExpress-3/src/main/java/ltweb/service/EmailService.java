// ltweb/service/EmailService.java

package ltweb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;


@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Xác thực tài khoản UTEExpress";
        String verificationUrl = frontendUrl + "/customer/verify?token=" + token;

        String message = "Xin chào,\n\n"
                + "Vui lòng click vào link dưới đây để xác thực tài khoản của bạn:\n"
                + verificationUrl + "\n\n"
                + "Link này sẽ hết hạn sau 24 giờ.\n\n"
                + "Trân trọng,\n"
                + "UTEExpress Team";

        sendEmail(toEmail, subject, message);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String subject = "Đặt lại mật khẩu UTEExpress";
        String resetUrl = frontendUrl + "/customer/reset-password?token=" + token;

        String message = "Xin chào,\n\n"
                + "Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng click vào link dưới đây:\n"
                + resetUrl + "\n\n"
                + "Link này sẽ hết hạn sau 24 giờ.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\n"
                + "UTEExpress Team";

        sendEmail(toEmail, subject, message);
    }

    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}