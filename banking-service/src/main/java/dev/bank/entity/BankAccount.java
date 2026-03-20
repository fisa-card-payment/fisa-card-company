package dev.bank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bank_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankAccount {

    @Id
    @Column(name = "account_no", length = 20)
    private String accountNo;

    @Column(name = "owner_name", length = 50)
    private String ownerName;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "account_type", length = 10)
    private String accountType;

    public void withdraw(Long amount) {
        this.balance -= amount;
    }

    public void deposit(Long amount) {
        this.balance += amount;
    }
}
