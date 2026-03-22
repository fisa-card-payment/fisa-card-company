package dev.settlement.contoller;

import dev.settlement.dto.ReconcileOutcome;
import dev.settlement.dto.VanCsvReceiveResult;
import dev.settlement.service.LedgerReconciliationService;
import dev.settlement.service.VanCsvReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final VanCsvReceiveService vanCsvReceiveService;
    private final LedgerReconciliationService ledgerReconciliationService;

    /**
     * VAN → API Gateway: 정산용 CSV 수신 후 임시 저장 및 shared DB 스테이징.
     * <p>
     * TODO: 후속 단계(원장 비교 실패/정산 완료) 알림은 SSE로 VAN에 전달.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return badRequest("파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            return badRequest("CSV 파일만 업로드 가능합니다.");
        }

        log.info("[정산업로드] CSV 파일 수신: {}", originalFilename);

        try {
            VanCsvReceiveResult result = vanCsvReceiveService.receiveAndStage(file, originalFilename);
            ReconcileOutcome reconcile = ledgerReconciliationService.reconcile(result.fileId());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "SUCCESS");
            body.put("message", "CSV 수신·스테이징·원장 대사까지 완료되었습니다.");
            body.put("fileId", result.fileId());
            body.put("fileName", result.fileName());
            body.put("rowCount", result.rowCount());
            body.put("storedPath", result.storedPath());
            body.put("compareStatus", reconcile.fileStatus());
            if (reconcile.message() != null) {
                body.put("compareDetail", reconcile.message());
            }
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("[정산업로드] 요청 오류: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[정산업로드] 처리 실패: {}", e.getMessage(), e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "FAIL");
            error.put("message", "CSV 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "FAIL");
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }
}
