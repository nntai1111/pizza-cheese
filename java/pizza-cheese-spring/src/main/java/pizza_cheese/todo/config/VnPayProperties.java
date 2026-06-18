package pizza_cheese.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.vnpay")
public class VnPayProperties {

    private String tmnCode;
    private String hashSecret;
    private String payUrl;
    private String returnUrl;
    private String ipnUrl;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String locale = "vn";
    private String currCode = "VND";
}
