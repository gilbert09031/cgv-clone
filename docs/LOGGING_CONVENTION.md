# Logging Convention (Draft)

도메인별 이벤트의 이름·레벨·발생 상황·필드·구현 상태는 [로그 이벤트 카탈로그](log-events/README.md)에서 관리합니다. 카탈로그는 예매 API 범위부터 정의한 이벤트 계약과 구현·검증 상태를 관리합니다.

예매 API의 현재 구현에서는 카탈로그에 따라 정상 종료 로그를 기록하지 않습니다. 예상 가능한 HTTP 거절은 최종 거절 경계에서 구조화 `DEBUG` 로그로만 남기며, 기본 운영 레벨에서는 출력되지 않는 진단 정보로 취급합니다. 공통 필드는 `event`와 요청의 `requestId`로 제한하고, 상태 변화는 커밋 후 기록합니다. HTTP·결제 상세 오류는 원본 예외를 `setCause`에 전달해 타입·메시지·원인 체인·스택 프레임을 남깁니다. 예외 메시지의 민감정보는 자동으로 제거되지 않습니다.

> 구조화 로깅(JSON) 출력, Correlation ID 발급/전파 등 **인프라 설정은 이미 되어 있다고 가정**하고, 애플리케이션에서 Loki가 필드로 검색할 수 있는 로그를 남기는 규칙을 다룹니다.
---

## 1. 로그 레벨 기준

| 레벨 | 언제 쓰는가 | 예시 (이 프로젝트 기준) |
| :--- | :--- | :--- |
| `ERROR` | 즉시 확인/조치가 필요한 실패. 요청이 실패로 끝나거나 데이터 정합성이 깨질 위험이 있는 경우 | 재고 차감 실패, 결제 승인 실패, 예상치 못한 예외 |
| `WARN` | 실패는 아니지만 비정상적이거나 재시도/보정이 일어난 상황 | 락 이미 해제됨, Payment 중복으로 재시도, 외부 결제 거절 |
| `INFO` | 비즈니스적으로 의미 있는 상태 전이 1건당 1줄 | 회원가입 완료, 결제 승인 완료, 주문 취소 완료, 락 획득/해제 |
| `DEBUG` | 개발/트러블슈팅 시에만 보고 싶은 세부 흐름 | 트랜잭션 시작/커밋, 락 획득 시도, 예상 가능한 요청 거절 |
| `TRACE` | 사실상 사용하지 않음 (필요하면 DEBUG로 충분한지 먼저 검토) | - |

**원칙**
- "일단 info로 찍고 본다"를 지양합니다. INFO 로그는 운영자가 대시보드/알림에서 계속 보게 될 로그라는 것을 전제로 씁니다.
- 하나의 요청 처리 흐름에서 같은 사건을 여러 레벨로 중복해서 찍지 않습니다 (예: 서비스 계층에서 ERROR로 찍고, 전역 예외 핸들러에서 또 ERROR로 찍는 이중 로깅 지양 — [GlobalExceptionHandler.java](../src/main/java/com/ceos22/cgv_clone/common/error/GlobalExceptionHandler.java) 처럼 최종적으로 응답을 만드는 지점에서 한 번만).
- 반복문 안에서 INFO/WARN을 찍지 않습니다. 필요하면 DEBUG + `log.isDebugEnabled()`로 감쌉니다.

---

## 2. 구조화 필드 포맷

### 2.1 이벤트와 필드를 JSON 필드로 기록

`event=... key=value` 문자열을 메시지로 기록하면 파일 전체는 JSON이어도 해당 내용은 `message` 문자열에 머뭅니다. Loki의 JSON 파이프라인에서 `event`나 식별자를 바로 필터링할 수 있도록 SLF4J fluent API의 `addKeyValue`로 각각 기록합니다.

```java
log.atInfo()
        .addKeyValue("event", "payment.confirmed")
        .addKeyValue("paymentNumber", paymentNumber)
        .addKeyValue("gatewayOrderId", gatewayOrderId)
        .addKeyValue("amount", amount)
        .log();
```

- 이벤트명은 `event` JSON 필드의 값으로 기록합니다. 대괄호나 `event=...` 문장을 메시지에 넣는 방식은 사용하지 않습니다.
- `resource`는 도메인 명사(`payment`, `order`, `lock`, `auth` ...), `action`은 **과거형 동사**(`confirmed`, `failed`, `rejected`, `released` ...)로, 여러 단어면 `snake_case`로 이어 씁니다 (`stock_decrease_failed`). Stripe(`payment_intent.succeeded`, `charge.refunded`)나 ECS `event.action` 등 업계에서 일반적으로 쓰는 표기와 맞춘 것입니다. `PAYMENT_CONFIRMED`처럼 대문자 스네이크 케이스는 쓰지 않습니다.
- 검색·알림 규칙은 `message` 문자열이 아니라 `event` JSON 필드와 필터링할 필드에 의존합니다. 같은 사건은 코드 어디서 기록하든 같은 이벤트명을 씁니다.
- 검색 대상 값은 각각 별도 key-value 필드로 기록합니다. 여러 값을 문자열로 합치거나 `message`에 `key=value`로 붙이지 않습니다.
- 값이 없거나 확인되지 않은 필드는 생략합니다. 필드 이름·타입은 [이벤트 카탈로그](log-events/README.md) 정의를 따릅니다.

