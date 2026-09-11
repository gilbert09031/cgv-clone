# Logging Convention (Draft)

> 구조화 로깅(JSON) 출력, Correlation ID 발급/전파 등 **인프라 설정은 이미 되어 있다고 가정**하고, 그 위에서 개발자가 `log.info(...)`를 호출할 때 지켜야 할 규칙만 다룹니다.
---

## 1. 로그 레벨 기준

| 레벨 | 언제 쓰는가 | 예시 (이 프로젝트 기준) |
| :--- | :--- | :--- |
| `ERROR` | 즉시 확인/조치가 필요한 실패. 요청이 실패로 끝나거나 데이터 정합성이 깨질 위험이 있는 경우 | 재고 차감 실패, 결제 승인 실패, 예상치 못한 예외 |
| `WARN` | 실패는 아니지만 비정상적이거나 재시도/보정이 일어난 상황 | 락 이미 해제됨, Payment 중복으로 재시도, 인증 실패(4xx) |
| `INFO` | 비즈니스적으로 의미 있는 상태 전이 1건당 1줄 | 회원가입 완료, 결제 승인 완료, 주문 취소 완료, 락 획득/해제 |
| `DEBUG` | 개발/트러블슈팅 시에만 보고 싶은 세부 흐름 | 트랜잭션 시작/커밋, 락 획득 시도, 쿼리 파라미터 |
| `TRACE` | 사실상 사용하지 않음 (필요하면 DEBUG로 충분한지 먼저 검토) | - |

**원칙**
- "일단 info로 찍고 본다"를 지양합니다. INFO 로그는 운영자가 대시보드/알림에서 계속 보게 될 로그라는 것을 전제로 씁니다.
- 하나의 요청 처리 흐름에서 같은 사건을 여러 레벨로 중복해서 찍지 않습니다 (예: 서비스 계층에서 ERROR로 찍고, 전역 예외 핸들러에서 또 ERROR로 찍는 이중 로깅 지양 — [GlobalExceptionHandler.java](../src/main/java/com/ceos22/cgv_clone/common/error/GlobalExceptionHandler.java) 처럼 최종적으로 응답을 만드는 지점에서 한 번만).
- 반복문 안에서 INFO/WARN을 찍지 않습니다. 필요하면 DEBUG + `log.isDebugEnabled()`로 감쌉니다.

---

## 2. 메시지 포맷

### 2.1 기본 형식

```
event=resource.action, key1={}, key2={}, key3={}
```

```java
log.info("event=payment.confirmed, paymentId={}, orderId={}, amount={}", paymentId, orderId, amount);
log.warn("event=lock.already_released, lockKey={}, method={}", lockKey, methodName);
log.error("event=order.stock_decrease_failed, orderId={}, productId={}, quantity={}", orderId, productId, quantity, e);
```

- 이벤트명은 대괄호 태그가 아니라 **다른 필드와 동일하게 `event=resource.action` key=value 한 쌍**으로 남기고, 항상 메시지의 맨 앞에 둡니다. 대괄호(`[...]`)로 감싸는 표기는 쓰지 않습니다 — 필드 하나가 특별 취급되지 않고 다른 key=value와 동일하게 파싱/검색되도록 하기 위함입니다.
- `resource`는 도메인 명사(`payment`, `order`, `lock`, `auth` ...), `action`은 **과거형 동사**(`confirmed`, `failed`, `rejected`, `released` ...)로, 여러 단어면 `snake_case`로 이어 씁니다 (`stock_decrease_failed`). Stripe(`payment_intent.succeeded`, `charge.refunded`)나 ECS `event.action` 등 업계에서 일반적으로 쓰는 표기와 맞춘 것입니다. `PAYMENT_CONFIRMED`처럼 대문자 스네이크 케이스는 쓰지 않습니다.
- 검색/알림 규칙(예: "`order.stock_decrease_failed`가 5분간 10건 이상 발생하면 알림")이 이 문자열에 그대로 의존하므로, 같은 사건은 코드 어디서 찍든 항상 같은 이벤트명을 씁니다.
- 그 뒤에 붙는 한글 설명은 선택입니다. `event=payment.confirmed, 결제 승인 완료, paymentId={}, ...` 처럼 사람이 읽을 문구를 끼워 넣어도 되지만, `event=`는 항상 맨 앞, `key=value` 나열 순서는 일관된 구분자(`, `)로 유지합니다.
- 기존 코드에 섞여 있던 `key: {}` / `key={}` / `[key]` 등 표기 방식을 **`key={}`로 통일**합니다.

