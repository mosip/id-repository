package io.mosip.idrepository.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public ConcurrentMapCacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}