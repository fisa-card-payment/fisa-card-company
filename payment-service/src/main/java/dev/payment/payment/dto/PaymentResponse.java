package dev.payment.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.payment.domain.ledger.entity.CardLedger;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "카드 결제 응답 (신용/체크 공통)")
public class PaymentResponse {

    @Schema(description = "거래 참조번호 (카드사 발급 12자리 HEX)", example = "6C2B740A7E58")
    private String rrn;

    @Schema(description = "승인번호 (6자리, 승인 시에만 반환)", example = "123456")
    private String approvalCode;

    @Schema(description = "응답 코드 - 00: 승인, 99: 거절", example = "00")
    private String responseCode;

    @Schema(description = "처리 상태 (APPROVED / REJECTED)", example = "APPROVED")
    private String status;

    @Schema(description = "처리 메시지", example = "승인 완료")
    private String message;

    public static PaymentResponse approved(CardLedger ledger) {
        return PaymentResponse.builder()
                .rrn(ledger.getRrn())
                .approvalCode(ledger.getApprovalCode())
                .responseCode("00")
                .status("APPROVED")
                .message("승인 완료")
                .build();
    }

    public static PaymentResponse approvedCheck(String rrn, String approvalCode) {
        return PaymentResponse.builder()
                .rrn(rrn)
                .approvalCode(approvalCode)
                .responseCode("00")
                .status("APPROVED")
                .message("승인 완료")
                .build();
    }

    public static PaymentResponse rejected(String rrn, String message) {
        return PaymentResponse.builder()
                .rrn(rrn)
                .approvalCode(null)
                .responseCode("99")
                .status("REJECTED")
                .message(message)
                .build();
    }
}
