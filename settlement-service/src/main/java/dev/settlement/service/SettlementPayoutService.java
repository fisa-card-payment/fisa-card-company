package dev.settlement.service;

import dev.settlement.client.BankTransferClient;
import dev.settlement.dto.PayoutOutcome;
import dev.settlement.dto.bank.BankTransferApiRequest;
import dev.settlement.exception.BankTransferException;
import dev.settlement.global.config.SettlementBankProperties;
import dev.settlement.global.util.SettlementStringUtils;
import dev.settlement.repository.VanSettlementFileDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 일치 확인 완료에 대해 정산{@code COMPARE_OK})
 * 
 * 가맹점 입금액 = 금액 × (1 − fee_rate)
 * VAN입금액 = 수수료의 절반(반올림)
 */
@Slf4j
@Service
public class SettlementPayoutService {

    private final JdbcTemplate sharedJdbcTemplate;
    private final BankTransferClient bankTransferClient;
    private final SettlementBankProperties bankProperties;
    private final VanSettlementFileDao fileDao;

    public SettlementPayoutService(
            @Qualifier("sharedJdbcTemplate") JdbcTemplate sharedJdbcTemplate,
            BankTransferClient bankTransferClient,
            SettlementBankProperties bankProperties,
            VanSettlementFileDao fileDao) {
        this.sharedJdbcTemplate = sharedJdbcTemplate;
        this.bankTransferClient = bankTransferClient;
        this.bankProperties = bankProperties;
        this.fileDao = fileDao;
    }

    public PayoutOutcome settleComparedFile(long fileId) {
        FileHead head;
        try {
            head = sharedJdbcTemplate.queryForObject(
                    "SELECT status, row_count FROM van_settlement_file WHERE id = ?",
                    (rs, rowNum) -> new FileHead(rs.getString("status"), rs.getInt("row_count")),
                    fileId);
        } catch (EmptyResultDataAccessException e) {
            return PayoutOutcome.failed("van_settlement_file 없음: id=" + fileId);
        }

        if (head == null) {
            return PayoutOutcome.failed("van_settlement_file 없음: id=" + fileId);
        }

        if (!"COMPARE_OK".equals(head.status())) {
            return PayoutOutcome.skipped("상태가 COMPARE_OK 가 아님: " + head.status());
        }

        if (head.rowCount() == 0) {
            fileDao.updateStatus(fileId, "SETTLED", null);
            log.info("[정산입금] 스테이징 0건 — SETTLED 처리 fileId={}", fileId);
            return PayoutOutcome.settled();
        }

        int stagingCount = sharedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM van_settlement_staging WHERE file_id = ?",
                Integer.class,
                fileId);

        List<PayoutLine> lines = sharedJdbcTemplate.query(
                """
                        SELECT s.amount, s.merchant_id, m.settle_account, m.fee_rate
                        FROM van_settlement_staging s
                        INNER JOIN merchant_master m ON m.merchant_id = s.merchant_id
                        WHERE s.file_id = ?
                        ORDER BY s.line_no
                        """,
                (rs, rowNum) -> PayoutLine.from(rs),
                fileId);

        if (lines.size() != stagingCount) {
            String msg = "가맹점 마스터에 없는 merchant_id가 포함됨 (staging=" + stagingCount + ", matched=" + lines.size() + ")";
            fileDao.updateStatus(fileId, "SETTLEMENT_FAIL", SettlementStringUtils.truncate(msg, 500));
            return PayoutOutcome.failed(msg);
        }

        String fromAcc = bankProperties.getCardCompanyAccount();
        String vanAcc = bankProperties.getVanAccount();

        try {
            int line = 0;
            for (PayoutLine pl : lines) {
                line++;
                BigDecimal amountBd = BigDecimal.valueOf(pl.amount());
                BigDecimal fee = amountBd.multiply(pl.feeRate()).setScale(0, RoundingMode.HALF_UP);
                long merchantPay = amountBd.subtract(fee).longValue();
                long vanShare = fee.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP).longValue();

                if (pl.settleAccount().isBlank()) {
                    throw new BankTransferException("line " + line + " 정산 계좌 없음: merchantId=" + pl.merchantId());
                }
                if (merchantPay <= 0) {
                    throw new BankTransferException("line " + line + " 가맹점 입금액이 0 이하: amount=" + pl.amount());
                }

                bankTransferClient.transfer(new BankTransferApiRequest(fromAcc, pl.settleAccount(), merchantPay));
                log.info("[정산입금] 가맹점 이체 line={} to={} amount={}", line, pl.settleAccount(), merchantPay);

                if (vanShare > 0) {
                    bankTransferClient.transfer(new BankTransferApiRequest(fromAcc, vanAcc, vanShare));
                    log.info("[정산입금] VAN 수수료 분배 line={} to={} amount={}", line, vanAcc, vanShare);
                }
            }
            fileDao.updateStatus(fileId, "SETTLED", null);
            return PayoutOutcome.settled();
        } catch (BankTransferException e) {
            log.error("[정산입금] 이체 실패 fileId={}: {}", fileId, e.getMessage());
            String msg = SettlementStringUtils.truncate(e.getMessage(), 500);
            fileDao.updateStatus(fileId, "SETTLEMENT_FAIL", msg);
            return PayoutOutcome.failed(e.getMessage());
        }
    }


    private record FileHead(String status, int rowCount) {
    }

    private record PayoutLine(long amount, String merchantId, String settleAccount, BigDecimal feeRate) {

        static PayoutLine from(ResultSet rs) throws SQLException {
            String acc = rs.getString("settle_account");
            BigDecimal rate = rs.getBigDecimal("fee_rate");
            return new PayoutLine(
                    rs.getLong("amount"),
                    rs.getString("merchant_id"),
                    acc == null ? "" : acc.trim(),
                    rate != null ? rate : BigDecimal.ZERO);
        }
    }
}
