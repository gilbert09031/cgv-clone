# 영화관 도메인 로그 이벤트

[카탈로그](README.md) · [공통 계약](common.md) · 계약 상태: 초안

## 대상 API

| API (`httpMethod` + `httpRoute`) | 관측 목적 |
|---|---|
| `GET /api/theaters` | 지역별 영화관 조회의 상태·지연 |
| `GET /api/theaters/{theaterId}` | 특정 영화관 조회 실패·지연 |

목록 API의 `region`은 쿼리 입력이며 `httpRoute`에 포함하지 않는다. `operation`·`domain`은 추가하지 않는다. API 목록은 로그 발생이나 필드 자동 주입을 요구하지 않는다.

## 이벤트 계약

| 상황 | 처리·이벤트 | 레벨 | 기록 책임·시점 | 현재 상태 |
|---|---|---|---|---|
| 영화관 목록·상세 응답 완료 | 기록하지 않음. 메트릭·Tracing으로 관측 | — | 로그 없음 | 공통 로그 미기록 |
| 존재하지 않는 영화관 상세 조회 | `api.request.rejected` DEBUG. `T001` 응답 반환 | DEBUG | 전역 처리기 | 공통 적용 |
| region 누락·enum 변환 실패·경로 ID 타입 오류 | `api.request.rejected` DEBUG. 오류 응답 반환 | DEBUG | 전역 처리기 | 입력 예외 매핑 보완 필요 |
| DB·응답 변환 등의 예상 밖 오류 | `api.request.errored` | ERROR | 전역 처리기, 상세 예외 1회 | 공통 적용 |

`api.request.errored`는 [공통 계약](common.md)을 따른다. 목록 오류의 `region`(string, 검증된 enum 이름), 상세 오류의 `theaterId`(number)는 원인 분석에 필요한 경우에만 선택적으로 추가한다. 전체 쿼리 문자열과 변환에 실패한 입력 원문은 저장하지 않는다.

조회 성공에 별도 도메인 INFO 이벤트는 추가하지 않는다. 영화관·상영관 등록 등 관리자 작업은 이번 범위에 포함하지 않는다.

## 현재 동작과 확인할 점

- 별도 영화관 도메인 로그는 없다.
- 유효한 지역에 영화관이 없으면 정상 빈 목록이다.
- 잘못된 `region`은 컨트롤러 바인딩 실패이며, 현재 서비스가 `REGION_NOT_FOUND`를 던지는 상황과 같다고 취급하지 않는다.

## 소스 근거

- [TheaterController](../../src/main/java/com/ceos22/cgv_clone/domain/theater/controller/TheaterController.java): `getTheaters`, `getTheaterDetail`
- [TheaterService](../../src/main/java/com/ceos22/cgv_clone/domain/theater/service/TheaterService.java): `findTheatersByRegion`, `findTheaterById`
