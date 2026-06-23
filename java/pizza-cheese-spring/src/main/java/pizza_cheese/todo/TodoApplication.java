package pizza_cheese.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import pizza_cheese.todo.config.AppProperties;
import pizza_cheese.todo.config.VnPayProperties;

@SpringBootApplication
@EnableConfigurationProperties({ AppProperties.class, VnPayProperties.class })
public class TodoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodoApplication.class, args);
	}

}
