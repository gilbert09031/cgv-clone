# Docstring & Comment Convention (Draft)

> 본 문서는 `spring-cgv-22nd` 프로젝트에서 **앞으로 작성·수정하는 파일**의 Javadoc과 코드 주석을 어떻게 쓸지 정의합니다.
> 이미 있는 주석을 일괄 수정하지 않습니다. 파일을 고치는 김에 그 파일의 주석을 이 규칙으로 맞추는 것은 권장합니다.
> 팀/개인 상황에 맞게 자유롭게 수정해서 사용하세요.

---

## 1. 원칙

1. **코드가 이미 말하는 것은 쓰지 않는다.** 메서드명, 시그니처, 필드 타입으로 알 수 있는 내용을 문장으로 반복하지 않습니다. 반복된 주석은 코드가 바뀔 때 함께 썩습니다.
2. **계약과 이유를 쓴다.** 주석의 자리는 "코드만 봐서는 알 수 없는 것"입니다. 호출자가 지켜야 할 조건, 예외가 뜻하는 바, 그렇게 설계한 이유, 외부 시스템의 사실이 여기에 해당합니다.
3. **짧게 쓴다.** 타입 설명은 한 줄. 두 문장이 넘어가면 설계 문서(`docs/`)로 옮기고 주석에는 결론만 남깁니다.
4. **주석으로 나쁜 코드를 변명하지 않는다.** 이름이 애매해서 주석이 필요하면 이름을 고칩니다. 설명이 어려우면 코드가 복잡하다는 신호입니다.

---

## 2. 언어

- 주석은 **영어**로 씁니다. 짧은 구(句)로 쓰기에 유리하고, 식별자·외부 API 용어와 자연스럽게 섞입니다.
- 한 파일 안에서 언어를 섞지 않습니다. 기존 한국어 주석이 있는 파일을 수정할 때, 그 파일의 주석을 함께 영어로 바꾸는 것은 권장이지 의무는 아닙니다. 다만 새로 추가하는 주석은 영어로 씁니다.
- 한국어 고유 개념(예: 특별관, 매점)은 코드에서 쓰는 영어 식별자(`SPECIAL_SCREEN`, `Store`)를 그대로 씁니다. 설계 논의와 문서(`docs/`)는 한국어를 유지합니다.

---

## 3. Javadoc (`/** ... */`)

### 3.1 어디에 쓰는가

| 대상 | 규칙 |
| :--- | :--- |
| public 클래스·인터페이스·레코드·enum | **한 줄 요약 필수.** 무엇인지, 무엇에 쓰이는지 |
| 포트 인터페이스의 메서드 (`PaymentGateway`, `PaymentTargetHandler` 등) | 계약이 있으면 작성. 인자 의미, 예외 의미, 호출 시점 |
| 그 외 public 메서드 | 이름과 시그니처로 충분하면 생략. 비자명한 조건이 있을 때만 |
| `@Override` 메서드 | 생략. 문서는 인터페이스에 둡니다. `{@inheritDoc}`도 쓰지 않습니다 |
| getter, 단순 팩토리, DTO `from()` | 생략 |
| private 멤버 | Javadoc 대신 필요할 때 `//` 한 줄 |

### 3.2 요약 문장

- **명사구 또는 동사구**로 씁니다. 완전한 문장이 아닙니다. "This class represents ...", "Returns the ..." 같은 서술을 피합니다.
- 마침표를 찍지 않습니다. 두 구를 이어야 하면 마침표로 나눕니다.
- 한 줄에 들어가면 한 줄 형태 `/** ... */`를 씁니다.

```java
// Bad
/**
 * This record represents the response of the payment preparation step.
 * The client can use these values to open the payment widget.
 */

// Good
/** Payment preparation result. Values the client needs to open the payment widget */
```

### 3.3 블록 태그

- 순서는 `@param`, `@return`, `@throws`.
- `@param`은 **이름만으로 의미가 안 드러날 때만** 씁니다. 값의 출처나 형식이 비자명한 경우입니다. IDE가 만들어 준 빈 `@param` 골격을 남기지 않습니다.
- `@return`은 요약 문장에 이미 반환값이 드러나면 생략합니다.
- `@throws`는 **호출자가 구분해서 처리해야 하는 예외**에 씁니다. 체크 예외 여부와 무관합니다. "언제 던지고, 언제는 안 던지는지"가 계약이면 반드시 씁니다.
- 태그 설명은 소문자·명사구로 시작하고 마침표를 찍지 않습니다.
- 레코드의 컴포넌트 설명이 필요하면 레코드 Javadoc에 `@param`으로 씁니다.

```java
// Good: argument meaning and exception contract are not obvious from the signature
/**
 * Request approval from the gateway
 * @param gatewayToken opaque value the client received from the payment widget; never parsed or stored
 * @throws PaymentGatewayException when the gateway explicitly rejects; timeouts and unknown outcomes propagate as-is
 */
GatewayApproval approve(String paymentNumber, String gatewayToken, int amount);
```

### 3.4 인라인 태그

