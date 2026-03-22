package dev.payment.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "체크카드 결제 응답")
public class CheckPaymentResponse {

    @Schema(description = "거래 참조번호 (카드사 발급)")
    private String rrn;

    @Schema(description = "승인번호 (6자리, 승인 시에만 반환)")
    private String approvalCode;

    @Schema(description = "응답 코드 (00: 성공, 99: 실패)")
    private String responseCode;

    @Schema(description = "처리 상태 (APPROVED / REJECTED)")
    private String status;

    @Schema(description = "처리 메시지")
    private String message;

    public static CheckPaymentResponse approved(String rrn, String approvalCode) {
        return CheckPaymentResponse.builder()
                .rrn(rrn)
                .approvalCode(approvalCode)
                .responseCode("00")
                .status("APPROVED")
                .message("승인 완료")
                .build();
    }

    public static CheckPaymentResponse rejected(String rrn, String reason) {
        return CheckPaymentResponse.builder()
                .rrn(rrn)
                .approvalCode(null)
                .responseCode("99")
                .status("REJECTED")
                .message(reason)
                .build();
    }
}
