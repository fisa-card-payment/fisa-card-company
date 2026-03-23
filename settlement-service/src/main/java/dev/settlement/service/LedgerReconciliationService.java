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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VAN 스테이징(shared)과 원장 Replica({@code card_ledger}) 건별 대사.
 */
@Slf4j
@Service
public class LedgerReconciliationService {

    private static final int MAX_ERROR_LEN = 500;

    /** VAN CSV 카드번호: 앞 6자리(BIN) + ****** + 뒤 4자리 */
    private static final Pattern VAN_MASKED_CARD = Pattern.compile("^(\\d{6})\\*{6}(\\d{4})$");

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
     * VAN CSV는 카드번호가 항상 마스킹으로 온다: 앞 6자리 + ****** + 뒤 4자리.
     * 원장은 전체 번호(하이픈 등 가능) — 숫자만 뽑아 앞 6·뒤 4가 마스킹과 같으면 일치.
     */
    static boolean cardMatches(String maskedCardFromVanCsv, String fullCardFromLedger) {
        if (maskedCardFromVanCsv == null || fullCardFromLedger == null) {
            return false;
        }
        String compact = maskedCardFromVanCsv.trim().replaceAll("[^0-9*]", "");
        Matcher m = VAN_MASKED_CARD.matcher(compact);
        if (!m.matches()) {
            return false;
        }
        String first6 = m.group(1);
        String last4 = m.group(2);
        String ledgerDigits = fullCardFromLedger.trim().replaceAll("[^0-9]", "");
        return ledgerDigits.length() >= 10
                && ledgerDigits.startsWith(first6)
                && ledgerDigits.endsWith(last4);
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
