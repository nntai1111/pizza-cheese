package pizza_cheese.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

	private String cloudName;
	private String apiKey;
	private String apiSecret;
	private String folderAvatar = "pizza-store/avatar";
	private String folderPizza = "pizza-store/pizza";
	private String folderCategory = "pizza-store/category";
}
