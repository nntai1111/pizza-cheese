package pizza_cheese.todo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.cache.redis-enabled=false")
class TodoApplicationTests {

	@Test
	void contextLoads() {
	}

}
