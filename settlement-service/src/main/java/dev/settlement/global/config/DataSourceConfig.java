package dev.settlement.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    // DB 3: 원장 Replica DataSource (Primary - JPA 기본 사용)
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    // DB 2: 공유 마스터 DataSource
    @Bean
    @ConfigurationProperties("spring.datasource.shared")
    public DataSource sharedDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "sharedJdbcTemplate")
    public JdbcTemplate sharedJdbcTemplate(@Qualifier("sharedDataSource") DataSource sharedDataSource) {
        return new JdbcTemplate(sharedDataSource);
    }

    @Bean(name = "replicaJdbcTemplate")
    public JdbcTemplate replicaJdbcTemplate(@Qualifier("replicaDataSource") DataSource replicaDataSource) {
        return new JdbcTemplate(replicaDataSource);
    }

    @Bean
    @Primary
    public PlatformTransactionManager replicaTransactionManager(@Qualifier("replicaDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public PlatformTransactionManager sharedTransactionManager(@Qualifier("sharedDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}