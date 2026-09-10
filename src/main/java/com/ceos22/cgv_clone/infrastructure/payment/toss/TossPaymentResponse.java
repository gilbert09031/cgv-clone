package com.ceos22.cgv_clone.infrastructure.payment.toss;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * 토스 Payment 객체. 승인·취소·조회 API가 모두 이 형태로 응답
 * 날짜는 ISO 8601 오프셋 포함 문자열(yyyy-MM-dd'T'HH:mm:ss±hh:mm)이며, 변환 규칙은 이 레코드가 소유한다.
 */
public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        String orderName,
        Integer totalAmount,
        String status,
        String approvedAt,
        List<Cancel> cancels
) {
    /** 승인 일시를 서버 시간대의 LocalDateTime으로 변환 */
    public LocalDateTime approvedLocalDateTime() {
        return parseToLocalDateTime(approvedAt, "approvedAt");
    }

    /** 누적된 cancels 배열 중 가장 최근 취소 건.*/
    public Cancel latestCancel() {
        if (cancels == null || cancels.isEmpty()) {
            throw new IllegalStateException("토스 취소 응답에 cancels가 없습니다.");
        }
        return cancels.stream()
                .max(Comparator.comparing(Cancel::canceledLocalDateTime))
                .orElseThrow();
    }

    /** cancels 배열의 원소. 취소 건마다 별도의 transactionKey를 갖는다. */
    public record Cancel(
            String transactionKey,
            Integer cancelAmount,
            String cancelReason,
            String canceledAt
    ) {
        /** 취소 일시를 서버 시간대의 LocalDateTime으로 변환 */
        public LocalDateTime canceledLocalDateTime() {
            return parseToLocalDateTime(canceledAt, "canceledAt");
        }
    }

    private static LocalDateTime parseToLocalDateTime(String isoOffsetDateTime, String fieldName) {
        if (isoOffsetDateTime == null) {
            throw new IllegalStateException("토스 응답에 " + fieldName + " 시각 정보가 없습니다.");
        }
        return OffsetDateTime.parse(isoOffsetDateTime)
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
