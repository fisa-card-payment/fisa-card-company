package dev.settlement.dto.van;


public enum BatchStatusCode {

    /** 원장 대사·정산 입금까지 정상 완료 */
    SUCCESS,

    /** 스테이징 vs 원장 대사 불일치 */
    COMPARE_FAILED,

    /** 대사 성공 후 은행 이체 등 정산 단계 실패 */
    SETTLEMENT_FAILED,

    /** 처리 중 예외 */
    PROCESSING_FAILED;

    /** JSON {@code statusCode} 필드 값 */
    public String code() {
        return name();
    }
}
