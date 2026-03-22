package dev.settlement.service;

import dev.settlement.dto.VanCsvRow;
import dev.settlement.dto.VanSettleDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SettlementService {

    // CSV읽어 DTO로 반환환
    public List<VanSettleDto> parseAndProcess(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return readCsvRows(reader).stream().map(VanCsvRow::dto).toList();
        }
    }

    // 스테이징 적재
    public List<VanCsvRow> parseCsvFile(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return readCsvRows(reader);
        }
    }

    private List<VanCsvRow> readCsvRows(BufferedReader reader) throws IOException {
        List<VanCsvRow> rows = new ArrayList<>();
        String line;
        boolean isFirstLine = true;
        int lineNo = 0;

        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            try {
                rows.add(new VanCsvRow(lineNo, VanSettleDto.fromCsvRow(line)));
            } catch (Exception e) {
                log.warn("[정산처리] CSV 행 파싱 실패 - 건너뜀: lineNo={}, line=\"{}\", error={}",
                        lineNo, line, e.getMessage());
            }
        }

        log.info("[정산처리] 총 {}건 파싱 완료", rows.size());
        return rows;
    }
}
