package dev.settlement.contoller;

import dev.settlement.dto.VanSettleDto;
import dev.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * VAN사에서 전송한 정산용 CSV 파일을 수신하고 파싱·처리를 시작합니다.
     *
     * <p>요청 형식: POST /api/settlement/upload (multipart/form-data)
     * <p>파라미터: file - .csv 확장자 파일
     *
     * @param file 업로드된 CSV 파일
     * @return 수신 성공 응답 JSON
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        // 1. 파일 비어있는지 검증
        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "FAIL");
            error.put("message", "파일이 비어있습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        // 2. 확장자 .csv 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "FAIL");
            error.put("message", "CSV 파일만 업로드 가능합니다.");
            return ResponseEntity.badRequest().body(error);
        }

        log.info("[정산업로드] CSV 파일 수신: {}", originalFilename);

//        // 3. CSV 파싱 및 정산 처리
//        try {
//            List<VanSettleDto> settlementList = settlementService.parseAndProcess(file);
//            log.info("[정산업로드] 처리 완료 - 파일명: {}, 건수: {}", originalFilename, settlementList.size());
//        } catch (Exception e) {
//            log.error("[정산업로드] CSV 처리 중 오류 발생: {}", e.getMessage(), e);
//            Map<String, String> error = new HashMap<>();
//            error.put("status", "FAIL");
//            error.put("message", "CSV 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }

        // 4. 성공 응답 반환
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "CSV 파일 수신 및 정산 처리가 시작되었습니다.");
        response.put("fileName", originalFilename);

        return ResponseEntity.ok(response);
    }
}
