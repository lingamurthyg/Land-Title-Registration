package com.trianz.ltr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * RedisConfig - Redis configuration for distributed session management.
 *
 * CLOUD-NATIVE FEATURES:
 *   - AWS ElastiCache Redis integration
 *   - Distributed session storage for horizontal scaling
 *   - Lettuce client (async, thread-safe)
 *   - JSON serialization for session data
 *
 * REPLACES:
 *   - WebSphere distributed session replication
 *   - JBoss cluster state management
 *   - Application server-specific session clustering
 *
 * AWS ELASTICACHE CONFIGURATION:
 *   - REDIS_HOST: ElastiCache Redis endpoint
 *   - REDIS_PORT: Redis port (default: 6379)
 *   - REDIS_PASSWORD: Redis AUTH password (if enabled)
 *
 * SESSION CONFIGURATION:
 *   - Sessions stored in Redis with 30-minute timeout
 *   - Automatic session cleanup
 *   - Compatible with ECS/EKS multi-instance deployments
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * Redis connection factory using Lettuce client.
     * Lettuce is preferred for cloud deployments (async, reactive support).
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate for custom Redis operations.
     * Uses JSON serialization for complex objects.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
