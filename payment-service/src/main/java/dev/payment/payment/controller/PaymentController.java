package dev.payment.payment.controller;

import dev.payment.payment.dto.CreditPaymentRequest;
import dev.payment.payment.dto.CreditPaymentResponse;
import dev.payment.payment.service.CreditPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "카드 결제 승인 API")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final CreditPaymentService creditPaymentService;

    @Operation(
            summary = "신용카드 결제 승인",
            description = "VAN으로부터 전달된 신용카드 결제 요청을 처리합니다. " +
                          "카드 한도를 확인하고 차감 후 원장에 기록합니다. 은행 호출 없음."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 완료",
                    content = @Content(schema = @Schema(implementation = CreditPaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "한도 부족 / 카드 상태 오류 / 입력값 오류"),
            @ApiResponse(responseCode = "404", description = "카드 정보 없음"),
            @ApiResponse(responseCode = "409", description = "중복 RRN"),
            @ApiResponse(responseCode = "500", description = "원장 기록 실패")
    })
    @PostMapping("/credit")
    public ResponseEntity<CreditPaymentResponse> creditPayment(
            @Valid @RequestBody CreditPaymentRequest request) {
        return ResponseEntity.ok(creditPaymentService.processCredit(request));
    }
}