- 코드 요소와 리터럴은 `{@code ...}`로 감쌉니다: `{@code null}`, `{@code APPROVING}`, `{@code Payment.method}`.
- 다른 타입을 가리킬 때 이동이 유용하면 `{@link Type}`을 씁니다. 같은 문장에 여러 번 나오면 첫 번째만.
- HTML 태그(`<p>`, `<code>`)는 쓰지 않습니다.

### 3.5 쓰지 않는 것

- `@author`, `@since`, 작성일, 변경 이력 — Git이 기록합니다.
- 설계 논의 서사("compared option A and B ...") — `docs/`에 쓰고 주석에는 결론 한 줄과 필요하면 문서 경로만.
- 주석 처리된 코드.

---

## 4. 구현 주석 (`//`)

### 4.1 쓰는 경우

| 상황 | 예 |
| :--- | :--- |
| 비자명한 결정의 **이유** | `// PENDING does not block: failures inside the widget never reach the server, so they stay PENDING` |
| 호출자·후속 코드가 지켜야 할 **조건** | `// Handlers join the caller's transaction; this method must be a read-write @Transactional` |
| **외부 시스템의 사실**과 확인 근거 | `// Toss rejects reuse of a failed orderId with DUPLICATED_ORDER_ID (verified 2026-09-11, developer community)` |
| 관용적이지 않은 코드의 사정 | `// -i '' is required for BSD sed compatibility` |
| 우회·버그 수정 | 원인과 이슈 링크를 함께 |
| 미완성 표시 | `// TODO: what and why` — 콜론 뒤 한 칸 |

### 4.2 위치와 형태

- 설명하는 멤버·문장 **바로 위**에 둡니다. 줄 끝 주석은 짧은 상수 설명에만 씁니다.
- 한두 줄로 끝냅니다. 길어지면 `docs/`로 보냅니다.
- 상수 하나로 규칙이 표현되는 경우, 이유 주석은 상수 위에 한 번만 씁니다. 사용처마다 반복하지 않습니다.
- 서비스의 공개/비공개 구역 구분선(`//====`)은 기존 관례대로 허용하되, 새 파일에서 필수는 아닙니다.

### 4.3 쓰지 않는 것

```java
// Bad: repeats the code
count++; // increment count
// Bad: repeats the method name
// Prepare a payment
public PaymentPrepareResponse prepare(...)
// Bad: creates confusion instead of removing it
// fix later (what? why?)
```

---

## 5. 이 프로젝트의 예시

### 5.1 타입 요약 한 줄

```java
/** Operations the payment domain requires from an external payment gateway. Implementations live in infrastructure */
public interface PaymentGateway { ... }

/** Toss rejected the request with 4xx. Translated to PaymentGatewayException before reaching the domain */
public class TossApiException extends RuntimeException { ... }
```

### 5.2 상수 위의 이유 주석

```java
// A new payment is not prepared while one of these exists for the same target.
// PENDING and FAILED do not block: failures inside the widget never reach the server and stay PENDING.
private static final List<PaymentStatus> IN_PROGRESS_OR_DONE = List.of(APPROVING, APPROVED);
```

### 5.3 계약이 있는 콜백

```java
/** Called in the same transaction right after the approval is recorded in the ledger. Confirms the target */
void onApproved(Long targetId);
```

### 5.4 Before → After

```java
// Before: three sentences of design rationale in the class comment
/**
 * PaymentGateway의 토스페이먼츠 구현.
 * 도메인 언어(paymentNumber, gatewayToken, transactionId)와 토스 언어(orderId, paymentKey, transactionKey)의
 * 변환은 이 클래스에서 끝나며, 토스 응답과 오류 형식은 도메인으로 올라가지 않는다.
 */

// After: one-line summary; rationale moves to a // comment above the method that does the translation
/** Toss Payments implementation of PaymentGateway */
```

---

## 6. 리뷰 체크리스트

- [ ] public 타입에 한 줄 요약이 있는가
- [ ] 코드나 이름을 반복하는 주석이 없는가
- [ ] 예외 계약(언제 던지고 언제 안 던지는지)이 포트 메서드에 적혀 있는가
- [ ] "왜"가 필요한 결정에 `//` 이유 주석이 있는가
- [ ] 두 문장을 넘는 주석이 `docs/`로 옮겨졌는가
- [ ] 빈 `@param`/`@return` 골격, `@author`, 주석 처리된 코드가 없는가
- [ ] 새 주석이 영어로 쓰였고, 한 파일 안에서 언어가 섞이지 않았는가

---

## 7. 요약 치트시트

```
타입:      /** one-line summary, noun/verb phrase, no period */    public 타입은 필수
메서드:    계약이 있을 때만. @Override·getter·DTO 변환은 생략
태그:      @param(비자명할 때만) → @return(요약에 없을 때만) → @throws(호출자가 구분할 예외)
//:        why · precondition · external fact · workaround · TODO. 멤버 바로 위, 한두 줄
언어:      영어. 한 파일 안에서 섞지 않음
금지:      코드 반복, 이름 반복, @author/@since, 변경 이력, 주석 처리된 코드
길어지면:  docs/ 로 보내고 결론만 남긴다
```

---