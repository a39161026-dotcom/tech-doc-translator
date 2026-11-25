# API 명세 (초안)

프론트(김대희)와 백엔드(이성헌)가 함께 확정하는 문서. 필드와 경로는 협의 후 바꿀 수 있다.

- Base URL: `/api`
- 형식: JSON (파일 업로드만 `multipart/form-data`)
- 인증: 로그인 후 세션 쿠키 또는 JWT (**미정**, 확정 전까지 프론트는 `Authorization` 헤더 가정 없이 개발)
- 시간: ISO 8601 문자열 (`2026-11-08T23:00:00`)

## 공통 에러 응답

```json
{ "code": "DOCUMENT_NOT_FOUND", "message": "문서를 찾을 수 없습니다." }
```

| HTTP | 의미 |
| --- | --- |
| 400 | 요청 값 오류 |
| 401 | 로그인 필요 |
| 403 | 다른 사용자의 데이터 접근 |
| 404 | 대상 없음 |
| 409 | 중복 (이메일, 용어 등), 현재 상태에서 할 수 없는 요청 (`INVALID_STATUS`, `ALREADY_TRANSLATING`) |

## 회원 (F1)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/members` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/logout` | 로그아웃 (204, 아래 참고) |
| GET | `/api/members/me` | 지금 로그인한 회원 (토큰이 유효한지 확인할 때) |

**인증 방식: JWT.** `app.auth.enabled=true`일 때만 적용되고, 꺼져 있으면(기본값) 지금처럼 로그인 없이 임시 회원으로 동작합니다. 프론트에 로그인 화면이 생기면 켭니다.

- 로그인 응답의 `token`을 저장해 두고, 이후 모든 요청에 `Authorization: Bearer <token>` 헤더를 붙입니다.
- 토큰 없이 호출하면 401 `LOGIN_REQUIRED`, 만료·위조된 토큰이면 401 `INVALID_TOKEN`입니다. 회원가입·로그인·로그아웃은 토큰 없이 호출합니다.
- 토큰 유효 시간은 24시간입니다. 로그아웃은 서버에 상태가 없으므로 프론트가 저장한 토큰을 지우면 됩니다.
- 남의 문서에 접근하면 404 `DOCUMENT_NOT_FOUND`로 답합니다.

`POST /api/auth/login`
```json
// 요청
{ "email": "a@b.com", "password": "******" }
// 응답 200
{ "token": "eyJhbGciOi...", "member": { "id": 1, "email": "a@b.com", "nickname": "성헌" } }
```
오류: 이메일 또는 비밀번호가 틀리면 401 `INVALID_CREDENTIALS`(어느 쪽이 틀렸는지는 알려 주지 않음), 이미 가입된 이메일로 가입하면 409 `EMAIL_ALREADY_EXISTS`, 비밀번호가 8자 미만이면 400 `VALIDATION_FAILED`.

`POST /api/members`
```json
// 요청
{ "email": "a@b.com", "password": "******", "nickname": "성헌" }
// 응답 201
{ "id": 1, "email": "a@b.com", "nickname": "성헌" }
```

## 문서 (F2, F6, F8)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/documents` | 문서 업로드 (`file`, 선택 `title`) |
| GET | `/api/documents` | 내 문서 목록 (최신순) |
| GET | `/api/documents/{id}` | 문서 상세와 진행 상태 |
| GET | `/api/documents/{id}/result` | 결과 파일 다운로드 (`type`, `format` 쿼리, 아래 참고) |
| DELETE | `/api/documents/{id}` | 문서와 문단 삭제 (204). 번역·요약 중이면 409 `DOCUMENT_BUSY`, 없으면 404 |

`GET /api/documents/{id}` 응답
```json
{
  "id": 10,
  "title": "Spring Boot Reference",
  "originalFilename": "spring.txt",
  "status": "TRANSLATING",
  "segmentCount": 40,
  "doneCount": 12,
  "failedCount": 1,
  "summaryStatus": "NONE",
  "summaryLength": null,
  "createdAt": "2026-11-08T23:00:00"
}
```

`status`: `UPLOADED`, `TRANSLATING`, `DONE`, `FAILED` (문단이 하나라도 실패하면 `FAILED`)

## 번역 (F3, F5)

