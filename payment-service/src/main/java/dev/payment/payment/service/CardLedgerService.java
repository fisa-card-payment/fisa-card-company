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
     * RRN 중복 확인 (멱등성 보장)
     * DB에 UNIQUE 제약이 있으나 애플리케이션 레벨에서 선제 차단
     */
    @Transactional(readOnly = true)
    public boolean existsByRrn(String rrn) {
        return cardLedgerRepository.existsByRrn(rrn);
    }

    /**
     * 원장 기록 (sourceTransactionManager - @Primary이므로 생략 가능하나 명시)
     */
    @Transactional
    public CardLedger saveLedger(String rrn, String cardNumber, String merchantId,
                                 long amount, String status, String approvalCode) {
        CardLedger ledger = CardLedger.builder()
                .rrn(rrn)
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
