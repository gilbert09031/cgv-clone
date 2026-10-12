# 공통 로그 이벤트

[카탈로그](README.md) · 계약 상태: 초안

## 최소 공통 필드

애플리케이션 이벤트의 공통 필드는 아래 두 가지로 제한한다. `@timestamp`와 `level`은 로깅 프레임워크가 출력하는 기본 메타데이터이며, 이벤트마다 직접 추가하지 않는다. 로거 이름 등 프레임워크의 기존 출력까지 삭제하라는 의미는 아니다.

| 필드 | 타입 | 적용 조건·의미 |
|---|---|---|
| `event` | string | 모든 카탈로그 이벤트. 사건을 식별하는 고정된 `resource.action` 이름 |
| `requestId` | string | HTTP 요청 안에서 발생한 이벤트에만 포함. 기존 요청 문맥으로 같은 요청의 로그를 연결 |

HTTP 요청이 없는 작업에는 `requestId`를 임의로 만들지 않는다. 선택 필드는 값을 알 수 없으면 생략하며 빈 문자열이나 추정값으로 채우지 않는다.

`requestId`는 현재 클라이언트가 전달한 안전한 형식의 값도 수용한다. 서버가 항상 유일하게 발급한 ID라고 간주하지 않으며 인증·권한 판정에 사용하지 않는다. 분산 추적이 구현되지 않은 현재 단계에서는 `traceId`, `spanId`를 필수 필드로 요구하지 않는다.

### HTTP 오류 이벤트의 필드

다음 필드는 HTTP 오류의 진단에 필요한 경우 해당 이벤트에서 선언한다. 모든 도메인 로그에 자동으로 붙이는 공통 필드가 아니다.

| 필드 | 타입 | 적용 조건·의미 |
|---|---|---|
| `statusCode` | number | 오류 응답 결과를 확인해야 하며 최종 HTTP 상태가 확정되었을 때만 기록 |

작업은 `event` 이름으로 구분하고 `operation`을 추가하지 않는다. 도메인은 이벤트 이름·문서의 소유 관계로 구분하며 `domain`을 일괄 추가하지 않는다. HTTP 메서드·경로는 로그 필드로 기록하지 않는다.

요청 전체의 `durationMs`는 공통 필드로 요구하지 않는다. 요청 지연은 메트릭·Tracing으로 관측한다. 외부 호출 시간처럼 특정 이벤트에 필요한 측정값은 그 이벤트에서 측정 범위와 단위를 정의한다.

### 이벤트별 필드와 공통 승격

- 도메인 식별자, 오류 코드, 결과, 재시도 횟수 등은 필요한 이벤트의 필수·선택 필드로 선언한다. 같은 도메인의 다른 이벤트에도 자동 적용하지 않는다.
- 이벤트 이름만으로 확정되는 상태·결과·처리 구간을 필드로 반복하지 않는다. 여러 결과를 갖는 이벤트의 `outcome`처럼 추가 구분이 필요한 값은 유지한다.
- 같은 값을 담는 식별자를 여러 이름으로 중복 기록하지 않는다. 이번 예매 결제 범위에서는 `reservationId`와 동일한 `targetId`를 함께 요구하지 않는다.
- 여러 이벤트에서 같은 의미·타입·적용 조건으로 실제 반복되고 공동 관리가 필요해졌을 때 공통 승격을 검토한다. 해당 이벤트와 사용 근거를 문서에 남기고, 도메인 내부 공통으로 충분한지 먼저 확인한다.
- 이름·타입을 문서에서 재사용하는 것과 모든 로그에 자동 출력하는 것은 구분한다. 공통 승격 시 적용 범위·필수 조건·기존 검색 영향도 함께 정의한다.

### 현재 구현

필터는 `requestId`만 MDC에 넣고 요청이 끝나면 이전 문맥을 복원한다. HTTP 메서드·경로는 오류 로그에도 별도로 추가하지 않는다. 요청의 로그는 `requestId`로 연결한다.

