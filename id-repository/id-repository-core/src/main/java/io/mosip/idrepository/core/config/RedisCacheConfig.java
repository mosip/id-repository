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
import java.util.Map;

@ConditionalOnProperty(value = "spring.cache.type", havingValue = "redis")
@Configuration
public class RedisCacheConfig {

	@Value("${mosip.credential.cache.expire-in-seconds:60}")
    private Integer cacheExpireInSeconds;
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
      Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
      cacheConfigurations.put("credential_transaction", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(cacheExpireInSeconds)));
      
      return RedisCacheManager.builder(connectionFactory)
          .withInitialCacheConfigurations(cacheConfigurations)
          .build();
    }
    

}
