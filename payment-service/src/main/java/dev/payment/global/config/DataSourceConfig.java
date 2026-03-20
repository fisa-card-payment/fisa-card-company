package dev.payment.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // DB 1: 원장 Source DataSource (Primary - JPA 기본 사용)
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.source")
    public DataSource sourceDataSource() {
        return DataSourceBuilder.create().build();
    }

    // DB 2: 공유 마스터 DataSource
    @Bean
    @ConfigurationProperties("spring.datasource.shared")
    public DataSource sharedDataSource() {
        return DataSourceBuilder.create().build();
    }
}
