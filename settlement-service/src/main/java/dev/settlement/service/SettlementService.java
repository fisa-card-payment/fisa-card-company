package dev.settlement.service;

import dev.settlement.dto.VanSettleDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SettlementService {

    /**
     * 업로드된 CSV 파일을 파싱하여 VanSettleDto 목록으로 반환합니다.
     * 첫 번째 줄(헤더)은 건너뜁니다.
     *
     * @param file 업로드된 CSV MultipartFile
     * @return 파싱된 VanSettleDto 목록
     * @throws IOException CSV 파일 읽기 실패 시
     */
    public List<VanSettleDto> parseAndProcess(MultipartFile file) throws IOException {
        List<VanSettleDto> settlementList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 첫 번째 줄(헤더: RRN,STAN,...) 스킵
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 빈 줄 스킵
                if (line.isBlank()) {
                    continue;
                }

                try {
                    VanSettleDto dto = VanSettleDto.fromCsvRow(line);
                    settlementList.add(dto);
                    log.info("[정산처리] rrn={}, stan={}, amount={}, approvalCode={}",
                            dto.getRrn(), dto.getStan(), dto.getAmount(), dto.getApprovalCode());
                } catch (Exception e) {
                    log.warn("[정산처리] CSV 행 파싱 실패 - 건너뜀: line=\"{}\", error={}", line, e.getMessage());
                }
            }
        }

        log.info("[정산처리] 총 {}건 파싱 완료", settlementList.size());
        return settlementList;
    }
}
