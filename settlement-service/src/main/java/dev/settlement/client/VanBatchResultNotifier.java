package dev.settlement.client;

import dev.settlement.dto.PayoutOutcome;
import dev.settlement.dto.ReconcileOutcome;
import dev.settlement.dto.VanSseNotifyResult;
import dev.settlement.dto.van.BatchResultDto;
import dev.settlement.dto.van.BatchStatusCode;
import dev.settlement.global.config.VanSseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * VAN이 구독 중인 배치에 결과를 밀어 넣기 위한 호출.
 * <p>
 * {@code POST /api/van/sse/batch-result} — JSON 본문 {@link BatchResultDto}
 * ({@code batchDate}, {@code statusCode}, {@code message}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VanBatchResultNotifier {

    private final VanSseProperties vanSseProperties;

    public VanSseNotifyResult notifyBatchOutcome(String batchDate, ReconcileOutcome reconcile, PayoutOutcome payout) {
        BatchResultDto dto = buildDto(batchDate, reconcile, payout);
        return post(dto);
    }

    /**
     * 스테이징 이후 비동기 처리 중 예외 등 — {@link BatchStatusCode#PROCESSING_FAILED}.
     */
    public VanSseNotifyResult notifyBatchFailure(String batchDate, String errorMessage) {
        String msg = errorMessage != null && !errorMessage.isBlank()
                ? errorMessage
                : "정산 비동기 처리 중 오류가 발생했습니다.";
        return post(new BatchResultDto(batchDate, BatchStatusCode.PROCESSING_FAILED.code(), msg));
    }

    private static BatchResultDto buildDto(String batchDate, ReconcileOutcome reconcile, PayoutOutcome payout) {
        if (!reconcile.matched()) {
            String msg = reconcile.message() != null ? reconcile.message() : "원장 대사 불일치";
            return new BatchResultDto(batchDate, BatchStatusCode.COMPARE_FAILED.code(), msg);
        }
        if ("SETTLED".equals(payout.settlementStatus())) {
            return new BatchResultDto(
                    batchDate,
                    BatchStatusCode.SUCCESS.code(),
                    "대사 및 정산 입금이 완료되었습니다.");
        }
        if ("SETTLEMENT_FAIL".equals(payout.settlementStatus())) {
            String msg = payout.message() != null ? payout.message() : "정산 입금 실패";
            return new BatchResultDto(batchDate, BatchStatusCode.SETTLEMENT_FAILED.code(), msg);
        }
        String msg = payout.message() != null ? payout.message() : "정산 단계를 완료하지 못했습니다.";
        return new BatchResultDto(batchDate, BatchStatusCode.SETTLEMENT_FAILED.code(), msg);
    }

    private VanSseNotifyResult post(BatchResultDto dto) {
        if (!vanSseProperties.isEnabled()) {
            return VanSseNotifyResult.skippedDisabled();
        }
        String base = vanSseProperties.getBaseUrl() == null ? "" : vanSseProperties.getBaseUrl().trim();
        if (base.isEmpty()) {
            return VanSseNotifyResult.skippedNoBaseUrl();
        }
        if (dto.batchDate() == null || dto.batchDate().isBlank()) {
            return VanSseNotifyResult.skippedNoBatchDate();
        }

        try {
            RestClient client = RestClient.builder().baseUrl(base).build();
            client.post()
                    .uri("/api/van/sse/batch-result")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[VAN-SSE] batch-result 전송 batchDate={} statusCode={}", dto.batchDate(), dto.statusCode());
            return VanSseNotifyResult.sent(dto);
        } catch (RestClientResponseException e) {
            String msg = "HTTP " + e.getStatusCode().value() + " " + e.getResponseBodyAsString();
            log.warn("[VAN-SSE] batch-result 실패 batchDate={} {}", dto.batchDate(), msg);
            return VanSseNotifyResult.failed(msg);
        } catch (Exception e) {
            log.warn("[VAN-SSE] batch-result 실패 batchDate={}: {}", dto.batchDate(), e.getMessage());
            return VanSseNotifyResult.failed(e.getMessage());
        }
    }
}