상세 예외는 원본 예외를 `setCause`에 전달해 로깅 프레임워크의 기본 형식으로 출력한다. 예외 타입·메시지·원인 체인·suppressed 예외를 별도로 가공하지 않는다. 예외 메시지의 민감정보는 자동으로 제거되지 않으므로 예외 생성 시 포함하지 않는다. 오류 분류는 이벤트 이름과 명시적인 코드 필드로 처리한다.

## 이벤트 계약

### 운영 로그 정책

정상 요청의 종료 로그는 기록하지 않는다. 예상 가능한 요청 거절은 최종 거절 경계에서 구조화 `DEBUG` 로그로만 남기며, 기본 운영 레벨에서는 출력되지 않는 진단 정보로 취급한다. 필터는 RequestID 발급·응답 헤더 전파와 MDC 설정·정리를 담당하며, 필터 밖으로 전파되는 예상 밖 실패만 ERROR 요약으로 기록한다.

지연시간 관측의 역할은 다음처럼 나눈다.

- **메트릭:** 전체 요청 지연 분포와 p95·p99 집계, 알림
- **Tracing:** 개별 요청의 end-to-end 경로와 DB·외부 호출 등 span별 지연 분석
- **로그:** 예상 밖 오류·정합성 위험의 원인, 필요한 도메인 상태 변화, 필요 시 DEBUG 거절 분류. 오류 로그를 RequestID로 연결

요청률·HTTP 응답 상태별 비율·지연시간은 메트릭으로 관측한다. 현재 Tracing의 도입과 구간별 계측이 완료된 것은 아니다. Tracing을 추가한 뒤에도 샘플링 때문에 모든 요청의 trace가 보존된다고 가정하지 않는다. 공통 로그 삭제가 메트릭·Tracing의 구성과 검증을 대신하지 않는다.

| 이벤트 | 목표 레벨 | 발생 상황·기록 시점 | 추가 필수 필드 | 기록 책임 | 현재 상태 |
|---|---|---|---|---|---|
| `http.request.failed` | ERROR | 예외가 필터 밖으로 전파되어 필터 체인이 정상 반환되지 않음 | 없음 | 요청 필터 | 구현 완료: 필터·MVC 오류 회귀 테스트. 파일 출력 검증 범위는 아래 기록 참조 |
| `api.request.rejected` | DEBUG | 도메인·입력 오류를 최종 응답으로 변환하는 경계. 정상 운영 레벨에서는 출력하지 않음 | `errorCode`, `statusCode` | 전역 예외 처리기 | 구현 완료: 응답 동작 유지, DEBUG 진단 필드 검증 |
| `auth.authentication_rejected` | DEBUG | JWT·인증 실패를 401 응답으로 확정하는 경계 | `errorCode`, `statusCode` | JWT 필터·인증 진입점 | 구현 완료: JWT 원문·Authorization 헤더·스택 트레이스 비기록 검증 |
| `auth.authorization_rejected` | DEBUG | 접근 권한이 없어 403 응답으로 확정하는 경계 | `errorCode`, `statusCode` | 권한 거절 처리기 | 구현 완료: 스택 트레이스·원문 메시지 비기록 검증 |
| `api.request.errored` | ERROR | 상세 도메인 오류 로그가 없는 예상 밖 예외. 최종 응답 변환 시 기록 | 상세 예외 | 전역 예외 처리기 | 구현 완료: 전역 처리기·인증 필터의 구조화 ERROR. 원본 상세 예외 출력 |

거절 이벤트의 `errorCode`(string)는 원인 분류 코드이고, `statusCode`(number)는 실제 최종 응답 상태다. JWT 필터에서 `CustomException`이 원인인 경우 `errorCode`는 원인 코드지만 응답은 현재 `A001` AUTHENTICATION_FAILED 401로 유지된다. 전역 입력 예외도 응답 매핑은 이번 변경에서 바꾸지 않는다. `TypeMismatchException`은 `C002`, `HttpMessageNotReadableException`은 `C001`, 그 밖의 Spring 4xx `ErrorResponse`는 구체 분류가 없으면 `C007`로 기록하되 현재 응답은 기존 500을 유지할 수 있다.

