package dev.settlement.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "settlement.bank")
public class SettlementBankProperties {

    /**카드사 계좌*/
    private String cardCompanyAccount = "9000-0001";

    /** VAN사 계좌 */
    private String vanAccount = "9000-0002";
}
