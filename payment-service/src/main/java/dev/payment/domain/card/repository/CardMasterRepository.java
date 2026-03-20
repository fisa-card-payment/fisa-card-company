package dev.payment.domain.card.repository;

import dev.payment.domain.card.entity.CardMaster;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardMasterRepository extends JpaRepository<CardMaster, String> {

    /**
     * 동시 결제 요청 시 Race Condition 방지를 위해 Pessimistic Write Lock 적용.
     * 같은 카드로 동시 요청이 들어올 경우 한 트랜잭션이 완료될 때까지 나머지는 대기.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CardMaster c WHERE c.cardNumber = :cardNumber")
    Optional<CardMaster> findByCardNumberWithLock(@Param("cardNumber") String cardNumber);
}