### 2.2 값은 항상 `{}` 플레이스홀더로 전달

메시지를 문자열로 직접 이어붙이지 않습니다.

```java
// Bad — 로그 레벨이 꺼져 있어도 문자열 연산이 발생하고, 필드 추출이 불가능
log.info("결제 승인 완료 - paymentId: " + paymentId);

// Good
log.info("event=payment.confirmed, paymentId={}", paymentId);
```

### 3.3 Correlation ID / trace 정보는 메시지에 다시 적지 않는다

Correlation ID, traceId 등은 MDC를 통해 로그 인프라가 자동으로 모든 라인에 붙여줍니다. 메시지 본문에 `requestId={}` 같은 식으로 중복 기재하지 않습니다. 메시지에는 **그 요청 하나만으로는 알 수 없는 도메인 식별자**(paymentId, orderId, memberId 등)만 남깁니다.

---

## 4. 예외(Exception) 로깅

### 4.1 예외 객체를 그대로 전달한다

```java
// Bad — 스택트레이스가 사라져서 원인 파악 불가
log.error("event=payment.cancel_failed, paymentId={}, error={}", paymentId, e.getMessage());

// Good — 마지막 인자로 Throwable을 전달하면 스택트레이스가 함께 남는다
log.error("event=payment.cancel_failed, paymentId={}", paymentId, e);
```

`e.getMessage()`만 문자열로 찍는 습관은 지양합니다. SLF4J는 마지막 인자가 `Throwable`이면 자동으로 스택트레이스를 로그에 포함시켜 줍니다.

### 4.2 어디서 로그를 남길지 한 곳으로 정한다

같은 예외를 여러 계층에서 각각 `log.error`로 남기면 로그가 중복되고 집계 시 건수가 부풀려집니다.
- 예외를 **처리(응답 변환/트랜잭션 롤백 등)하는 계층**에서 1번만 남깁니다.
- 상위로 다시 던지기만 하는 계층에서는 로그를 남기지 않거나, 남긴다면 `DEBUG`로 낮춰서 남깁니다.

### 4.3 예상 가능한 실패 vs 예상치 못한 실패 구분

- 예상 가능한 실패(중복 요청, 유효성 실패, 이미 처리된 상태 등)는 `WARN` + 짧은 메시지로 충분합니다. 스택트레이스가 꼭 필요하지 않다면 `e`를 넘기지 않아도 됩니다.
- 예상치 못한 실패(`RuntimeException`, 외부 API 5xx 등)는 `ERROR` + 항상 예외 객체를 함께 남깁니다.

---

## 5. 민감정보 마스킹

로그는 애플리케이션 코드보다 훨씬 오래, 훨씬 많은 사람이 접근할 수 있는 저장소로 흘러갑니다. 아래 항목은 **원문 그대로 로그에 남기지 않습니다.**

| 데이터 | 규칙 |
| :--- | :--- |
| 이메일 | 로컬 파트 일부 마스킹 (`ab***@hongik.ac.kr`) 후 로깅 |
| 비밀번호 | 어떤 형태로도 로깅 금지 |
| JWT / Access Token / Refresh Token | 전체 값 로깅 금지. 필요 시 앞 8자 등 식별 가능한 일부만 |
| Toss `paymentKey`, 카드 정보 등 결제 민감 데이터 | 카드번호·인증정보는 로깅 금지. `paymentKey`/`paymentId`처럼 시스템 식별자 성격이면 로깅 가능하나 카드번호·고객 개인정보는 별도 |
| 사용자 요청/응답 바디 전체를 통째로 로깅 | 지양. 필요한 필드만 추출해서 로깅 |

