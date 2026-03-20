package dev.payment.payment.service;

import dev.payment.domain.ledger.entity.CardLedger;
import dev.payment.domain.ledger.repository.CardLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardLedgerService {

    private final CardLedgerRepository cardLedgerRepository;

    /**
     * STAN 중복 확인 (멱등성 보장)
     * STAN은 VAN이 발급하는 거래 추적 번호 - 동일 STAN 재전송 차단
     */
    @Transactional(readOnly = true)
    public boolean existsByStan(String stan) {
        return cardLedgerRepository.existsByStan(stan);
    }

    /**
     * 원장 기록 (sourceTransactionManager - @Primary이므로 생략 가능하나 명시)
     * RRN은 카드사가 생성하여 전달
     */
    @Transactional
    public CardLedger saveLedger(String rrn, String stan, String cardNumber,
                                 String merchantId, long amount, String status, String approvalCode) {
        CardLedger ledger = CardLedger.builder()
                .rrn(rrn)
                .stan(stan)
                .cardNumber(cardNumber)
                .merchantId(merchantId)
                .amount(amount)
                .status(status)
                .approvalCode(approvalCode)
                .approvedAt(LocalDateTime.now())
                .build();

        return cardLedgerRepository.save(ledger);
    }
}
