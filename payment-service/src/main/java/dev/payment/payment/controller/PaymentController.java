package dev.payment.payment.controller;

import dev.payment.global.exception.ErrorCode;
import dev.payment.global.exception.PaymentException;
import dev.payment.payment.dto.PaymentRequest;
import dev.payment.payment.dto.PaymentResponse;
import dev.payment.payment.service.CardMasterService;
import dev.payment.payment.service.CheckPaymentService;
import dev.payment.payment.service.CreditPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Payment", description = "카드 결제 승인 API")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final CreditPaymentService creditPaymentService;
    private final CheckPaymentService checkPaymentService;
    private final CardMasterService cardMasterService;

    @Operation(
            summary = "카드 결제 승인 (API Gateway 진입점)",
            description = "VAN으로부터 전달된 결제 요청을 수신합니다. " +
                          "카드 타입(CREDIT/CHECK)을 조회하여 각 결제 처리 서비스로 라우팅합니다."
    )
    @ApiResponse(responseCode = "200", description = "처리 결과 (responseCode=00: 승인, 99: 거절)",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @PostMapping("/api/payment/approve")
    public ResponseEntity<PaymentResponse> approve(@Valid @RequestBody PaymentRequest request) {
        String cardType = cardMasterService.getCardType(request.getCardNumber());
        log.info("결제 요청 라우팅 - STAN: {}, 카드타입: {}", request.getStan(), cardType);

        PaymentResponse response = switch (cardType) {
            case "CREDIT" -> creditPaymentService.processCredit(request);
            case "CHECK"  -> checkPaymentService.processCheck(request);
            default -> throw new PaymentException(ErrorCode.CARD_NOT_FOUND);
        };

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "신용카드 결제 (직접 호출 / 테스트용)", description = "신용카드 전용 엔드포인트.")
    @PostMapping("/payment/credit")
    private ResponseEntity<PaymentResponse> credit(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(creditPaymentService.processCredit(request));
    }

    @Operation(summary = "체크카드 결제 (직접 호출 / 테스트용)", description = "체크카드 전용 엔드포인트.")
    @PostMapping("/payment/check")
    private ResponseEntity<PaymentResponse> check(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(checkPaymentService.processCheck(request));
    }
}
