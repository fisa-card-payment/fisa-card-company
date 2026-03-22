package dev.settlement.dto.bank;

public record BankTransferApiRequest(String fromAccount, String toAccount, Long amount) {
}