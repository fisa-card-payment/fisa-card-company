package dev.payment.payment.service;

import dev.payment.global.exception.ErrorCode;
import dev.payment.global.exception.PaymentException;
import dev.payment.domain.ledger.entity.CardLedger;
import dev.payment.payment.dto.CreditPaymentRequest;
import dev.payment.payment.dto.CreditPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditPaymentService {

    private final CardMasterService cardMasterService;
    private final CardLedgerService cardLedgerService;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 신용카드 결제 처리
     *
     * [흐름]
     * 1. RRN 생성 (카드사 발급 - 12자리 HEX)
     * 2. STAN 중복 확인 (VAN 발급 번호 기준 멱등성 보장)
     * 3. 카드 검증 + 한도 차감 (sharedTransactionManager, Pessimistic Lock)
     * 4. 원장 기록 - APPROVED (sourceTransactionManager)
     *    실패 시 → 보상 트랜잭션으로 한도 복원
     *
     * [분산 트랜잭션 참고]
     * shared DB와 source DB가 분리되어 있어 원자적 처리 불가.
     * 한도 차감 후 원장 기록 실패 시 보상 트랜잭션(compensating transaction)으로 복원.
     */
    public CreditPaymentResponse processCredit(CreditPaymentRequest request) {
        // 1. RRN 생성 (카드사 발급) - 거절 시에도 응답에 포함
        String rrn = generateRrn();
        log.info("신용카드 결제 요청 - STAN: {}, RRN: {}, 금액: {}", request.getStan(), rrn, request.getAmount());

        // 2. STAN 중복 확인 (VAN의 동일 거래 재전송 차단)
        if (cardLedgerService.existsByStan(request.getStan())) {
            log.warn("중복 STAN 요청 차단 - STAN: {}", request.getStan());
            return CreditPaymentResponse.rejected(rrn, "이미 처리된 거래입니다.");
        }

        // 3. 카드 검증 + 한도 차감 (Pessimistic Lock 적용)
        try {
            cardMasterService.validateAndDeductLimit(request.getCardNumber(), request.getAmount());
        } catch (PaymentException e) {
            log.warn("카드 검증 실패 - STAN: {}, 사유: {}", request.getStan(), e.getMessage());
            return CreditPaymentResponse.rejected(rrn, e.getMessage());
        }

        // 4. 원장 기록
        String approvalCode = generateApprovalCode();
        try {
            CardLedger ledger = cardLedgerService.saveLedger(
                    rrn,
                    request.getStan(),
                    request.getCardNumber(),
                    request.getMerchantId(),
                    request.getAmount(),
                    "APPROVED",
                    approvalCode
            );
            log.info("신용카드 결제 승인 완료 - STAN: {}, RRN: {}, 승인번호: {}", request.getStan(), rrn, approvalCode);
            return CreditPaymentResponse.approved(ledger);

        } catch (Exception e) {
            // 보상 트랜잭션: 한도 복원
            log.error("원장 기록 실패, 한도 복원 시작 - STAN: {}, RRN: {}", request.getStan(), rrn, e);
            cardMasterService.restoreLimit(request.getCardNumber(), request.getAmount());
            return CreditPaymentResponse.rejected(rrn, ErrorCode.LEDGER_WRITE_FAILED.getMessage());
        }
    }

    /** RRN 생성 - 카드사 발급 12자리 HEX (예: 6C2B740A7E58) */
    private String generateRrn() {
        byte[] bytes = new byte[6]; // 6 bytes = 12자리 HEX
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /** 승인번호 생성 - 6자리 숫자 */
    private String generateApprovalCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
