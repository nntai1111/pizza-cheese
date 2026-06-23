package pizza_cheese.todo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private String frontendUrl;
	private Cors cors = new Cors();

	@Getter
	@Setter
	public static class Cors {

		private List<String> allowedOrigins = new ArrayList<>();
	}

	public String paymentReturnUrl() {
		return frontendUrl.replaceAll("/+$", "") + "/customer/payment/return";
	}
}
