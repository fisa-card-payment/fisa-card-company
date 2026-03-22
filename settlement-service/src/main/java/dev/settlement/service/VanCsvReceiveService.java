package dev.settlement.service;

import dev.settlement.dto.VanCsvReceiveResult;
import dev.settlement.dto.VanCsvRow;
import dev.settlement.dto.VanSettleDto;
import dev.settlement.global.config.VanUploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

/**
 * 임시 파일 저장 후 shared DB 스테이징 적재.
 * TODO: 정산 비교/입금 결과를 VAN으로 SSE 푸시
 */
@Slf4j
@Service
public class VanCsvReceiveService {

    private final SettlementService settlementService;
    private final VanUploadProperties vanUploadProperties;
    private final JdbcTemplate sharedJdbcTemplate;

    public VanCsvReceiveService(
            SettlementService settlementService,
            VanUploadProperties vanUploadProperties,
            @Qualifier("sharedJdbcTemplate") JdbcTemplate sharedJdbcTemplate) {
        this.settlementService = settlementService;
        this.vanUploadProperties = vanUploadProperties;
        this.sharedJdbcTemplate = sharedJdbcTemplate;
    }

    @Transactional(transactionManager = "sharedTransactionManager")
    public VanCsvReceiveResult receiveAndStage(MultipartFile file, String originalFilename) throws IOException {
        Path baseDir = Paths.get(vanUploadProperties.getTempDir()).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);

        String safeName = Paths.get(originalFilename).getFileName().toString();
        if (safeName.contains("..") || safeName.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        }

        Path storedPath = baseDir.resolve(UUID.randomUUID() + "_" + safeName);
        file.transferTo(storedPath.toFile());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        sharedJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO van_settlement_file (file_name, stored_path, status, row_count)
                            VALUES (?, ?, 'RECEIVED', 0)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, originalFilename);
            ps.setString(2, storedPath.toString());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("van_settlement_file PK 조회 실패");
        }
        long fileId = key.longValue();

        List<VanCsvRow> rows = settlementService.parseCsvFile(storedPath);
        if (!rows.isEmpty()) {
            String insertStaging = """
                    INSERT INTO van_settlement_staging
                    (file_id, line_no, rrn, stan, card_number, amount, merchant_id, card_company, approval_code, created_at_raw)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            sharedJdbcTemplate.batchUpdate(insertStaging, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    VanCsvRow row = rows.get(i);
                    VanSettleDto d = row.dto();
                    ps.setLong(1, fileId);
                    ps.setInt(2, row.lineNo());
                    ps.setString(3, d.getRrn());
                    ps.setString(4, d.getStan());
                    ps.setString(5, d.getCardNumber());
                    ps.setLong(6, d.getAmount());
                    ps.setString(7, d.getMerchantId());
                    ps.setString(8, d.getCardCompany());
                    ps.setString(9, d.getApprovalCode());
                    ps.setString(10, d.getCreatedAtRaw());
                }

                @Override
                public int getBatchSize() {
                    return rows.size();
                }
            });
        }

        sharedJdbcTemplate.update(
                "UPDATE van_settlement_file SET status = 'STAGED', row_count = ? WHERE id = ?",
                rows.size(),
                fileId);

        log.info("[VAN-CSV] 스테이징 완료 fileId={}, fileName={}, rows={}, path={}",
                fileId, originalFilename, rows.size(), storedPath);

        return new VanCsvReceiveResult(fileId, originalFilename, rows.size(), storedPath.toString());
    }
}
