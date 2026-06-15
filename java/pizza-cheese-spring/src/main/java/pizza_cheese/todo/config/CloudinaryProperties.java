package pizza_cheese.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

	private String cloudName;
	private String apiKey;
	private String apiSecret;
	private String folderAvatar = "pizza-store/avatar";
	private String folderPizza = "pizza-store/pizza";
	private String folderCategory = "pizza-store/category";

	public String getCloudName() {
		return cloudName;
	}

	public void setCloudName(String cloudName) {
		this.cloudName = cloudName;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public String getFolderAvatar() {
		return folderAvatar;
	}

	public void setFolderAvatar(String folderAvatar) {
		this.folderAvatar = folderAvatar;
	}

	public String getFolderPizza() {
		return folderPizza;
	}

	public void setFolderPizza(String folderPizza) {
		this.folderPizza = folderPizza;
	}

	public String getFolderCategory() {
		return folderCategory;
	}

	public void setFolderCategory(String folderCategory) {
		this.folderCategory = folderCategory;
	}
}
