package dev.settlement.global.util;

/**
 * settlement-service 공통 문자열 유틸리티.
 */
public final class SettlementStringUtils {

    private SettlementStringUtils() {
    }

    /**
     * 문자열이 {@code max}자를 초과하면 뒤를 잘라 "..."을 붙여 반환합니다.
     */
    public static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
