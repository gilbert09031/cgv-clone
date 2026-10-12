# 예약 도메인 로그 이벤트

[카탈로그](README.md) · [공통 계약](common.md) · 계약 상태: 초안

## 대상 API

| API (`httpMethod` + `httpRoute`) | 처리 |
|---|---|
| `GET /api/schedules` | 날짜·영화·영화관 조건으로 일정 조회 |
| `GET /api/schedules/{scheduleId}/seats` | 좌석 조회 |
| `POST /api/reservations/reserve` | 예약 생성·기존 예약 대체 |
| `GET /api/reservations/my` | 내 예약 조회 |

API 목록은 로그 발생이나 입력 필드의 자동 기록을 요구하지 않는다. 쿼리 조건은 `httpRoute`에 포함하지 않는다. 결제 승인 요청에서 발생한 예약 사건도 `reservation.*` 이벤트로 이 문서에서 관리한다. `operation`·`domain`을 별도로 붙이지 않는다.

## 상태 변화 이벤트

기본 메타데이터와 최소 공통 필드는 [공통 계약](common.md)을 따른다. 아래 필드는 각 이벤트에서만 요구하며 다른 예약 로그에 자동으로 붙이지 않는다. 이벤트 이름으로 확정되는 예약·좌석 상태는 반복하지 않는다.

| 이벤트 | 레벨 | 발생 상황·기록 시점 | 필수 필드 | 기록 책임 | 현재 상태 |
|---|---|---|---|---|---|
| `reservation.created` | INFO | 예약 PENDING·좌석 HELD 생성 트랜잭션 커밋 후 | `reservationId`, `scheduleId`, `seatCount`, `amount`, `holdExpiresAt` | 예약 생성 유스케이스의 커밋 완료 지점 | 구현 완료: 커밋·롤백 회귀 검증 |
| `reservation.cancelled` | INFO | 동일 회원·일정의 기존 PENDING 예약을 새 예약으로 대체. 신규 예약 생성까지 포함한 트랜잭션 커밋 후 | 이전 `reservationId`, `replacementReservationId`, `reason=reservation_replaced` | 예약 생성 유스케이스의 커밋 완료 지점 | 구현 완료: 대체 취소 커밋·롤백과 신규 예약 연결 검증 |
| `reservation.seats_preempted` | INFO | 승인 직전 좌석 잠금·선점 연장과 payment APPROVING 전이가 함께 커밋됨 | `reservationId`, `scheduleId`, `seatCount`, `holdExpiresAt`, `paymentNumber`, `gatewayOrderId` | 승인 선점 트랜잭션의 커밋 완료 지점 | 구현 완료: 커밋 후 필드·결제 시도 연결 검증 |
| `reservation.confirmed` | INFO | 승인 기록·예약 CONFIRMED·좌석 RESERVED가 함께 커밋됨 | `reservationId`, `paymentNumber`, `gatewayOrderId` | 승인 기록 트랜잭션의 커밋 완료 지점 | 구현 완료: 커밋 후 필드·결제 시도 연결 검증 |

### 이벤트 필드 정의

아래 표는 이름·타입의 정의이며 일괄 출력 목록이 아니다. 필수 여부는 위 이벤트 표를 따른다. 선택 필드는 값을 확인하지 못했거나 진단에 필요하지 않으면 생략한다.

| 필드 | 타입 | 의미·단위·적용 |
|---|---|---|
| `reservationId` | number | 해당 사건의 예약 ID. 취소에서는 이전 예약 ID |
| `replacementReservationId` | number | 대체 취소를 일으킨 신규 예약 ID |
| `scheduleId` | number | 생성·선점 사건의 상영 일정 ID |
| `seatCount` | number | 생성·선점 사건의 예약 연결 좌석 수 |
| `amount` | number | 생성한 예약의 정수 원화 금액 |
| `holdExpiresAt` | string | 생성·연장 시 실제 만료 시각. offset ISO 8601. 기존 LocalDateTime.now()와 동일한 JVM 기본 시간대를 변환 기준으로 사용 |
| `paymentNumber`, `gatewayOrderId` | string | 선점·확정 사건을 결제·승인 시도에 연결. 의미는 [결제 문서](payment.md) 참조 |
| `reason` | string | 대체 취소 사유. 현재 허용 값은 `reservation_replaced`. 취소 경로 확장 시 다시 정의 |

취소의 `seatCount`(number)는 진단에 필요한 경우에만 선택적으로 추가한다. `seatCount`는 해당 예약에 연결된 좌석 수다. 생성에서는 중복 제거 후 확보한 좌석 수이며 요청한 좌석 수와 혼동하지 않는다. 취소에서는 취소된 예약에 연결된 좌석 수이며 실제 해제 수를 뜻하지 않는다. 다른 예약이 이미 재선점한 좌석은 해제가 no-op이고, 새 예약이 같은 트랜잭션에서 다시 선점할 수도 있으므로 취소 이벤트로 최종 좌석 가용 상태를 선언하지 않는다.

좌석별 INFO 로그·전체 좌석 배열은 기본 계약에 포함하지 않는다. 대체 취소는 이전 예약당 한 번의 상태 변화이며 신규 예약 ID로 연결한다. 후속 생성이 롤백되면 취소 성공도 기록하지 않는다.

## 실패·거절 이벤트