마스킹은 로그를 남기는 시점(호출부)에서 처리합니다. "나중에 로그 시스템에서 가릴 수 있겠지"라고 미루지 않습니다.

---

## 6. 비동기/동시성 코드에서 로깅

- `@Async`, `CompletableFuture`, 재시도 로직 등 **다른 스레드에서 실행되는 코드**는 MDC(Correlation ID)가 자동으로 이어지지 않습니다. 스레드를 넘기기 전에 MDC 값을 명시적으로 복사하거나, 없는 채로 남는다는 점을 감안해 메시지 안에 도메인 식별자를 반드시 포함합니다.
- 락/재시도처럼 반복 가능한 동작은 "몇 번째 시도인지"를 항상 필드로 남깁니다.
  ```java
  log.warn("event=order.create_retry, attempt={}/{}, reason=duplicate_payment_id", attempt, maxAttempt);
  ```

---

## 7. 지양할 표현/패턴

- `log.info("여기 도달함")`, `log.debug("test")` 처럼 이벤트를 특정할 수 없는 로그
- `System.out.println` / `e.printStackTrace()` 사용 (SLF4J `log`만 사용)
- 같은 의미의 이벤트를 파일마다 다른 이름으로 로깅 (`[결제 승인 완료]`, `PAYMENT_CONFIRM_SUCCESS`, `paymentConfirmed` 등 혼용) — 3.1절의 `event=resource.action` 명명 규칙으로 통일
- 이벤트명을 대괄호(`[payment.confirmed]`)나 다른 필드와 다른 표기로 감싸기 — `event=` key=value로 통일
- 예외를 잡고 로그만 남긴 뒤 그대로 삼키기(swallow) — 로그를 남긴다면 왜 무시해도 되는지 주석으로 남기거나, 상위로 전파
- 메시지 문자열에 값을 직접 이어붙이기 (`"paymentId: " + id`)
- 반복문/hot path 내부에서 레벨 가드 없이 INFO 이상 로깅

---

## 8. 도메인별 예시

**결제 (`payment`)**
```java
log.info("event=payment.confirmed, paymentId={}, orderId={}", paymentId, orderId);
log.warn("event=payment.cancel_rejected, paymentId={}, currentStatus={}", paymentId, status);
log.error("event=payment.confirm_failed, paymentId={}, orderId={}", paymentId, orderId, e);
```

**주문 (`order`)**
```java
log.info("event=order.cancelled, orderId={}", orderId);
log.error("event=order.stock_decrease_failed, orderId={}, productId={}, quantity={}", orderId, productId, quantity, e);
```

**인증 (`auth`)**
```java
log.info("event=auth.signup_succeeded, email={}", maskEmail(request.email()));
log.warn("event=auth.login_failed, email={}, reason=invalid_credential", maskEmail(request.email()));
```

---

## 9. 요약 치트시트

```
포맷:     event=resource.action, key1={}, key2={}, ...  (대괄호 태그 쓰지 않음)
이벤트명: 소문자 dot-notation, action은 과거형 동사(+snake_case), 프로젝트 전체에서 동일 사건은 동일 이름
레벨:     ERROR(즉시 조치) / WARN(비정상·재시도) / INFO(상태 전이 1건=1줄) / DEBUG(개발용 세부 흐름)
예외:     항상 Throwable을 마지막 인자로 전달, e.getMessage()만 찍지 않기
중복 로깅: 같은 예외는 처리하는 계층에서 한 번만
민감정보: 이메일/토큰/카드정보는 마스킹 후 로깅, 원문 로깅 금지
비동기:   MDC 자동 전파 안 됨 → 메시지에 도메인 식별자 필수 포함
```
