package pizza_cheese.todo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import pizza_cheese.todo.config.AppProperties;

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

    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String fullName, String verificationUrl) {
        if (fromAddress.isBlank()) {
            log.error("Skip verification email to {}: MAIL_FROM / MAIL_USERNAME is not configured", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Pizza Cheese");
            helper.setText(buildVerificationBody(fullName, verificationUrl), true);
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send verification email to {}", toEmail, ex);
        }
    }

    @Async("mailTaskExecutor")
    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String otp, int validityMinutes) {
        if (fromAddress.isBlank()) {
            log.error("Skip password reset OTP email to {}: MAIL_FROM / MAIL_USERNAME is not configured", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Mã OTP đặt lại mật khẩu Pizza Cheese");
            helper.setText(buildPasswordResetOtpBody(fullName, otp, validityMinutes), true);
            mailSender.send(message);
            log.info("Password reset OTP email sent to {}", toEmail);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send password reset OTP email to {}", toEmail, ex);
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

    private String buildPasswordResetOtpBody(String fullName, String otp, int validityMinutes) {
        String name = (fullName == null || fullName.isBlank()) ? "bạn" : fullName.trim();
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #1a1a2e;">
                  <h2>Đặt lại mật khẩu Pizza Cheese</h2>
                  <p>Xin chào %s,</p>
                  <p>Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu. Mã OTP của bạn là:</p>
                  <p style="margin: 28px 0; text-align: center;">
                    <span style="font-size: 32px; letter-spacing: 8px; font-weight: 700; color: #e94560;">%s</span>
                  </p>
                  <p>Mã có hiệu lực trong <strong>%d phút</strong>. Không chia sẻ mã này với bất kỳ ai.</p>
                  <p>Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>
                </div>
                """.formatted(name, otp, validityMinutes);
    }
}
