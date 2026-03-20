package dev.payment.domain.ledger.repository;

import dev.payment.domain.ledger.entity.CardLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardLedgerRepository extends JpaRepository<CardLedger, Long> {

    boolean existsByRrn(String rrn);
}
