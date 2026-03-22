package dev.settlement.dto;

/**
 * 대사 성공 후 은행 정산(이체) 처리 결과.
 *
 * @param settlementStatus {@code SETTLED}, {@code SETTLEMENT_FAIL}, {@code SKIPPED}
 */
public record PayoutOutcome(String settlementStatus, String message) {

    public static PayoutOutcome skipped(String reason) {
        return new PayoutOutcome("SKIPPED", reason);
    }

    public static PayoutOutcome settled() {
        return new PayoutOutcome("SETTLED", null);
    }

    public static PayoutOutcome failed(String reason) {
        return new PayoutOutcome("SETTLEMENT_FAIL", reason);
    }
}
