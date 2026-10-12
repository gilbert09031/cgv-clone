# 결제 도메인 로그 이벤트

[카탈로그](README.md) · [공통 계약](common.md) · 계약 상태: 초안

## 대상 API와 연결 식별자

| API (`httpMethod` + `httpRoute`) | 처리 |
|---|---|
| `POST /api/payments` | 결제 준비 |
| `POST /api/payments/confirm` | 결제 승인 |
| `GET /api/payments/{paymentNumber}` | 결제 결과 조회 |

API 목록은 로그 발생이나 필드 자동 주입을 요구하지 않는다. `operation`·`domain`을 추가하지 않는다. 이번 계약은 예매 결제 범위이며 예약 연결에는 `reservationId`를 사용한다. 같은 값인 `targetId`나 고정 값 `targetType=RESERVATION`을 함께 요구하지 않는다. 매점 주문까지 실제 로그 계약을 확장할 때 대상 구분 필드를 검토한다.

- `paymentNumber`: 서버 결제 식별자. 실패 후 준비 시 동일 payment를 재사용할 수 있다.
- `gatewayOrderId`: 준비마다 발급되는 시도 식별자. 자동 선점 재시도는 같은 시도의 내부 재시도이며 별도 gatewayOrderId를 발급하지 않는다.
- `paymentKey`·`gatewayToken`·현재 Toss의 `transactionId`: 원문 비기록. 식별자 연결은 위 두 값으로 처리한다.

## 상태 변화·외부 호출 이벤트

최소 공통 필드 외에는 아래 이벤트 표에 선언한 필드만 요구한다. 예약과 결제의 연결은 준비 이벤트의 `reservationId`로 남기고, 이후 사건은 `paymentNumber`·`gatewayOrderId`로 연결한다. 모든 이벤트에 예약·대상 식별자를 반복하지 않는다. 이벤트명으로 확정되는 결제 상태·결과는 중복 필드로 요구하지 않는다.

| 이벤트 | 목표 레벨 | 발생 상황·기록 시점 | 필수 필드 | 기록 책임 | 현재 상태 |
|---|---|---|---|---|---|
| `payment.attempt_prepared` | INFO | 예약 결제 생성·재사용 및 새 시도 원장 저장 커밋 후 | `paymentNumber`, `gatewayOrderId`, `reservationId`, `amount`, `paymentMethod` | 준비 트랜잭션의 커밋 완료 지점 | 구현 완료: 커밋 후 구조화 필드·예약 연결 |
| `payment.approval_started` | INFO | 좌석 선점·APPROVING 커밋 후, 외부 승인 호출 직전 | `paymentNumber`, `gatewayOrderId` | `PaymentApprovalRecorder.preempt`의 커밋 완료 지점 | 구현 완료: 선점 커밋 후 결제·시도 연결 |
| `payment.gateway_completed` | 승인 INFO / 거절 WARN / 결과 불명 ERROR | 외부 승인 호출 종료, 로컬 기록 전. 외부 호출마다 1회 | `paymentNumber`, `gatewayOrderId`, `durationMs`, `outcome=approved/rejected/unknown`, 거절 코드가 있으면 `failureCode` | 외부 호출을 감싸는 승인 경계 | 구현 완료: 결과별 레벨·측정·필드 |
| `payment.approved` | INFO | 승인 원장·예약 확정 트랜잭션 커밋 후 | `paymentNumber`, `gatewayOrderId`, `amount` | `PaymentApprovalRecorder.recordSuccess`의 커밋 완료 지점 | 구현 완료: 커밋 후 구조화 필드, 결제 키 비기록 |
| `payment.approval_rejected` | WARN | 외부 거절을 DB 원장에 기록하여 FAILED 커밋 후 | `paymentNumber`, `gatewayOrderId`, 외부 코드가 있으면 `failureCode` | `PaymentApprovalRecorder.recordFailure`의 커밋 완료 지점 | 구현 완료: 커밋 후 구조화 필드 |

`payment.gateway_completed.durationMs`는 외부 승인 호출만 측정한다. 단위는 밀리초이며 선점·DB 기록 시간은 포함하지 않는다. 요청 전체 지연은 메트릭·Tracing으로 관측한다. 외부 승인 이벤트는 로컬 APPROVED 저장을 보장하지 않는다.

## 오류·경합 이벤트

