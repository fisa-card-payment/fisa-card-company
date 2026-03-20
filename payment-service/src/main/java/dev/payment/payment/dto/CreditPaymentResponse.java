package dev.payment.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.payment.domain.ledger.entity.CardLedger;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "신용카드 결제 응답")
public class CreditPaymentResponse {

    @Schema(description = "처리 상태 (APPROVED / DECLINED)")
    private String status;

    @Schema(description = "승인번호 (6자리, 승인 시에만 반환)")
    private String approvalCode;

    @Schema(description = "거래 참조번호")
    private String rrn;

    @Schema(description = "처리 메시지")
    private String message;

    @Schema(description = "처리 시각")
    private LocalDateTime approvedAt;

    public static CreditPaymentResponse approved(CardLedger ledger) {
        return CreditPaymentResponse.builder()
                .status("APPROVED")
                .approvalCode(ledger.getApprovalCode())
                .rrn(ledger.getRrn())
                .message("결제가 승인되었습니다.")
                .approvedAt(ledger.getApprovedAt())
                .build();
    }

    public static CreditPaymentResponse declined(String rrn, String reason) {
        return CreditPaymentResponse.builder()
                .status("DECLINED")
                .rrn(rrn)
                .message(reason)
                .approvedAt(LocalDateTime.now())
                .build();
    }
}