### 2.2 설명 메시지와 값 전달

사람이 읽을 추가 설명이 필요하면 `log("...")` 메시지로 기록할 수 있습니다. 검색에 필요한 이벤트명·식별자·상태는 여전히 `addKeyValue`로 남깁니다. 문자열 연결이나 민감한 전체 값을 메시지에 넣지 않습니다.

```java
// Bad — 파일은 JSON이어도 event와 paymentId는 message 안의 문자열이다
log.info("event=payment.confirmed, paymentId={}, amount={}", paymentId, amount);

// Good — 설명은 선택, 검색 필드는 별도 JSON 필드
log.atInfo()
        .addKeyValue("event", "payment.confirmed")
        .addKeyValue("paymentId", paymentId)
        .addKeyValue("amount", amount)
        .log("결제 승인 완료");
```

### 2.3 Correlation ID / trace 정보는 중복 기록하지 않는다

`requestId` 같은 Correlation ID는 MDC에서 JSON 필드로 출력되도록 인프라에서 관리합니다. 이벤트마다 중복해서 `addKeyValue`로 넣지 않습니다. 메시지·이벤트 필드에는 **그 요청만으로 알 수 없는 도메인 식별자**(`paymentNumber`, `gatewayOrderId`, `reservationId` 등)를 기록합니다. 현재 `requestId`는 분산 `traceId`와 같지 않습니다.

---

## 4. 예외(Exception) 로깅

### 4.1 예외 객체를 그대로 전달한다

```java
// Bad — 예외와 event 필드가 일반 문자열 인자로 처리된다
log.error("event=payment.cancel_failed, paymentId={}", paymentId, e);

// Good — event와 paymentId는 JSON 필드, 예외는 cause로 전달
log.atError()
        .addKeyValue("event", "payment.cancel_failed")
        .addKeyValue("paymentId", paymentId)
        .setCause(e)
        .log();
```

원인 예외는 `setCause(e)`로 전달해 스택 트레이스를 보존합니다. 예상 가능한 거절과 예상 밖 장애를 구분해 예외와 메시지의 민감정보가 기록되지 않는지 확인합니다.

### 4.2 어디서 로그를 남길지 한 곳으로 정한다

같은 예외를 여러 계층에서 각각 `log.error`로 남기면 로그가 중복되고 집계 시 건수가 부풀려집니다.
- 예외를 **처리(응답 변환/트랜잭션 롤백 등)하는 계층**에서 1번만 남깁니다.
- 상위로 다시 던지기만 하는 계층에서는 로그를 남기지 않거나, 남긴다면 `DEBUG`로 낮춰서 남깁니다.

### 4.3 예상 가능한 실패 vs 예상치 못한 실패 구분

- 예상 가능한 HTTP 거절(중복 요청, 유효성 실패, 이미 처리된 상태, 인증·권한 거절 등)은 최종 거절 경계에서 `DEBUG`로만 남깁니다. `event`, `errorCode`, `statusCode`처럼 분류에 필요한 필드만 기록하고 스택 트레이스·원문 메시지·인증정보는 남기지 않습니다.
- 외부 시스템 거절이나 재시도 경합처럼 운영자가 실패 흐름을 집계해야 하는 도메인 사건은 도메인 카탈로그에 따라 `WARN`을 쓸 수 있습니다.
- 예상치 못한 실패(`RuntimeException`, 외부 API 5xx 등)는 `ERROR` + 항상 예외 객체를 함께 남깁니다.

---

## 5. 민감정보 마스킹

로그는 애플리케이션 코드보다 훨씬 오래, 훨씬 많은 사람이 접근할 수 있는 저장소로 흘러갑니다. 아래 항목은 **원문 그대로 로그에 남기지 않습니다.**

| 데이터 | 규칙 |
| :--- | :--- |
| 이메일 | 로컬 파트 일부 마스킹 (`ab***@hongik.ac.kr`) 후 로깅 |
| 비밀번호 | 어떤 형태로도 로깅 금지 |
| JWT / Access Token / Refresh Token | 전체 값 로깅 금지. 필요 시 앞 8자 등 식별 가능한 일부만 |
| Toss `paymentKey`, `gatewayToken`, 카드 정보 등 결제 민감 데이터 | 원문 기록 금지. 일반 로그 상관관계에는 `paymentNumber`, `gatewayOrderId` 사용 |
| 사용자 요청/응답 바디 전체를 통째로 로깅 | 지양. 필요한 필드만 추출해서 로깅 |

