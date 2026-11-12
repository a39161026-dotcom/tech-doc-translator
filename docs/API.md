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
| POST | `/api/auth/logout` | 로그아웃 |

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
| GET | `/api/documents/{id}/result` | 번역 문서 파일 다운로드 |
| DELETE | `/api/documents/{id}` | 문서 삭제 |

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
| PATCH | `/api/segments/{id}` | 문단 번역 직접 수정 |
| POST | `/api/documents/{id}/retry` | 실패한 문단만 다시 번역 (`FAILED` 상태만, 202 반환) |

`GET /api/documents/{id}/segments` 응답
```json
[
  { "id": 101, "seq": 1, "sourceText": "Spring Boot makes it easy...", "translatedText": "스프링 부트는 ...", "status": "DONE" },
  { "id": 102, "seq": 2, "sourceText": "...", "translatedText": null, "status": "FAILED" }
]
```

`Segment.status`: `PENDING`, `DONE`, `FAILED`

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

## 미정

- 인증 방식 (세션 vs JWT)
- 지원 파일 형식과 업로드 최대 크기
- 번역 문서 다운로드 형식 (docx, pdf, md 중)
- 용어 후보 추천(F9) API
