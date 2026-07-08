package pizza_cheese.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean redisEnabled = false;
    private long menuTtlSeconds = 600;
}
