package dev.payment.domain.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_master")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardMaster {

    @Id
    @Column(name = "card_number", length = 19)
    private String cardNumber;

    @Column(name = "user_id", length = 20)
    private String userId;

    @Column(name = "card_type", length = 10)
    private String cardType;

    @Column(name = "linked_account", length = 20)
    private String linkedAccount;

    @Column(name = "credit_limit")
    private Long creditLimit;

    @Column(name = "used_amount")
    private Long usedAmount;

    @Column(name = "status", length = 10)
    private String status;

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public boolean isCreditCard() {
        return "CREDIT".equals(this.cardType);
    }

    public boolean isCheckCard() {
        return "CHECK".equals(this.cardType);
    }

    public long getAvailableLimit() {
        return this.creditLimit - this.usedAmount;
    }

    public void deductLimit(long amount) {
        this.usedAmount += amount;
    }

    public void restoreLimit(long amount) {
        this.usedAmount -= amount;
    }
}
