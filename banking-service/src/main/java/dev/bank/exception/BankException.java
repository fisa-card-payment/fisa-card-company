package dev.bank.exception;

import lombok.Getter;

@Getter
public class BankException extends RuntimeException {

    private final String code;

    public BankException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static BankException accountNotFound(String accountNo) {
        return new BankException("ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다: " + accountNo);
    }

    public static BankException insufficientBalance(String accountNo) {
        return new BankException("INSUFFICIENT_BALANCE", "잔액이 부족합니다: " + accountNo);
    }
}
