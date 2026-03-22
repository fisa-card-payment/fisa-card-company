package dev.payment.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "신용카드 결제 요청")
public class CreditPaymentRequest {

    @Schema(description = "카드번호 (16자리 숫자)", example = "1234567890120001")
    @NotBlank(message = "카드번호는 필수입니다.")
    @Pattern(
            regexp = "\\d{4}-\\d{4}-\\d{4}-\\d{4}|\\d{16}",
            message = "카드번호는 16자리 숫자 또는 XXXX-XXXX-XXXX-XXXX 형식이어야 합니다."
    )
    private String cardNumber;

    @Schema(description = "결제 금액 (1원 이상)", example = "300000")
    @NotNull(message = "결제 금액은 필수입니다.")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
    private Long amount;

    @Schema(description = "가맹점 ID (최대 15자)", example = "MERCHANT_001")
    @NotBlank(message = "가맹점 ID는 필수입니다.")
    @Size(max = 15, message = "가맹점 ID는 최대 15자입니다.")
    private String merchantId;

    @Schema(description = "STAN - VAN 발급 거래 추적 번호 (6자리 숫자)", example = "123456")
    @NotBlank(message = "STAN은 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "STAN은 6자리 숫자여야 합니다.")
    private String stan;
}
