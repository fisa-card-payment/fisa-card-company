package dev.settlement.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * VAN은 {@code GET /api/van/sse/subscribe/{batchDate}} 로 구독하고,
 * 카드사(정산)는 {@code POST /api/van/sse/batch-result} 로 JSON
 * {@code { "batchDate", "statusCode", "message" }} 를 보냅니다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "settlement.van-sse")
public class VanSseProperties {

    private boolean enabled = true;

    private String baseUrl;
}