마스킹은 로그를 남기는 시점(호출부)에서 처리합니다. "나중에 로그 시스템에서 가릴 수 있겠지"라고 미루지 않습니다.

---

## 6. 비동기/동시성 코드에서 로깅

- `@Async`, `CompletableFuture`, 재시도 로직 등 **다른 스레드에서 실행되는 코드**는 MDC(Correlation ID)가 자동으로 이어지지 않습니다. 스레드 전환 시 컨텍스트 전파를 확인하고, 도메인 식별자는 별도의 key-value 필드로 기록합니다.
- 락/재시도처럼 반복 가능한 동작은 "몇 번째 시도인지"를 항상 필드로 남깁니다.
  ```java
  log.atWarn()
      .addKeyValue("event", "order.create_retry")
      .addKeyValue("attempt", attempt)
      .addKeyValue("maxAttempt", maxAttempt)
      .addKeyValue("reason", "duplicate_payment_id")
      .log();
  ```

---

## 7. 지양할 표현/패턴

- `log.info("여기 도달함")`, `log.debug("test")` 처럼 이벤트를 특정할 수 없는 로그
- `System.out.println` / `e.printStackTrace()` 사용 (SLF4J `log`만 사용)
- 같은 의미의 이벤트를 파일마다 다른 이름으로 로깅 (`[결제 승인 완료]`, `PAYMENT_CONFIRM_SUCCESS`, `paymentConfirmed` 등 혼용) — 2.1절의 `event` 필드 규칙으로 통일
- `event=...`나 `paymentId=...`를 메시지 문자열에 넣고 Loki가 JSON 필드로 검색할 수 있다고 가정
- 예외를 잡고 로그만 남긴 뒤 그대로 삼키기(swallow) — 로그를 남긴다면 왜 무시해도 되는지 주석으로 남기거나, 상위로 전파
- 메시지 문자열에 값을 직접 이어붙이기 (`"paymentId: " + id`)
- 반복문/hot path 내부에서 레벨 가드 없이 INFO 이상 로깅

---

## 8. 도메인별 예시

**결제 (`payment`)**
```java
log.atInfo()
        .addKeyValue("event", "payment.confirmed")
        .addKeyValue("paymentNumber", paymentNumber)
        .addKeyValue("gatewayOrderId", gatewayOrderId)
        .addKeyValue("amount", amount)
        .log();

log.atWarn()
        .addKeyValue("event", "payment.cancel_rejected")
        .addKeyValue("paymentNumber", paymentNumber)
        .addKeyValue("currentStatus", status)
        .log();

log.atError()
        .addKeyValue("event", "payment.confirm_failed")
        .addKeyValue("paymentNumber", paymentNumber)
        .addKeyValue("gatewayOrderId", gatewayOrderId)
        .setCause(e)
        .log();
```

**주문 (`order`)**
```java
log.atInfo()
        .addKeyValue("event", "order.cancelled")
        .addKeyValue("orderId", orderId)
        .log();

log.atError()
        .addKeyValue("event", "order.stock_decrease_failed")
        .addKeyValue("orderId", orderId)
        .addKeyValue("productId", productId)
        .addKeyValue("quantity", quantity)
        .setCause(e)
        .log();
```

**인증 (`auth`)**
```java
log.atInfo()
        .addKeyValue("event", "auth.signup_succeeded")
        .addKeyValue("email", maskEmail(request.email()))
        .log();

log.atWarn()
        .addKeyValue("event", "auth.login_failed")
        .addKeyValue("email", maskEmail(request.email()))
        .addKeyValue("reason", "invalid_credential")
        .log();
```

---

## 9. 요약 치트시트

```
포맷:     SLF4J addKeyValue → JSON 최상위 필드 → Loki에서 | json 으로 조회
이벤트명: event 필드에 소문자 dot-notation, action은 과거형 동사(+snake_case)
레벨:     ERROR(즉시 조치) / WARN(비정상·재시도) / INFO(상태 전이 1건=1줄) / DEBUG(개발용 세부 흐름)
예외:     setCause(Throwable)로 전달. 예외 메시지 문자열에만 의존하지 않기
중복 로깅: 같은 예외는 처리하는 계층에서 한 번만
민감정보: 이메일/토큰/카드정보는 마스킹 후 로깅, 원문 로깅 금지
비동기:   MDC 자동 전파 안 됨 → 컨텍스트 전파 확인, 도메인 식별자는 key-value 필드로 기록
```
