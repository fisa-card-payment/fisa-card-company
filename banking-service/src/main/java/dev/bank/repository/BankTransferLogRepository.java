package dev.bank.repository;

import dev.bank.entity.BankTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransferLogRepository extends JpaRepository<BankTransferLog, Long> {
}
