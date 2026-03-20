package dev.bank.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BalanceResponse {
    private String accountNo;
    private Long balance;
    private String ownerName;
}
