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

    private String rrn;
    private String stan;
    private String cardNumber;
    private Long amount;
    private String merchantId;
    private String cardCompany;
    private String approvalCode;
    /** CSV 원문 일시 (ISO-8601 문자열) */
    private String createdAtRaw;

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
        if (fields.length < 8) {
            throw new IllegalArgumentException("CSV 컬럼은 8개여야 합니다. 실제=" + fields.length);
        }
        return VanSettleDto.builder()
                .rrn(fields[0].trim())
                .stan(fields[1].trim())
                .cardNumber(fields[2].trim())
                .amount(Long.parseLong(fields[3].trim()))
                .merchantId(fields[4].trim())
                .cardCompany(fields[5].trim())
                .approvalCode(fields[6].trim())
                .createdAtRaw(fields[7].trim())
                .build();
    }
}
