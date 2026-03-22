package dev.settlement;

import dev.settlement.global.config.SettlementBankProperties;
import dev.settlement.global.config.VanSseProperties;
import dev.settlement.global.config.VanUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({VanUploadProperties.class, SettlementBankProperties.class, VanSseProperties.class})
public class SettlementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SettlementServiceApplication.class, args);
	}

}
