# 영화 도메인 로그 이벤트

[카탈로그](README.md) · [공통 계약](common.md) · 계약 상태: 초안

## 대상 API

| API (`httpMethod` + `httpRoute`) | 관측 목적 |
|---|---|
| `GET /api/movies` | 현재 상영 영화 조회의 상태·지연 |
| `GET /api/movies/{movieid}` | 특정 영화 상세 조회 실패·지연 |

현재 컨트롤러의 라우트 변수는 `{movieid}`다. `operation`·`domain`은 추가하지 않는다. API 목록은 로그 발생이나 필드 자동 주입을 요구하지 않는다.

## 이벤트 계약

| 상황 | 처리·이벤트 | 레벨 | 기록 책임·시점 | 현재 상태 |
|---|---|---|---|---|
| 영화 목록·상세 응답 완료 | 기록하지 않음. 메트릭·Tracing으로 관측 | — | 로그 없음 | 공통 로그 미기록 |
| 존재하지 않는 영화 상세 조회 | `api.request.rejected` DEBUG. `M001` 응답 반환 | DEBUG | 전역 처리기 | 공통 적용 |
| 경로 ID 타입 등 입력 오류 | `api.request.rejected` DEBUG. 오류 응답 반환 | DEBUG | 전역 처리기 | 입력 예외 매핑 보완 필요 |
| DB·응답 변환 등의 예상 밖 오류 | `api.request.errored` | ERROR | 전역 처리기, 상세 예외 1회 | 공통 적용 |

`api.request.errored`는 [공통 계약](common.md)을 따른다. 영화 상세 오류에서 확인된 `movieId`(number)는 객체 단위 원인 분석이 필요할 때만 선택적으로 추가한다. 숫자로 변환되지 않은 ID를 원문 덤프하지 않는다. 목록 오류에는 `movieId`를 요구하지 않는다.

조회 성공마다 `movie.listed`, `movie.viewed` 등의 추가 INFO 이벤트를 만들지 않는다. 영화 등록·수정·삭제는 관리자 API 범위를 다룰 때 추가한다.

## 현재 동작과 확인할 점

- 별도 영화 도메인 로그는 없다.
- 상영 영화 목록이 비어 있으면 정상 빈 목록이다. 실패 이벤트를 기록하지 않는다.
- 상세 조회에서 `MOVIE_NOT_FOUND`를 반환한다.
- 예상 가능한 요청 거절은 전역 처리기의 공통 DEBUG 진단 이벤트로만 남긴다. 입력 예외의 세부 매핑은 공통 계약에 적힌 보완 범위를 따른다.

## 소스 근거

- [MovieController](../../src/main/java/com/ceos22/cgv_clone/domain/movie/controller/MovieController.java): `getNowPlayingMovies`, `getMovieDetail`
- [MovieService](../../src/main/java/com/ceos22/cgv_clone/domain/movie/service/MovieService.java): `findAllNowPlayingMovies`, `findMovieById`
