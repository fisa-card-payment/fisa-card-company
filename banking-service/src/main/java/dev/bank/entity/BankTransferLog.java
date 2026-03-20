package dev.bank.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transfer_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankTransferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "from_acc", length = 20)
    private String fromAcc;

    @Column(name = "to_acc", length = 20)
    private String toAcc;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "transfer_type", length = 20)
    private String transferType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public BankTransferLog(String fromAcc, String toAcc, Long amount, String transferType) {
        this.fromAcc = fromAcc;
        this.toAcc = toAcc;
        this.amount = amount;
        this.transferType = transferType;
    }
}
