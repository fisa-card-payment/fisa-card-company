package dev.settlement.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
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
}