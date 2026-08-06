package pizza_cheese.todo.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

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
}