`api.request.errored`의 `errorCode`(string)는 내부 오류 코드로 원인을 더 구분할 수 있을 때 선택적으로 기록한다. `statusCode`는 위 적용 조건을 따른다. 상세 예외는 로깅 프레임워크의 예외 출력으로 한 번만 남긴다. `stage`·고정 `outcome`은 공통 필수 필드가 아니다.

`http.request.completed`는 제거했으며 정상 요청 완료는 계속 기록하지 않는다. 입력·인증·권한·좌석 충돌·상태 검증 등의 예상 가능한 요청 거절은 응답 코드·본문으로 전달하고, 필요 시 위 DEBUG 이벤트로만 분류를 남긴다. 예외 처리기가 응답으로 변환한 4xx·5xx도 필터에서 종료 로그를 추가하지 않는다.

예상 밖 오류는 상세 도메인 이벤트 또는 전역 처리기의 ERROR로 기록한다. 외부 결제 승인 결과·결제 기록 실패 같은 도메인 사건은 각 도메인 계약을 따르며 HTTP 요청 거절과 구분한다. HTTP 결과와 결제 결과도 독립적이다. 예를 들어 HTTP 502여도 결제 승인 여부는 unknown일 수 있다.

프레임워크의 4xx ErrorResponse 예외, 타입 변환 실패, 읽을 수 없는 요청 본문도 전역 처리기에서 `api.request.rejected` DEBUG로만 분류한다. 현재 구체적인 입력 예외 응답 매핑이 없어 일부 입력 오류는 기존 일반 예외 처리의 500 응답을 유지한다. 입력 오류를 올바른 4xx로 매핑하는 것은 별도 보완 사항이며 이번 변경에서는 응답 동작을 바꾸지 않는다.

입력 오류의 목표 매핑은 필수 값 누락·Bean Validation 실패·읽을 수 없는 요청 본문에 `C001`(INVALID_INPUT_VALUE), 경로·쿼리·필드 타입 또는 enum 변환 실패에 `C002`(INVALID_TYPE_VALUE)다. 실제 예외 처리 구현에서 더 구체적인 분류가 필요하면 이 계약을 함께 갱신한다.

## 중복·레벨·집계 규칙

- 정상 요청 완료는 공통 로그 0건을 목표로 한다. 예상 가능한 요청 거절은 WARN/INFO가 아니라 최종 경계의 DEBUG 진단 이벤트로만 남긴다.
- DEBUG 거절 이벤트는 스택 트레이스, 원문 예외 메시지, JWT·Authorization 헤더, 요청·응답 전체 바디를 기록하지 않는다.
- 요청 전체 지연 분포는 메트릭, 개별 요청의 구간별 지연은 Tracing이 담당하도록 구성·검증한다.
- 비동기 경로를 추가할 때 메트릭·Tracing의 완료 처리와 오류 로그의 MDC 전달을 별도로 검증한다.
- 시스템 오류·정합성 위험은 ERROR, 필요한 커밋된 도메인 상태 변화는 INFO로 기록한다.
- `http.request.failed`는 요청 실패 요약이다. 상세 스택 트레이스는 원인 처리 책임 지점에서 한 번만 남긴다.
- 운영의 HTTP 요청률·실패율·지연 분포는 메트릭으로 집계한다. 로그는 RequestID로 예상 밖 오류를 연결해 원인을 분석하며, 오류 요약과 상세 로그를 합산하지 않는다.
- 조회 결과 0건은 해당 API 계약상 정상일 수 있다. 오류를 새로 만들어 기록하지 않는다.

## 식별자·민감정보·Loki 규칙

| 항목 | 계약 |
|---|---|
| 결제 키·인증정보 | `paymentKey`, `gatewayToken`, JWT, Authorization 헤더 원문을 기록하지 않음 |
| `transactionId` | 현재 Toss에서는 `paymentKey`이므로 원문 기록 금지. 일반 식별자로 취급하지 않음 |
| 요청·응답·예외 | 전체 바디, 쿼리 문자열, 외부 원문 오류 메시지 덤프 금지. 스택 트레이스도 민감값 포함 여부 확인 |
| Loki 라벨 | 우선 기존 `service_name`, `environment` 유지. ID·실제 요청 경로는 라벨이 아닌 JSON 필드로 검색 |

