package dev.payment.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.payment.domain.ledger.entity.CardLedger;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "신용카드 결제 응답")
public class CreditPaymentResponse {

    @Schema(description = "거래 참조번호 (카드사 발급 12자리 HEX)", example = "6C2B740A7E58")
    private String rrn;

    @Schema(description = "승인번호 (6자리, 승인 시에만 반환)", example = "AB1234")
    private String approvalCode;

    @Schema(description = "응답 코드 - 00: 승인, 99: 거절", example = "00")
    private String responseCode;

    @Schema(description = "처리 상태 (APPROVED / REJECTED)")
    private String status;

    @Schema(description = "처리 메시지", example = "승인 완료")
    private String message;

    public static CreditPaymentResponse approved(CardLedger ledger) {
        return CreditPaymentResponse.builder()
                .rrn(ledger.getRrn())
                .approvalCode(ledger.getApprovalCode())
                .responseCode("00")
                .status("APPROVED")
                .message("승인 완료")
                .build();
    }

    public static CreditPaymentResponse rejected(String rrn, String message) {
        return CreditPaymentResponse.builder()
                .rrn(rrn)
                .approvalCode(null)
                .responseCode("99")
                .status("REJECTED")
                .message(message)
                .build();
    }
}
