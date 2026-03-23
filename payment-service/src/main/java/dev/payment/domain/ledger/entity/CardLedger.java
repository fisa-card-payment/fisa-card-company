package dev.payment.domain.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_ledger")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "rrn", length = 12, unique = true, nullable = false)
    private String rrn;

    @Column(name = "stan", length = 6, unique = true, nullable = false)
    private String stan;

    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "merchant_id", length = 15, nullable = false)
    private String merchantId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "approval_code", length = 6)
    private String approvalCode;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
