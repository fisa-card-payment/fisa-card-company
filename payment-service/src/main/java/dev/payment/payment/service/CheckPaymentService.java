package dev.payment.payment.service;

import dev.payment.domain.card.entity.CardMaster;
import dev.payment.domain.card.repository.CardMasterRepository;
import dev.payment.domain.ledger.entity.CardLedger;
import dev.payment.global.exception.ErrorCode;
import dev.payment.global.exception.PaymentException;
import dev.payment.payment.client.BankClient;
import dev.payment.payment.dto.CheckPaymentRequest;
import dev.payment.payment.dto.CheckPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckPaymentService {

    private final CardMasterRepository cardMasterRepository;
    private final CardLedgerService cardLedgerService;
    private final BankClient bankClient;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${service.card-company-account:9000-0001}")
    private String cardCompanyAccount;

    /**
     * 체크카드 결제 처리
     *
     * [흐름]
     * 1. RRN 생성
     * 2. 카드 검증 (존재, 활성, 체크카드 여부)
     * 3. 은행 서버에 출금 요청 (사용자 계좌 → 카드사 계좌)
     *    실패 시 → 잔액 부족 거절 응답
     * 4. 원장 기록 - APPROVED (sourceTransactionManager)
     */
    public CheckPaymentResponse processCheck(CheckPaymentRequest request) {
        // 1. RRN 생성
        String rrn = generateRrn();
        log.info("체크카드 결제 요청 - RRN: {}, 금액: {}", rrn, request.getAmount());

        // 2. 카드 검증 (체크카드는 card_master 수정 없으므로 락 불필요)
        CardMaster card = cardMasterRepository.findByCardNumber(request.getCardNumber())
                .orElseThrow(() -> new PaymentException(ErrorCode.CARD_NOT_FOUND));

        if (!card.isActive()) {
            throw new PaymentException(ErrorCode.CARD_INACTIVE);
        }
        if (!card.isCheckCard()) {
            throw new PaymentException(ErrorCode.CHECK_CARD_TYPE_MISMATCH);
        }

        // 3. 은행 서버에 출금 요청
        boolean withdrawn = bankClient.withdraw(card.getLinkedAccount(), cardCompanyAccount, request.getAmount());

        if (!withdrawn) {
            log.warn("체크카드 결제 거절 (잔액 부족) - RRN: {}", rrn);
            return CheckPaymentResponse.rejected(rrn, "승인 거절");
        }

        // 4. 원장 기록
        String approvalCode = generateApprovalCode();
        cardLedgerService.saveLedger(
                rrn,
                request.getStan(),
                request.getCardNumber(),
                request.getMerchantId(),
                request.getAmount(),
                "APPROVED",
                approvalCode
        );

        log.info("체크카드 결제 승인 완료 - RRN: {}, 승인번호: {}", rrn, approvalCode);
        return CheckPaymentResponse.approved(rrn, approvalCode);
    }

    private String generateApprovalCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String generateRrn() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
