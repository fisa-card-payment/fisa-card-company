package dev.settlement.dto;

// VAN CSV 수신,스테이징 적재 결과
public record VanCsvReceiveResult(long fileId, String fileName, int rowCount, String storedPath) {
}
