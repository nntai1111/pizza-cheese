package pizza_cheese.todo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /** tự động xóa khoảng trắng trong mail.password trước khi đăng nhập SMTP. */
    @Bean
    public BeanPostProcessor mailPasswordNormalizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof JavaMailSenderImpl sender && sender.getPassword() != null) {
                    sender.setPassword(sender.getPassword().replaceAll("\\s+", ""));
                }
                return bean;
            }
        };
    }

    @Bean
    ApplicationRunner mailConfigStartupCheck(AppProperties appProperties, JavaMailSenderImpl mailSender) {
        return args -> {
            String from = appProperties.getMail() != null ? appProperties.getMail().getFrom() : null;
            String username = mailSender.getUsername();
            boolean fromOk = StringUtils.hasText(from);
            boolean userOk = StringUtils.hasText(username);
            boolean passOk = StringUtils.hasText(mailSender.getPassword());
            if (fromOk && userOk && passOk) {
                log.info("Mail SMTP ready: host={} port={} user={} from={}",
                        mailSender.getHost(), mailSender.getPort(), username, from.trim());
            } else {
                log.error(
                        "Mail SMTP NOT configured (fromOk={} userOk={} passwordSet={}). "
                                + "Set MAIL_USERNAME / MAIL_PASSWORD / MAIL_FROM in server .env "
                                + "then recreate backend: docker compose up -d --force-recreate backend",
                        fromOk, userOk, passOk);
            }
        };
    }
}
