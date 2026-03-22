package dev.settlement.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankTransferApiResponse(
        boolean success,
        String message,
        Long transferId
) {
}
