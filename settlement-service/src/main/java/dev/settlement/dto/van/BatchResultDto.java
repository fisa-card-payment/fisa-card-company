package dev.settlement.dto.van;

// POST /api/van/sse/batch-result 요청
public record BatchResultDto(String batchDate, String statusCode, String message) {
}
