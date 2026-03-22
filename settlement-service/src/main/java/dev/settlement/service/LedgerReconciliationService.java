package dev.settlement.service;

import dev.settlement.dto.ReconcileOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VAN 스테이징(shared)과 원장 Replica({@code card_ledger}) 건별 대사.
 */
@Slf4j
@Service
public class LedgerReconciliationService {

    private static final int MAX_ERROR_LEN = 500;

    private final JdbcTemplate sharedJdbcTemplate;
    private final JdbcTemplate replicaJdbcTemplate;

    public LedgerReconciliationService(
            @Qualifier("sharedJdbcTemplate") JdbcTemplate sharedJdbcTemplate,
            @Qualifier("replicaJdbcTemplate") JdbcTemplate replicaJdbcTemplate) {
        this.sharedJdbcTemplate = sharedJdbcTemplate;
        this.replicaJdbcTemplate = replicaJdbcTemplate;
    }

    @Transactional(transactionManager = "sharedTransactionManager")
    public ReconcileOutcome reconcile(long fileId) {
        List<StagingRow> staging = loadStaging(fileId);
        if (staging.isEmpty()) {
            markFile(fileId, "COMPARE_OK", null);
            log.info("[대사] 건수 0 → COMPARE_OK fileId={}", fileId);
            return ReconcileOutcome.ok();
        }

        Set<String> seenKeys = new HashSet<>();
        for (StagingRow s : staging) {
            String k = key(s.rrn, s.stan);
            if (!seenKeys.add(k)) {
                String msg = "스테이징에 동일 RRN+STAN 중복: " + truncate(k, 120);
                markFile(fileId, "COMPARE_FAIL", msg);
                return ReconcileOutcome.fail(msg);
            }
        }

        Map<String, LedgerRow> ledgerByKey;
        try {
            ledgerByKey = loadLedger(staging);
        } catch (Exception e) {
            log.error("[대사] 원장 조회 실패 fileId={}", fileId, e);
            String msg = truncate("원장 조회 오류: " + e.getMessage(), MAX_ERROR_LEN);
            markFile(fileId, "COMPARE_FAIL", msg);
            return ReconcileOutcome.fail(msg);
        }

        List<String> problems = new ArrayList<>();
        for (StagingRow s : staging) {
            String k = key(s.rrn, s.stan);
            LedgerRow leg = ledgerByKey.get(k);
            if (leg == null) {
                problems.add("원장 없음 line=" + s.lineNo + " rrn=" + s.rrn + " stan=" + s.stan);
                continue;
            }
            if (s.amount != leg.amount) {
                problems.add("금액 불일치 line=" + s.lineNo + " staging=" + s.amount + " ledger=" + leg.amount);
            }
            if (!s.merchantId.equals(leg.merchantId)) {
                problems.add("가맹점 불일치 line=" + s.lineNo);
            }
            if (!approvalMatches(s.approvalCode, leg.approvalCode)) {
                problems.add("승인번호 불일치 line=" + s.lineNo);
            }
            if (!cardMatches(s.cardNumber, leg.cardNumber)) {
                problems.add("카드번호 불일치 line=" + s.lineNo);
            }
        }

        if (!problems.isEmpty()) {
            String msg = truncate(String.join("; ", problems), MAX_ERROR_LEN);
            markFile(fileId, "COMPARE_FAIL", msg);
            log.warn("[대사] 불일치 fileId={} {}", fileId, msg);
            return ReconcileOutcome.fail(msg);
        }

        markFile(fileId, "COMPARE_OK", null);
        log.info("[대사] 일치 fileId={} rows={}", fileId, staging.size());
        return ReconcileOutcome.ok();
    }

    private void markFile(long fileId, String status, String errorMessage) {
        sharedJdbcTemplate.update(
                "UPDATE van_settlement_file SET status = ?, error_message = ? WHERE id = ?",
                status,
                errorMessage,
                fileId);
    }

    private List<StagingRow> loadStaging(long fileId) {
        return sharedJdbcTemplate.query(
                """
                        SELECT line_no, rrn, stan, card_number, amount, merchant_id, approval_code
                        FROM van_settlement_staging
                        WHERE file_id = ?
                        ORDER BY line_no
                        """,
                (rs, rowNum) -> StagingRow.from(rs),
                fileId);
    }

    private Map<String, LedgerRow> loadLedger(List<StagingRow> staging) {
        List<Object> args = new ArrayList<>();
        StringBuilder in = new StringBuilder();
        for (StagingRow s : staging) {
            if (!in.isEmpty()) {
                in.append(", ");
            }
            in.append("(?, ?)");
            args.add(s.rrn);
            args.add(s.stan);
        }
        String sql = """
                SELECT rrn, stan, card_number, merchant_id, amount, approval_code
                FROM card_ledger
                WHERE (rrn, stan) IN (%s)
                """.formatted(in);
        return replicaJdbcTemplate.query(sql, rs -> {
            Map<String, LedgerRow> map = new HashMap<>();
            while (rs.next()) {
                LedgerRow row = LedgerRow.from(rs);
                map.put(key(row.rrn, row.stan), row);
            }
            return map;
        }, args.toArray());
    }

    private static String key(String rrn, String stan) {
        return rrn.trim() + "\u0001" + stan.trim();
    }

    private static boolean approvalMatches(String staging, String ledger) {
        if (staging == null && ledger == null) {
            return true;
        }
        if (staging == null || ledger == null) {
            return false;
        }
        return staging.trim().equalsIgnoreCase(ledger.trim());
    }

    /**
     * 마스킹된 카드번호(예: 412345******2345)는 원장 번호의 끝자리와 일치하면 통과.
     */
    static boolean cardMatches(String stagingCard, String ledgerCard) {
        if (stagingCard == null || ledgerCard == null) {
            return false;
        }
        String s = stagingCard.trim();
        String l = ledgerCard.trim();
        if (s.contains("*")) {
            int lastStar = s.lastIndexOf('*');
            String tail = s.substring(lastStar + 1).replaceAll("[^0-9]", "");
            String ledgerDigits = l.replaceAll("[^0-9]", "");
            return !tail.isEmpty() && ledgerDigits.endsWith(tail);
        }
        String sd = s.replaceAll("[^0-9]", "");
        String ld = l.replaceAll("[^0-9]", "");
        return sd.equals(ld);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private record StagingRow(int lineNo, String rrn, String stan, String cardNumber, long amount,
                                String merchantId, String approvalCode) {

        static StagingRow from(ResultSet rs) throws SQLException {
            String mid = rs.getString("merchant_id");
            String rrn = rs.getString("rrn");
            String stan = rs.getString("stan");
            return new StagingRow(
                    rs.getInt("line_no"),
                    rrn == null ? "" : rrn.trim(),
                    stan == null ? "" : stan.trim(),
                    rs.getString("card_number"),
                    rs.getLong("amount"),
                    mid == null ? "" : mid.trim(),
                    rs.getString("approval_code"));
        }
    }

    private record LedgerRow(String rrn, String stan, String cardNumber, long amount,
                               String merchantId, String approvalCode) {

        static LedgerRow from(ResultSet rs) throws SQLException {
            String mid = rs.getString("merchant_id");
            String rrn = rs.getString("rrn");
            String stan = rs.getString("stan");
            return new LedgerRow(
                    rrn == null ? "" : rrn.trim(),
                    stan == null ? "" : stan.trim(),
                    rs.getString("card_number"),
                    rs.getLong("amount"),
                    mid != null ? mid.trim() : "",
                    rs.getString("approval_code"));
        }
    }
}
