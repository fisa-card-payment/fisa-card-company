package dev.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BalanceRequest {

    @NotBlank(message = "계좌번호는 필수입니다")
    private String accountNo;
}
