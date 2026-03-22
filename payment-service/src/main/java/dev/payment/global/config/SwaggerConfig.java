package dev.payment.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("카드사 결제 승인 서비스 - 신용카드 실시간 승인 처리")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Card Payment Team")))
                .servers(List.of(
                        new Server().url("/").description("Current Server")
                ));
    }
}
