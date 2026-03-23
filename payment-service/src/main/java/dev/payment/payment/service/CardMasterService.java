package dev.payment.payment.service;

import dev.payment.domain.card.entity.CardMaster;
import dev.payment.domain.card.repository.CardMasterRepository;
import dev.payment.global.exception.ErrorCode;
import dev.payment.global.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardMasterService {

    private final CardMasterRepository cardMasterRepository;

    /**
     * 카드 유효성 검증 + 한도 차감 (Pessimistic Lock으로 동시성 보호)
     * sharedTransactionManager 트랜잭션 내에서 실행
     */
    @Transactional("sharedTransactionManager")
    public CardMaster validateAndDeductLimit(String cardNumber, long amount) {
        CardMaster card = cardMasterRepository.findByCardNumberWithLock(cardNumber)
                .orElseThrow(() -> new PaymentException(ErrorCode.CARD_NOT_FOUND));

        if (!card.isActive()) {
            throw new PaymentException(ErrorCode.CARD_INACTIVE);
        }
        if (!card.isCreditCard()) {
            throw new PaymentException(ErrorCode.CARD_TYPE_MISMATCH);
        }
        if (card.getAvailableLimit() < amount) {
            throw new PaymentException(ErrorCode.INSUFFICIENT_CREDIT_LIMIT);
        }

        card.deductLimit(amount);
        log.info("신용한도 차감 완료 - 카드: {}, 차감액: {}, 잔여한도: {}",
                maskCardNumber(cardNumber), amount, card.getAvailableLimit());

        return card;
    }

    /**
     * 카드 타입 조회 - approve 엔드포인트 라우팅 전용 (락 없는 단순 조회)
     */
    @Transactional(value = "sharedTransactionManager", readOnly = true)
    public String getCardType(String cardNumber) {
        return cardMasterRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new PaymentException(ErrorCode.CARD_NOT_FOUND))
                .getCardType();
    }

    /**
     * 보상 트랜잭션: 원장 기록 실패 시 한도 복원
     */
    @Transactional("sharedTransactionManager")
    public void restoreLimit(String cardNumber, long amount) {
        cardMasterRepository.findByCardNumberWithLock(cardNumber).ifPresent(card -> {
            card.restoreLimit(amount);
            log.warn("보상 트랜잭션 실행 - 카드: {}, 복원액: {}", maskCardNumber(cardNumber), amount);
        });
    }

    private String maskCardNumber(String cardNumber) {
        // 로그에 카드번호 전체 노출 방지: 앞 6자리, 뒤 4자리만 표시
        String digits = cardNumber.replace("-", "");
        return digits.substring(0, 6) + "******" + digits.substring(digits.length() - 4);
    }
}
