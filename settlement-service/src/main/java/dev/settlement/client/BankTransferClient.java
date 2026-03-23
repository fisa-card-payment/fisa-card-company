package dev.settlement.client;

import dev.settlement.dto.bank.BankTransferApiRequest;
import dev.settlement.dto.bank.BankTransferApiResponse;
import dev.settlement.exception.BankTransferException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;


@Component
public class BankTransferClient {

    private final RestClient bankRestClient;

    public BankTransferClient(@Qualifier("bankRestClient") RestClient bankRestClient) {
        this.bankRestClient = bankRestClient;
    }

    public BankTransferApiResponse transfer(BankTransferApiRequest request) {
        try {
            BankTransferApiResponse body = bankRestClient.post()
                    .uri("/api/bank/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BankTransferApiResponse.class);
            if (body == null || !body.success()) {
                throw new BankTransferException("은행 이체 응답 비정상: " + body);
            }
            return body;
        } catch (RestClientResponseException e) {
            throw new BankTransferException(
                    "은행 이체 실패 HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(),
                    e);
        }
    }
}
