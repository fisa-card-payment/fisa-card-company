package dev.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VanSettleDto {

    private String rrn;           // 거래 참조 번호 (RRN)
    private String stan;          // 시스템 추적 감사 번호 (STAN)
    private Long amount;          // 거래 금액
    private String approvalCode;  // 승인 코드

    /**
     * CSV 한 줄(row)을 파싱하여 VanSettleDto 객체로 변환합니다.
     * CSV 컬럼 순서: RRN,STAN,CARD_NUMBER,AMOUNT,MERCHANT_ID,CARD_COMPANY,APPROVAL_CODE,CREATED_AT
     * 6C2B740A7E58,123456,412345******2345,50000,MERCHANT001,신한,AB1234,2026-03-20T10:00:00
     *
     * @param csvRow CSV 파일의 데이터 행 문자열
     * @return 파싱된 VanSettleDto 객체
     */
    public static VanSettleDto fromCsvRow(String csvRow) {
        String[] fields = csvRow.split(",", -1);
        return VanSettleDto.builder()
                .rrn(fields[0].trim())
                .stan(fields[1].trim())
                .amount(Long.parseLong(fields[2].trim()))
                .approvalCode(fields[3].trim())
                .build();
    }
}
