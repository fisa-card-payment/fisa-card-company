package dev.settlement.global.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // 1. 원장 Replica DB 설정 읽기
    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSourceProperties replicaProperties() {
        return new DataSourceProperties();
    }

    // 2. 원장 Replica DataSource 생성 및 기본 DB(Primary)로 설정
    @Bean
    @Primary
    public DataSource replicaDataSource() {
        return replicaProperties().initializeDataSourceBuilder().build();
    }

    // 3. 공유 마스터 DB 설정 읽기
    @Bean
    @ConfigurationProperties("spring.datasource.shared")
    public DataSourceProperties sharedProperties() {
        return new DataSourceProperties();
    }

    // 4. 공유 마스터 DataSource 생
    @Bean
    public DataSource sharedDataSource() {
        return sharedProperties().initializeDataSourceBuilder().build();
    }
}