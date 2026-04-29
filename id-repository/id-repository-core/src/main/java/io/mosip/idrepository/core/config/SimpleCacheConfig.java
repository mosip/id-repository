package io.mosip.idrepository.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.CacheBuilder;


/**
 * The Class SimpleCacheConfig - this class is a configuration for simple spring cache.
 * 
 * @author Balaji
 */
@ConditionalOnProperty(value = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
@Configuration
public class SimpleCacheConfig extends CachingConfigurerSupport {
    
    @Value("#{'${mosip.idrepo.cache.names}'.split(',')}")
    private List<String> cacheNames;

    @Value("#{${mosip.idrepo.cache.size}}")
    private Map<String, Integer> cacheMaxSize;

    @Value("#{${mosip.idrepo.cache.expire-in-seconds}}")
    private Map<String, Integer> cacheExpireInSeconds;


    @Bean
    @Override
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<Cache> caches = new ArrayList<>();
        for(String name : cacheNames) {
        		caches.add(buildMapCache(name));
        }
        cacheManager.setCaches(caches);
        return cacheManager;
    }
	
    private ConcurrentMapCache buildMapCache(String name) {
        return new ConcurrentMapCache(name,
                CacheBuilder.newBuilder()
                        .maximumSize(cacheMaxSize.getOrDefault(name, 100))
                		.expireAfterWrite(cacheExpireInSeconds.get(name), TimeUnit.SECONDS)
                        .build()
                        .asMap(), true);
    }
}
