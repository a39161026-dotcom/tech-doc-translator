# tech-doc-translator

영어 IT 기술 문서(개발 문서, 논문)를 로컬 LLM으로 한국어로 번역하고, 용어를 정리해 문서 파일로 만든 뒤 DB에 저장하는 웹 서비스 (가칭).

- 백엔드: Spring Boot, Spring Data JPA, MySQL (초기 H2)
- 번역: 로컬 LLM (모델·서빙 도구 미정)
- 팀: 이성헌(백엔드), 김대희(프론트엔드)

기획 문서: [docs/PRD.md](docs/PRD.md)

## 협업 규칙
`main`에 직접 푸시하지 않는다. 기능마다 브랜치 → PR → 리뷰 → 머지.
