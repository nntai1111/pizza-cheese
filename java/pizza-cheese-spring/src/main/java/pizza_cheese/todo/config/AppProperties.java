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
	private Jwt jwt = new Jwt();
	private User user = new User();
	private Cors cors = new Cors();
	private Mail mail = new Mail();
	private long emailVerificationTokenValidityInSeconds = 86400;

	@Getter
	@Setter
	public static class Jwt {

		private String base64Secret;
		private long accessTokenValidityInSeconds;
		private long refreshTokenValidityInSeconds;
	}

	@Getter
	@Setter
	public static class User {

		private String defaultAvatarUrl;
	}

	@Getter
	@Setter
	public static class Cors {

		private List<String> allowedOrigins = new ArrayList<>();
	}

	@Getter
	@Setter
	public static class Mail {

		private String from;
	}

	public String paymentReturnUrl() {
		if (frontendUrl == null || frontendUrl.isBlank()) {
			throw new IllegalStateException("Missing app.frontend-url (set APP_FRONTEND_URL or profile properties)");
		}
		return frontendUrl.replaceAll("/+$", "") + "/customer/payment/return";
	}

	public String emailVerificationUrl(String token) {
		if (frontendUrl == null || frontendUrl.isBlank()) {
			throw new IllegalStateException("Missing app.frontend-url (set APP_FRONTEND_URL or profile properties)");
		}
		return frontendUrl.replaceAll("/+$", "") + "/verify-email?token=" + token;
	}
}
