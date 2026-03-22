package dev.payment.payment.client;

import dev.payment.global.exception.ErrorCode;
import dev.payment.global.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankClient {

    private final RestTemplate restTemplate;

    @Value("${service.bank-url}")
    private String bankUrl;

    /**
     * 은행 서버에 출금 요청 (사용자 계좌 → 카드사 계좌)
     * 잔액 부족 시 false 반환, 통신 오류 시 예외
     */
    public boolean withdraw(String fromAccount, String toAccount, Long amount) {
        String url = bankUrl + "/api/bank/withdraw";
        Map<String, Object> request = Map.of(
                "fromAccount", fromAccount,
                "toAccount", toAccount,
                "amount", amount
        );

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);
            log.info("은행 출금 성공 - from: {}, to: {}, amount: {}", fromAccount, toAccount, amount);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (HttpClientErrorException e) {
            // 잔액 부족 등 은행 서버에서 거절
            log.warn("은행 출금 거절 - from: {}, amount: {}, reason: {}", fromAccount, amount, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("은행 서버 통신 실패", e);
            throw new PaymentException(ErrorCode.BANK_CALL_FAILED);
        }
    }
}
