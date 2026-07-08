package pizza_cheese.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import pizza_cheese.todo.config.AppProperties;
import pizza_cheese.todo.config.CacheProperties;
import pizza_cheese.todo.config.VnPayProperties;

@SpringBootApplication(exclude = {
		RedisAutoConfiguration.class,
		RedisRepositoriesAutoConfiguration.class,
})
@EnableConfigurationProperties({ AppProperties.class, VnPayProperties.class, CacheProperties.class })
public class TodoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodoApplication.class, args);
	}

}
