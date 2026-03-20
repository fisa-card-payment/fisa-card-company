package dev.bank.service;

import dev.bank.dto.BalanceRequest;
import dev.bank.dto.BalanceResponse;
import dev.bank.dto.TransferRequest;
import dev.bank.dto.TransferResponse;
import dev.bank.entity.BankAccount;
import dev.bank.entity.BankTransferLog;
import dev.bank.exception.BankException;
import dev.bank.repository.BankAccountRepository;
import dev.bank.repository.BankTransferLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransferLogRepository bankTransferLogRepository;

    @Transactional(readOnly = true)
    public BalanceResponse checkBalance(BalanceRequest request) {
        BankAccount account = bankAccountRepository.findById(request.getAccountNo())
                .orElseThrow(() -> BankException.accountNotFound(request.getAccountNo()));

        return BalanceResponse.builder()
                .accountNo(account.getAccountNo())
                .balance(account.getBalance())
                .ownerName(account.getOwnerName())
                .build();
    }

    @Transactional
    public TransferResponse withdraw(TransferRequest request) {
        BankAccount fromAccount = bankAccountRepository.findById(request.getFromAccount())
                .orElseThrow(() -> BankException.accountNotFound(request.getFromAccount()));

        BankAccount toAccount = bankAccountRepository.findById(request.getToAccount())
                .orElseThrow(() -> BankException.accountNotFound(request.getToAccount()));

        if (fromAccount.getBalance() < request.getAmount()) {
            throw BankException.insufficientBalance(request.getFromAccount());
        }

        fromAccount.withdraw(request.getAmount());
        toAccount.deposit(request.getAmount());

        BankTransferLog log = BankTransferLog.builder()
                .fromAcc(request.getFromAccount())
                .toAcc(request.getToAccount())
                .amount(request.getAmount())
                .transferType("APPROVAL")
                .build();
        bankTransferLogRepository.save(log);

        return TransferResponse.builder()
                .success(true)
                .message("출금 완료")
                .transferId(log.getTransferId())
                .build();
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        BankAccount fromAccount = bankAccountRepository.findById(request.getFromAccount())
                .orElseThrow(() -> BankException.accountNotFound(request.getFromAccount()));

        BankAccount toAccount = bankAccountRepository.findById(request.getToAccount())
                .orElseThrow(() -> BankException.accountNotFound(request.getToAccount()));

        if (fromAccount.getBalance() < request.getAmount()) {
            throw BankException.insufficientBalance(request.getFromAccount());
        }

        fromAccount.withdraw(request.getAmount());
        toAccount.deposit(request.getAmount());

        BankTransferLog log = BankTransferLog.builder()
                .fromAcc(request.getFromAccount())
                .toAcc(request.getToAccount())
                .amount(request.getAmount())
                .transferType("SETTLE")
                .build();
        bankTransferLogRepository.save(log);

        return TransferResponse.builder()
                .success(true)
                .message("이체 완료")
                .transferId(log.getTransferId())
                .build();
    }
}
