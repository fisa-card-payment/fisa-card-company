package dev.settlement.dto;

/**
 * 스테이징 vs 원장 Replica 대사 결과.
 *
 * @param fileStatus {@code COMPARE_OK} 또는 {@code COMPARE_FAIL} ({@code van_settlement_file.status})
 */
public record ReconcileOutcome(boolean matched, String fileStatus, String message) {

    public static ReconcileOutcome ok() {
        return new ReconcileOutcome(true, "COMPARE_OK", null);
    }

    public static ReconcileOutcome fail(String message) {
        return new ReconcileOutcome(false, "COMPARE_FAIL", message);
    }
}
