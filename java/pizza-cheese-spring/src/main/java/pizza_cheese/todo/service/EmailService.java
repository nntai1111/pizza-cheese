package pizza_cheese.todo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import pizza_cheese.todo.config.AppProperties;
import pizza_cheese.todo.exception.ApiException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        String configuredFrom = appProperties.getMail().getFrom();
        this.fromAddress = configuredFrom != null ? configuredFrom.trim() : "";
    }

    public void sendVerificationEmail(String toEmail, String fullName, String verificationUrl) {
        if (fromAddress.isBlank()) {
            throw ApiException.badRequest("Chưa cấu hình email gửi (MAIL_FROM / MAIL_USERNAME)");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Pizza Cheese");
            helper.setText(buildVerificationBody(fullName, verificationUrl), true);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send verification email to {}", toEmail, ex);
            throw ApiException.badRequest("Không gửi được email xác thực. Vui lòng thử lại sau.");
        }
    }

    private String buildVerificationBody(String fullName, String verificationUrl) {
        String name = (fullName == null || fullName.isBlank()) ? "bạn" : fullName.trim();
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #1a1a2e;">
                  <h2>Xác thực tài khoản Pizza Cheese</h2>
                  <p>Xin chào %s,</p>
                  <p>Cảm ơn bạn đã đăng ký. Vui lòng bấm nút bên dưới để kích hoạt tài khoản.
                  Link có hiệu lực trong <strong>24 giờ</strong>.</p>
                  <p style="margin: 28px 0;">
                    <a href="%s"
                       style="background:#e94560;color:#fff;padding:12px 24px;border-radius:8px;
                              text-decoration:none;font-weight:600;display:inline-block;">
                      Xác thực email
                    </a>
                  </p>
                  <p>Nếu nút không hoạt động, copy link sau vào trình duyệt:</p>
                  <p style="word-break:break-all;color:#6b7280;">%s</p>
                  <p>Nếu bạn không đăng ký tài khoản này, hãy bỏ qua email.</p>
                </div>
                """.formatted(name, verificationUrl, verificationUrl);
    }
}
