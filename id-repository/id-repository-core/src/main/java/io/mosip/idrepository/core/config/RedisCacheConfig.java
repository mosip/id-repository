package io.mosip.idrepository.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The Class RedisCacheConfig - this class is a configuration class for redis cache if cache type is redis..
 * 
 * @author Balaji
 */
@ConditionalOnProperty(value = "spring.cache.type", havingValue = "redis")
@Configuration
public class RedisCacheConfig {
    
    @Value("#{${mosip.idrepo.cache.expire-in-seconds}}")
    private Map<String, Integer> cacheNames;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    	Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>();
    	cacheNames.forEach((cacheName, ttl) -> {
            configurationMap.put(cacheName, RedisCacheConfiguration
            					.defaultCacheConfig()
            					.disableCachingNullValues()
                                .entryTtl(Duration.ofSeconds(ttl)));
        });
    	 return RedisCacheManager.builder(connectionFactory)
                 .withInitialCacheConfigurations(configurationMap)
                 .build();
    }
    

}