| 이벤트 | 목표 레벨 | 발생 상황 | 필수 필드 | 기록 책임 | 현재 상태 |
|---|---|---|---|---|---|
| `payment.preempt_conflicted` | WARN | 선점 트랜잭션의 낙관적 잠금 경합, 실패한 시도마다 1회 | `gatewayOrderId`, `attempt`, `maxAttempts`, `retrying` | `PaymentService.preempt` 재시도 경계 | 구현 완료: 최대 3회, 숫자 attempt·maxAttempts 및 boolean retrying |
| `payment.approval_unknown` | ERROR | 외부 승인 결과를 확정하지 못함 | `paymentNumber`, `gatewayOrderId`, 상세 예외 | 외부 승인 결과를 판정하는 경계 | 구현 완료: 외부 호출 요약과 상세 예외 분리 |
| `payment.approval_record_failed` | ERROR | 외부 approved 확인 후 로컬 승인·예약 확정 기록 실패 | `paymentNumber`, `gatewayOrderId`, 상세 예외 | `PaymentService.recordSuccess` | 구현 완료: 원본 상세 예외 1회, 결제 키를 별도 필드로 기록하지 않음 |
| `payment.failure_record_failed` | ERROR | 외부 rejected 확인 후 실패 원장 저장 실패 | `paymentNumber`, `gatewayOrderId`, 상세 예외, 외부 코드가 있으면 `failureCode` | 외부 거절 후 `recordFailure` 호출 경계 | 구현 완료: 실패 원장 저장 오류를 기록, 거절 완료 이벤트 없음 |

오류 이벤트의 `lastKnownPaymentStatus`(string)는 원인 분석에 필요한 경우에만 선택적으로 추가한다. 오류 이전에 커밋을 확인한 상태이며 오류 후 DB 상태를 재조회했다는 뜻이 아니다. 허용 값은 현재 PaymentStatus enum 이름을 따른다. 커밋 결과 자체가 불명확한 장애에서 DB 상태를 확정해 기록하지 않는다.

`payment.gateway_completed(outcome=unknown)`는 외부 호출 요약, `payment.approval_unknown`은 해당 결제의 판단 결과다. 같은 사건의 스택 트레이스는 후자에서 한 번만 남기고 알림·건수는 후자를 기준으로 삼는다. 외부 거절 건수는 gateway_completed, 로컬 FAILED 전이 건수는 approval_rejected로 구분한다.

## 이벤트 필드 정의

아래 표는 이벤트별 필드의 이름·타입을 정의한다. 필수 여부는 각 이벤트 표를 따르며 일괄 출력하지 않는다. 선택 필드는 값이 확인되고 진단에 필요한 경우에만 추가한다.

| 필드 | 타입 | 의미·단위·적용 |
|---|---|---|
| `paymentNumber` | string | 서버 결제 ID. 결제 상태·시도를 연결 |
| `gatewayOrderId` | string | 외부 주문·승인 시도 ID. 현재 문자열 로그의 `orderId`를 이 이름으로 통일 |
| `reservationId` | number | 준비 사건에서 결제를 예약에 연결 |
| `amount` | number | 준비·승인 사건의 정수 원화 금액 |
| `paymentMethod` | string | 준비 사건의 결제 방식. 검증된 PaymentMethod enum 이름 |
| `durationMs` | number | gateway_completed에서 외부 승인 호출 시간, 밀리초 |
| `outcome` | string | gateway_completed에서만 결과 구분. `approved`, `rejected`, `unknown` |
| `failureCode` | string | 외부 거절 코드. gateway_completed 거절·approval_rejected·failure_record_failed에서 사용. 내부 errorCode와 구분 |
| `attempt`, `maxAttempts` | number | preempt_conflicted에서 1부터 시작하는 시도 번호·최대 횟수. 현재 최대 3회 |
| `retrying` | boolean | preempt_conflicted에서 다음 재시도 여부 |

외부 거절 코드가 제공되지 않으면 `failureCode`를 생략한다. 코드 누락을 자유 문장·원문 오류 메시지로 대체하지 않는다. 거절 시 좌석 선점이 유지되는 현재 동작은 [예약 문서](reservation.md)에 정의하며 `seatDisposition` 고정 값을 모든 거절 로그에 붙이지 않는다.

## 요청 거절·조회 이벤트

