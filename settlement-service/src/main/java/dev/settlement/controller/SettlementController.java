package dev.settlement.contoller;

import dev.settlement.dto.VanCsvReceiveResult;
import dev.settlement.service.SettlementAsyncProcessor;
import dev.settlement.service.VanCsvReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private static final Pattern BATCH_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final VanCsvReceiveService vanCsvReceiveService;
    private final SettlementAsyncProcessor settlementAsyncProcessor;

    /**
     * VAN → 카드사: CSV 수신·스테이징 후 즉시 응답.
     * {@code batchDate}(yyyy-MM-dd)는 multipart 폼 필드로 필수.
     * 원장 대사·입금·VAN 알림(SSE 트리거)은 비동기로 진행됩니다.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @RequestPart("file") MultipartFile file,
            @RequestParam("batchDate") String batchDate) {

        String batchDateNorm = batchDate.trim();
        if (batchDateNorm.isEmpty() || !BATCH_DATE.matcher(batchDateNorm).matches()) {
            return badRequest("batchDate는 yyyy-MM-dd 형식의 필수 값입니다.");
        }

        if (file.isEmpty()) {
            return badRequest("파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            return badRequest("CSV 파일만 업로드 가능합니다.");
        }

        log.info("[정산업로드] CSV 수신: {} batchDate={}", originalFilename, batchDateNorm);

        try {
            VanCsvReceiveResult result = vanCsvReceiveService.receiveAndStage(file, originalFilename);
            settlementAsyncProcessor.continueAfterStaging(result.fileId(), batchDateNorm);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fileName", result.fileName());
            body.put("message", "CSV 파일 수신 및 정산 처리가 시작되었습니다.");
            body.put("status", "SUCCESS");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("[정산업로드] 요청 오류: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[정산업로드] 스테이징 실패: {}", e.getMessage(), e);
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
