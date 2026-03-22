package dev.settlement.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BankRestClientConfig {

    @Bean(name = "bankRestClient")
    public RestClient bankRestClient(@Value("${service.bank-url}") String bankBaseUrl) {
        return RestClient.builder()
                .baseUrl(bankBaseUrl)
                .build();
    }
}