프레임워크 출력 필드와 MDC·이벤트 필드가 충돌하지 않도록 한다. `event`와 값을 SLF4J key-value 필드로 출력하고, 사람이 읽는 `message`는 선택 정보로 둔다. Alloy의 timestamp 처리와 JSON 필드의 실제 검색 가능 여부는 구현 후 검증한다.

### DEBUG 거절 로그 확인 방법

평소 운영 레벨에서는 예상 가능한 거절 진단 로그가 보이지 않아도 된다. 필요한 범위만 임시로 켜서 확인한다.

```yaml
logging:
  level:
    com.ceos22.cgv_clone.common.error.GlobalExceptionHandler: DEBUG
    com.ceos22.cgv_clone.common.security.CustomAuthenticationEntryPoint: DEBUG
    com.ceos22.cgv_clone.common.security.CustomAccessDeniedHandler: DEBUG
    com.ceos22.cgv_clone.domain.auth.jwt.JwtAuthenticationFilter: DEBUG
```

패키지 단위 확인이 필요하면 아래처럼 좁은 범위부터 올린다.

```yaml
logging:
  level:
    com.ceos22.cgv_clone.common.security: DEBUG
    com.ceos22.cgv_clone.domain.auth.jwt: DEBUG
```

## 구현 검증 기록

아래 첫 기록은 정상 종료·거절 로그 제거 시점의 검증이다. 최소 필드·라우트 템플릿과 도메인 이벤트 구현 검증은 [후속 검증 기록](implementation-verification-2026-10-12.md)을 따른다.

2026-10-12: [RequestLoggingFilterTest](../../src/test/java/com/ceos22/cgv_clone/common/logging/RequestLoggingFilterTest.java)의 10개 케이스로 200·401·403·500 응답의 접근 로그 미기록, RequestID 전달·재발급, MDC 복원·정리, 전파된 실패의 ERROR와 RequestID, CustomException·인증·권한·JWT·입력 거절의 로그 미기록 및 예상 밖 예외의 ERROR 유지를 검증했다. `bash gradlew test --console=plain` 전체 테스트가 통과했다. 실행 중인 컨테이너의 재빌드·재배포는 수행하지 않았다.

2026-10-12: 예상 가능한 거절의 정책을 무로그에서 최종 경계 `DEBUG` 진단 이벤트로 변경했다. `RequestLoggingFilterTest`와 `StructuredLogOutputTest`의 17개 테스트로 DEBUG 설정 시 거절당 한 이벤트, MDC 복원, 요청 본문·스택 트레이스 비노출, INFO 레벨의 거절 로그 억제, 응답 유지, 만료 JWT의 401·`J003`·requestId JSON 출력을 검증했다. 전체 `bash gradlew test --console=plain`도 43개 통과, 실패·오류·건너뜀 0건이었다. 실행 중인 컨테이너 재빌드와 Loki 조회는 수행하지 않았다.

## 소스 근거

- [RequestLoggingFilter](../../src/main/java/com/ceos22/cgv_clone/common/logging/RequestLoggingFilter.java): `doFilterInternal`, requestId·MDC·전파된 실패 요약
- [GlobalExceptionHandler](../../src/main/java/com/ceos22/cgv_clone/common/error/GlobalExceptionHandler.java): 예외 응답 변환과 구조화 오류 로그
- [TransactionLog](../../src/main/java/com/ceos22/cgv_clone/common/logging/TransactionLog.java): 실제 트랜잭션의 커밋 후 상태 이벤트 기록
- [ErrorCode](../../src/main/java/com/ceos22/cgv_clone/common/error/ErrorCode.java): 내부 오류 코드와 HTTP 상태
- [application.yml](../../src/main/resources/application.yml): `logging.structured.format.file: logstash`
- [Alloy 설정](../../monitoring/alloy/config.alloy): 파일 수집·Loki 전달과 기존 라벨. 별도 처리 단계 없음
