# Git Convention & Branching 전략 (Draft)

> 본 문서는 `spring-cgv-22nd` 프로젝트의 커밋 메시지 규칙과 브랜치 전략을 정의합니다.
---

## 1. 브랜치 전략

### 1.1 기본 구조

무거운 Git Flow 대신, 소규모 프로젝트에 적합한 **경량 브랜치 전략**을 사용합니다.

| 브랜치 | 역할                              | 비고                                |
| :--- |:----------------------------------|:------------------------------------|
| `main` | 배포 가능한 안정 버전             | 직접 커밋 금지, PR으로만 병합 허용  |
| `feature/*` | 기능 개발 브랜치                  | 작업 단위마다 생성, 완료 후 삭제    |
| `fix/*` | 버그 수정 브랜치                  |                                     |
| `refactor/*` | 리팩토링 브랜치                   |                                     |
| `chore/*` | 빌드, 설정, 의존성 등 잡무성 작업 |                                     |
| `docs/*` | 문서 작업 브랜치                  |                                     |
| `hotfix/*` | 배포 후 긴급 수정                 | `main`에서 분기, `main`로 즉시 병합 |

### 1.2 브랜치 네이밍 규칙

```
<type>/<issue-번호>-<간단한-설명>
```

- 예시
  - `feature/23-reservation-concurrency`
  - `fix/47-payment-null-pointer`
  - `refactor/51-security-filter-error-response`
  - `chore/12-gitignore-cleanup`
- 이슈 번호가 없다면 생략 가능: `feature/redis-cache-strategy`
- 설명은 영문 소문자 + 하이픈(kebab-case) 사용

### 1.3 작업 흐름

1. `main`에서 최신 상태로 브랜치 생성
2. 작업 단위(커밋 컨벤션 준수)로 커밋
3. 원격 푸시 후 Pull Request 생성
4. 코드 리뷰 및 CI 통과 확인
5. **Squash and Merge**로 `main`에 병합 (커밋 히스토리 정리 목적)
6. 병합 후 원격/로컬 브랜치 삭제

---

## 2. 커밋 컨벤션

### 2.1 형식

```
<type>: <subject>

<body (선택)>

<footer (선택)>
```

- `type`과 `subject` 사이는 `콜론 + 공백 1칸`으로 통일합니다. (기존 이력에 `feat :`처럼 공백이 섞여 있던 부분을 정리)
- `subject`는 한글 또는 영문 자유, 마침표 없이 간결하게 작성 (50자 이내 권장)
- 본문(`body`)이 필요한 경우 무엇을, 왜 변경했는지 서술 (어떻게는 코드가 설명)

### 2.2 타입(type) 목록

| 타입 | 설명 |
| :--- | :--- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 구조 개선 |
| `chore` | 빌드, 패키지 매니저, 설정, 잡무성 변경 (프로덕션 코드 X) |
| `docs` | 문서 추가/수정 |
| `test` | 테스트 코드 추가/수정 |
| `style` | 코드 포맷팅, 세미콜론 등 (로직 변경 없음) |
| `perf` | 성능 개선 |
| `ci` | CI/CD 설정 변경 |

### 2.3 예시

```
feat: Redisson 분산 락 기반 재고 동시성 제어 추가

- StoreService increaseStock/decreaseStock에 분산 락 적용
- 락 키 SpEL 표현식 통일

Resolves: #23
```

```
fix: 로그아웃 시 CustomUserDetails 미조회로 발생하는 NPE 수정
```

```
refactor: PaymentService가 Member 대신 memberId를 받도록 변경
```

### 2.4 지양할 표현

- `Update`, `수정`, `작업` 등 변경 내용을 알 수 없는 커밋 메시지
- 하나의 커밋에 여러 관심사(기능 + 리팩토링 + 설정)를 함께 담는 것

---

## 3. Pull Request 규칙

### 3.1 PR 제목

커밋 컨벤션과 동일한 형식을 따릅니다. 
```
feat: 좌석 예매 동시성 제어 구현
```

---

## 4. 이슈 연동 (선택)
커밋/PR 본문에 아래 키워드를 사용하면 GitHub Issue와 자동 연동됩니다.

```
Resolves: #23
Closes: #47
Related: #12
```