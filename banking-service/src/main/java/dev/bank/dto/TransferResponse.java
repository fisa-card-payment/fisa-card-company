package dev.bank.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResponse {
    private boolean success;
    private String message;
    private Long transferId;
}