요청 검증·비즈니스 거절은 응답으로 전달하고 별도 `reservation.*_rejected` 이벤트는 만들지 않는다. 최종 거절 경계의 공통 `api.request.rejected` DEBUG 진단 이벤트만 허용한다. 예상 밖 오류와 도메인 상태 변화는 별도 계약을 따른다.

| 상황 | 처리·오류 코드 |
|---|---|
| 좌석 조회의 일정 없음 | 공통 DEBUG 거절 분류. `S003` SCHEDULE_NOT_FOUND |
| 예약 생성의 회원·일정·좌석 없음 | 공통 DEBUG 거절 분류. `A003`, `S003`, `T004` |
| 다른 예약이 선점·확정한 좌석 | 공통 DEBUG 거절 분류. `R001` SEAT_ALREADY_RESERVED |
| 기존 예약의 결제 승인 진행 중 | 공통 DEBUG 거절 분류. `R004` RESERVATION_PAYMENT_IN_PROGRESS |
| 선점 만료·소유 관계 변경 | 공통 DEBUG 거절 분류. `R003` SEAT_HOLD_EXPIRED |
| 결제 준비·선점 시 예약 없음·유효하지 않은 상태 | 공통 DEBUG 거절 분류. `R005`, `R002` |
| 본인 소유가 아닌 예약 결제 | 공통 DEBUG 거절 분류. `A002` AUTHORIZATION_FAILED |
| 내 예약 조회의 회원·조회 결과 없음 | 공통 DEBUG 거절 분류. 현재 코드의 `A003` 또는 `C005`. 실제 repository 반환 방식 확인 없이 빈 목록을 오류로 간주하지 않음 |
| 요청 입력 검증·바인딩 실패 | 공통 DEBUG 거절 분류. 공통 입력 예외 매핑 보완 필요 |
| 예상 밖 생성·조회 오류 | `api.request.errored` · ERROR, 전역 처리기에서 상세 예외 1회. 공통 적용 |

`api.request.errored`에는 원인 분석에 필요한 경우 확인된 `reservationId`·`scheduleId`(number)를 선택적으로 추가한다. 생성 전에는 존재하지 않는 `reservationId`를 요구하지 않는다. 조회 목록·사용자 식별자·요청 좌석 배열은 기본 계약에 추가하지 않는다.

승인 기록 도중 예약 확정에 실패하면 결제의 `payment.approval_record_failed`가 상세 오류를 소유한다. 예약 확정 오류를 별도 ERROR로 중복 기록하지 않는다.

## 현재 동작과 보류한 이벤트

- `createReservation`은 기존 PENDING 예약 취소와 새 좌석 선점을 같은 트랜잭션에서 처리한다. 생성·대체 취소 로그는 해당 트랜잭션 커밋 후 기록한다.
- 좌석 선점은 10분이며 승인 전 다시 연장된다. 생성·연장 당시 실제 만료 시각을 기록한다.
- 결제 거절 시 `onApprovalFailed`는 비어 있다. 예약 PENDING과 연장된 좌석 선점이 유지되므로 `reservation.cancelled`나 `seats_released`를 기록하지 않는다.
- `Reservation.expire()`는 있지만 현재 호출부가 없다. 시간이 지나 가용 좌석으로 보이는 것과 DB 예약 상태 EXPIRED 전이는 다르다. `reservation.expired`는 추가하지 않는다.
- 결제 취소 callback은 이번 API 범위 밖이며, 대체 취소와 혼동되는 기존 `reservation.cancelled` 로그는 제거했다. 취소 API를 다룰 때 별도 이벤트·필드를 정의한다.
- 승인·선점 이벤트는 결제 트랜잭션에 참여한다. 예약 메서드 반환 시점만으로 커밋 완료를 판정하지 않는다.

## 구현 검증

[ReservationServiceLogTest](../../src/test/java/com/ceos22/cgv_clone/domain/reservation/service/ReservationServiceLogTest.java)에서 실제 H2 트랜잭션 매니저의 커밋·롤백으로 상태 이벤트와 식별자 연결을 검증한다. 콜백 등록 전에 값을 캡처하며 커밋 후 엔티티를 다시 조회하지 않는다. 정상 API의 좌석은 입력 검증에 따라 1개 이상이며, 만료 시각을 확인할 수 없는 내부 호출에서는 holdExpiresAt을 추정하지 않고 생략한다. 적용 환경·통합 검증 범위는 [후속 검증 기록](implementation-verification-2026-10-12.md)을 따른다.

## 소스 근거

- [ScheduleController](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/controller/ScheduleController.java), [ScheduleService](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/service/ScheduleService.java): 일정·좌석 조회
- [ReservationController](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/controller/ReservationController.java): `reserveSeat`, `getMyReservations`
- [ReservationService](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/service/ReservationService.java): `createReservation`, `releasePendingReservations`, `preemptSeatsForPayment`, `confirmReservationAfterPayment`
- [Reservation](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/entity/Reservation.java), [ScheduleSeat](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/entity/ScheduleSeat.java): 상태와 만료·선점 정책
- [ReservationPaymentTargetHandler](../../src/main/java/com/ceos22/cgv_clone/domain/reservation/service/ReservationPaymentTargetHandler.java): 결제에서 예약으로의 callback, 거절 시 빈 처리