번역은 오래 걸리므로 시작 요청과 결과 조회를 분리한다. 프론트는 상태를 주기적으로 조회(폴링)한다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/documents/{id}/translate` | 번역 시작 (`UPLOADED` 상태만, 비동기, 202 반환) |
| GET | `/api/documents/{id}/segments` | 문단별 원문·번역 목록 |
| PATCH | `/api/segments/{id}` | 문단 번역 직접 수정 (구현 완료) |
| POST | `/api/documents/{id}/retry` | 실패한 문단만 다시 번역 (`FAILED` 상태만, 202 반환) |

`GET /api/documents/{id}/segments` 응답
```json
[
  { "id": 101, "seq": 1, "sourceText": "Spring Boot makes it easy...", "translatedText": "스프링 부트는 ...", "status": "DONE" },
  { "id": 102, "seq": 2, "sourceText": "...", "translatedText": null, "status": "FAILED" }
]
```

`Segment.status`: `PENDING`, `DONE`, `FAILED`

`PATCH /api/segments/{id}` (문단 번역문 직접 수정)

요청
```json
{ "translatedText": "직접 고친 번역" }
```
- 응답: 수정된 문단 (`segments` 목록의 한 항목과 같은 형식, `status`는 `DONE`)
- 번역이 끝난(`DONE`) 문서나 실패 문단이 있는(`FAILED`) 문서만 가능
- 실패·미번역 문단이 더 없으면 문서 상태도 `DONE`으로 바뀜 (그러면 요약·다운로드 가능)
- 오류: 빈 번역문 400 `VALIDATION_FAILED`, 없는 문단 404 `SEGMENT_NOT_FOUND`, 번역 전 409 `NOT_TRANSLATED`, 번역 중 409 `ALREADY_TRANSLATING`
- 이미 만든 요약은 자동으로 갱신되지 않음 (다시 요약하면 반영)

## 요약 (F5)

번역이 끝난(`DONE`) 문서만 요약할 수 있다. 번역처럼 시작 요청과 결과 조회를 분리하고, 프론트는 상태를 주기적으로 조회(폴링)한다. 다시 요약하면 이전 요약을 덮어쓴다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/documents/{id}/summarize` | 요약 시작 (비동기, 202 반환) |
| GET | `/api/documents/{id}/summary` | 요약 상태와 결과 조회 |

`POST /api/documents/{id}/summarize`
```json
// 요청: 요약 길이를 사용자가 고른다
{ "length": "SHORT" }
```

`length`: `SHORT`(짧게), `MEDIUM`(보통), `LONG`(길게). 없거나 잘못된 값이면 400.

`GET /api/documents/{id}/summary` 응답
```json
{ "status": "DONE", "length": "SHORT", "text": "요약문..." }
```

`status`: `NONE`(요약 안 함), `SUMMARIZING`, `DONE`, `FAILED`

| HTTP | code | 의미 |
| --- | --- | --- |
| 409 | `NOT_READY` | 번역이 끝나지 않은 문서 |
| 409 | `ALREADY_SUMMARIZING` | 이미 요약 중 |
| 400 | `VALIDATION_FAILED` | `length`가 없음 |
| 400 | `INVALID_REQUEST` | 요청 본문을 읽을 수 없음 (없는 `length` 값 등) |

## 결과 파일 다운로드 (F6)

`GET /api/documents/{id}/result?type={type}&format={format}`

| 쿼리 | 값 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `type` | `summary`, `full` | `full` | 요약본 또는 번역 전문 |
| `format` | `docx`, `md`, `txt`, `pdf` | `md` | 파일 형식. PDF에는 한글 글꼴(NanumGothic)이 포함됨 |

응답은 파일 다운로드(`Content-Disposition: attachment`)다. 파일 이름은 `문서제목_요약.docx`, `문서제목_번역.md` 형식이다.

| HTTP | code | 의미 |
| --- | --- | --- |
| 409 | `NOT_READY` | `type=full`인데 번역이 끝나지 않음 |
| 409 | `SUMMARY_NOT_READY` | `type=summary`인데 요약이 끝나지 않음 |
| 400 | `UNSUPPORTED_TYPE` | `type`이 `summary`, `full`이 아님 |
| 400 | `UNSUPPORTED_FORMAT` | 지원하지 않는 형식 |

## 용어집 (F4, F9)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/terms` | 내 용어 목록 |
| POST | `/api/terms` | 용어 등록 |
| PUT | `/api/terms/{id}` | 용어 수정 |
| DELETE | `/api/terms/{id}` | 용어 삭제 |

`POST /api/terms`
```json
// 요청
{ "sourceTerm": "dependency injection", "targetTerm": "의존성 주입", "note": "" }
// 응답 201
{ "id": 5, "sourceTerm": "dependency injection", "targetTerm": "의존성 주입", "note": "" }
```

같은 사용자가 같은 `sourceTerm`을 다시 등록하면 409.

## CORS

개발용으로 `http://localhost:3000`, `http://localhost:5173`에서 `/api/**` 호출을 허용한다. 다른 주소는 설정 `app.cors.allowed-origins`로 추가한다.

## 미정

- 인증 방식 (세션 vs JWT)
- 지원하는 입력 파일 형식과 업로드 최대 크기 (현재 txt, md, 최대 5MB)
- 용어 후보 추천(F9) API
