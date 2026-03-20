package dev.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransferRequest {

    @NotBlank(message = "출금 계좌번호는 필수입니다")
    private String fromAccount;

    @NotBlank(message = "입금 계좌번호는 필수입니다")
    private String toAccount;

    @Positive(message = "금액은 0보다 커야 합니다")
    private Long amount;
}
