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
     * 1. RRN 중복 확인 (멱등성)
     * 2. 카드 검증 + 한도 차감 (sharedTransactionManager, Pessimistic Lock)
     * 3. 원장 기록 - APPROVED (sourceTransactionManager)
     *    실패 시 → 보상 트랜잭션으로 한도 복원
     *
     * [분산 트랜잭션 참고]
     * shared DB와 source DB가 분리되어 있어 원자적 처리 불가.
     * 한도 차감 후 원장 기록 실패 시 보상 트랜잭션(compensating transaction)으로 복원.
     */
    public CreditPaymentResponse processCredit(CreditPaymentRequest request) {
        log.info("신용카드 결제 요청 - RRN: {}, 금액: {}", request.getRrn(), request.getAmount());

        // 1. RRN 중복 확인
        if (cardLedgerService.existsByRrn(request.getRrn())) {
            throw new PaymentException(ErrorCode.DUPLICATE_RRN);
        }

        // 2. 카드 검증 + 한도 차감 (Pessimistic Lock 적용)
        cardMasterService.validateAndDeductLimit(request.getCardNumber(), request.getAmount());

        // 3. 원장 기록
        String approvalCode = generateApprovalCode();
        try {
            CardLedger ledger = cardLedgerService.saveLedger(
                    request.getRrn(),
                    request.getCardNumber(),
                    request.getMerchantId(),
                    request.getAmount(),
                    "APPROVED",
                    approvalCode
            );
            log.info("신용카드 결제 승인 완료 - RRN: {}, 승인번호: {}", request.getRrn(), approvalCode);
            return CreditPaymentResponse.approved(ledger);

        } catch (Exception e) {
            // 보상 트랜잭션: 한도 복원
            log.error("원장 기록 실패, 한도 복원 시작 - RRN: {}", request.getRrn(), e);
            cardMasterService.restoreLimit(request.getCardNumber(), request.getAmount());
            throw new PaymentException(ErrorCode.LEDGER_WRITE_FAILED);
        }
    }

    /** 6자리 숫자 승인번호 생성 */
    private String generateApprovalCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
