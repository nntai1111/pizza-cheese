package pizza_cheese.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Class này dùng để cấu hình Swagger
@Configuration
public class OpenApiConfig {

        private static final String BEARER_AUTH = "Bearer Authentication";

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Pizza Cheese API")
                                                .description("REST API cho ứng dụng Pizza Store")
                                                .version("v1"))
                                .components(new Components()
                                                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                                                .name(BEARER_AUTH)
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("Nhập JWT access token (không cần thêm tiền tố 'Bearer')")));
        }
}
