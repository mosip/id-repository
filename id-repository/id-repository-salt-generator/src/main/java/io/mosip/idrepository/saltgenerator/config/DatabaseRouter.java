//package io.mosip.idrepository.saltgenerator.config;
//
//import io.mosip.idrepository.saltgenerator.constant.DatabaseType;
//import jakarta.persistence.EntityManagerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@EnableTransactionManagement
//@EnableJpaRepositories(
//        basePackages = "io.mosip.idrepository.saltgenerator",
//        entityManagerFactoryRef = "entityManagerFactory",
//        transactionManagerRef = "transactionManager"
//)
//public class DatabaseRouter {
//
//    @Bean(name = "primaryDataSource")
//    @ConfigurationProperties(prefix = "datasource.primary")
//    public DataSource primaryDataSource() {
//        return DataSourceBuilder.create().build();  // ✅ Use DataSourceBuilder (Auto-detect properties)
//    }
//
//    @Bean(name = "secondaryDataSource")
//    @ConfigurationProperties(prefix = "datasource.secondary")
//    public DataSource secondaryDataSource() {
//        return DataSourceBuilder.create().build();  // ✅ Use DataSourceBuilder
//    }
//
//    @Bean
//    @Primary
//    public DataSource dataSource() {
//        Map<Object, Object> targetDataSources = new HashMap<>();
//        targetDataSources.put(DatabaseType.SECONDARY, secondaryDataSource());
//        targetDataSources.put(DatabaseType.PRIMARY, primaryDataSource());
//
//        RoutingDataSource routingDataSource = new RoutingDataSource();
//        routingDataSource.setTargetDataSources(targetDataSources);
//        routingDataSource.setDefaultTargetDataSource(primaryDataSource());
//        routingDataSource.afterPropertiesSet();
//
//        return routingDataSource;
//    }
//
//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
//            EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {
//        return builder
//                .dataSource(dataSource)
//                .packages("io.mosip.idrepository.saltgenerator.entity")
//                .persistenceUnit("default")
//                .build();
//    }
//
//    @Bean
//    public PlatformTransactionManager transactionManager(
//            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
//}
//
//
//
