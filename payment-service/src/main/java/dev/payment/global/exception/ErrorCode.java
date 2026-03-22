package dev.payment.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 카드 관련
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY-001", "카드 정보를 찾을 수 없습니다."),
    CARD_INACTIVE(HttpStatus.BAD_REQUEST, "PAY-002", "사용 정지된 카드입니다."),
    CARD_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "PAY-003", "신용카드 전용 엔드포인트입니다."),
    INSUFFICIENT_CREDIT_LIMIT(HttpStatus.BAD_REQUEST, "PAY-004", "신용카드 한도가 부족합니다."),

    // 체크카드 관련
    CHECK_CARD_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "PAY-010", "체크카드 전용 엔드포인트입니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "PAY-011", "계좌 잔액이 부족합니다."),
    BANK_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAY-012", "은행 서버 호출에 실패했습니다."),

    // 중복 처리
    DUPLICATE_STAN(HttpStatus.CONFLICT, "PAY-005", "이미 처리된 거래입니다."),

    // 원장 기록
    LEDGER_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAY-006", "원장 기록 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
