package dev.settlement.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// van_settlement_file 테이블 상태 업데이트
@Repository
public class VanSettlementFileDao {

    private final JdbcTemplate sharedJdbcTemplate;

    public VanSettlementFileDao(
            @Qualifier("sharedJdbcTemplate") JdbcTemplate sharedJdbcTemplate) {
        this.sharedJdbcTemplate = sharedJdbcTemplate;
    }

    /**
     * 파일의 처리 상태와 오류 메시지를 갱신합니다.
     *
     * @param fileId       van_settlement_file.id
     * @param status       처리 상태 (예: COMPARE_OK, COMPARE_FAIL, SETTLED,
     *                     SETTLEMENT_FAIL)
     * @param errorMessage 오류 메시지. 정상 처리 시 {@code null}
     */
    public void updateStatus(long fileId, String status, String errorMessage) {
        sharedJdbcTemplate.update(
                "UPDATE van_settlement_file SET status = ?, error_message = ? WHERE id = ?",
                status,
                errorMessage,
                fileId);
    }
}
