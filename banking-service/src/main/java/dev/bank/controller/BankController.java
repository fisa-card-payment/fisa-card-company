package dev.bank.controller;

import dev.bank.dto.BalanceRequest;
import dev.bank.dto.BalanceResponse;
import dev.bank.dto.TransferRequest;
import dev.bank.dto.TransferResponse;
import dev.bank.service.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @PostMapping("/balance")
    public ResponseEntity<BalanceResponse> checkBalance(@Valid @RequestBody BalanceRequest request) {
        return ResponseEntity.ok(bankService.checkBalance(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransferResponse> withdraw(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(bankService.withdraw(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(bankService.transfer(request));
    }
}
