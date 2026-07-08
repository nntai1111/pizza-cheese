package pizza_cheese.todo.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "app.cache.redis-enabled", havingValue = "true")
    static class RedisCacheSetup {

        @Bean
        RedisConnectionFactory redisConnectionFactory(
                @Value("${spring.data.redis.host}") String host,
                @Value("${spring.data.redis.port}") int port,
                @Value("${spring.data.redis.password:}") String password) {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }
            return new LettuceConnectionFactory(config);
        }

        @Bean
        CacheManager cacheManager(
                RedisConnectionFactory connectionFactory,
                CacheProperties cacheProperties,
                ObjectMapper objectMapper) {
            GenericJackson2JsonRedisSerializer serializer =
                    new GenericJackson2JsonRedisSerializer(objectMapper.copy());

            RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(cacheProperties.getMenuTtlSeconds()))
                    .disableCachingNullValues()
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaults)
                    .build();
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.cache.redis-enabled", havingValue = "false", matchIfMissing = true)
    static class InMemoryCacheSetup {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    MenuCacheNames.PIZZAS,
                    MenuCacheNames.CATEGORIES,
                    MenuCacheNames.COMBOS,
                    MenuCacheNames.TOPPINGS);
        }
    }
}
