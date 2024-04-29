package io.mosip.idrepository.core.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.CacheBuilder;

@ConditionalOnProperty(value = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
@Configuration
public class SimpleCacheConfig extends CachingConfigurerSupport {
	
	    @Value("${mosip.credential.cache.size:100}")
	    private Integer cacheMaxSize;

	    @Value("${mosip.credential.cache.expire-in-seconds:60}")
	    private Integer cacheExpireInSeconds;


	    @Bean
	    @Override
	    public CacheManager cacheManager() {
	        SimpleCacheManager cacheManager = new SimpleCacheManager();
	        cacheManager.setCaches(Arrays.asList(buildMapCache("credential_transaction")));
	        return cacheManager;
	    }

	    private ConcurrentMapCache buildMapCache(String name) {
	        return new ConcurrentMapCache(name,
	                CacheBuilder.newBuilder()
	                        .expireAfterWrite(cacheExpireInSeconds, TimeUnit.SECONDS)
	                        .maximumSize(cacheMaxSize)
	                        .build()
	                        .asMap(), true);
	    }
	
}