| 상황 | 처리·오류 코드·주의점 |
|---|---|
| 준비 시 결제 진행·완료 대상 | 공통 DEBUG 거절 분류. `P002` PAYMENT_ALREADY_EXISTS |
| 결제·소유권·금액·상태 검증 거절 | 공통 DEBUG 거절 분류. `P001`, `A002`, `P005`, `P007`, `P008` |
| 마지막 선점 경합 재시도 소진 | 최종 경계의 공통 DEBUG 거절 분류. `P003`. 별도 도메인 거절 이벤트는 추가하지 않으며 기존 `preempt_conflicted(retrying=false)`와 중복 실패 집계 금지 |
| 예약 없음·만료·상태·소유권 거절 | 공통 DEBUG 거절 분류. [예약 문서](reservation.md)의 응답 코드 적용 |
| 입력 검증 실패 | 공통 DEBUG 거절 분류. 현재 입력 예외 매핑 보완 필요 |
| 상세 도메인 오류 이벤트가 없는 예상 밖 준비·조회·선점 실패 | `api.request.errored` · ERROR, 전역 처리기에서 상세 예외 1회. 공통 적용 |
| 상태 조회 응답 완료 | 로그 없음. 상태·지연은 메트릭으로 관측. 별도 결제 성공 이벤트를 만들지 않음 |

`api.request.errored`의 결제 연결에는 확인된 `paymentNumber`·`gatewayOrderId`(string)를 원인 분석에 필요한 경우에만 선택적으로 추가한다. 준비 중 아직 발급되지 않은 식별자를 필수로 요구하지 않는다.

거절 요청은 결제 FAILED 전이를 의미하지 않는다. 이미 APPROVED 또는 APPROVING인 confirm은 현재 코드에서 P005로 거절되며, 성공 재응답이나 신규 승인 이벤트가 아니다.

## 흐름 예시와 집계 기준

```text
정상: attempt_prepared → approval_started → gateway_completed(approved)
      → approved + reservation.confirmed
거절: approval_started → gateway_completed(rejected) → approval_rejected
불명: approval_started → gateway_completed(unknown) → approval_unknown
승인 기록 실패: gateway_completed(approved) → approval_record_failed
거절 기록 실패: gateway_completed(rejected) → failure_record_failed
```

예시는 논리적 순서다. 동일 커밋 후 payment.approved와 reservation.confirmed의 출력 순서에 의존하지 않는다. 중간 실패 시 성공 이벤트를 채워 넣지 않는다.

- 승인 성공 건수는 커밋 후 `payment.approved`로 계산한다. `reservation.confirmed`까지 더하지 않는다.
- P009는 확정 거절·미승인을 뜻하지 않는다. 상태 조회는 현재 로컬 DB 조회이며 외부 결과를 자동 보정하는 기능이 아니다.
- 장시간 APPROVING 여부는 DB 상태 기반 지표로 확인한다. 시작 로그 뒤 완료 로그가 없다는 사실만으로 승인 실패를 확정하지 않는다.
- 현재 PaymentMetrics는 성공·거절 카운터만 기록한다. 이 계약의 모든 오류·unknown·경합을 이미 지표로 관측한다고 간주하지 않는다.
- 결과 보정 작업은 소스에 구현이 확인되지 않는다. `payment.reconciled`는 이번 초안에 추가하지 않는다.

## 구현 검증

결제 서비스의 외부 승인·거절·불명·기록 실패·경합 경로와 준비·원장 기록의 커밋 경계를 회귀 테스트로 검증한다. 실패 원장 저장 자체가 실패한 경우 상세 오류를 기록한 뒤 INTERNAL_SERVER_ERROR로 변환하여 기존 HTTP 500 응답을 유지하며 전역 처리기에서 상세 로그를 중복하지 않는다. 실제 Toss 호출·MySQL 동시성·수집 환경의 검증 여부는 [후속 검증 기록](implementation-verification-2026-10-12.md)을 따른다.

## 소스 근거

- [PaymentController](../../src/main/java/com/ceos22/cgv_clone/domain/payment/controller/PaymentController.java): 준비·승인·조회 API
- [PaymentService](../../src/main/java/com/ceos22/cgv_clone/domain/payment/service/PaymentService.java): 기존 이벤트, 선점 재시도, 외부 승인·기록 경계
- [PaymentApprovalRecorder](../../src/main/java/com/ceos22/cgv_clone/domain/payment/service/PaymentApprovalRecorder.java): 선점·승인·거절의 별도 트랜잭션
- [Payment](../../src/main/java/com/ceos22/cgv_clone/domain/payment/entity/Payment.java): 시도 원장, 결제 상태·식별자
- [TossPaymentGateway](../../src/main/java/com/ceos22/cgv_clone/infrastructure/payment/toss/TossPaymentGateway.java), [TossPaymentClient](../../src/main/java/com/ceos22/cgv_clone/infrastructure/payment/toss/client/TossPaymentClient.java): transactionId 매핑과 paymentKey DEBUG 로그 제거
- [PaymentMetrics](../../src/main/java/com/ceos22/cgv_clone/common/metrics/PaymentMetrics.java): 현재 성공·실패 카운터
