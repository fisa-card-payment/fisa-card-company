package dev.settlement.dto;

import dev.settlement.dto.van.BatchResultDto;

/**
 * VAN {@code /api/van/sse/batch-result} 호출 결과
 */
public record VanSseNotifyResult(String code, String detail) {

    /** 알림 전송 성공 */
    public static VanSseNotifyResult sent(BatchResultDto dto) {
        return new VanSseNotifyResult(
                "SENT",
                "batchDate=" + dto.batchDate() + ", statusCode=" + dto.statusCode() + ", message=" + dto.message());
    }

    public static VanSseNotifyResult skippedDisabled() {
        return new VanSseNotifyResult("SKIPPED", "settlement.van-sse.enabled=false");
    }

    public static VanSseNotifyResult skippedNoBaseUrl() {
        return new VanSseNotifyResult("SKIPPED", "settlement.van-sse.base-url 비어 있음");
    }

    public static VanSseNotifyResult skippedNoBatchDate() {
        return new VanSseNotifyResult("SKIPPED", "batchDate 미전달 — VAN SSE 알림 생략");
    }

    public static VanSseNotifyResult failed(String message) {
        return new VanSseNotifyResult("FAILED", message);
    }
}
