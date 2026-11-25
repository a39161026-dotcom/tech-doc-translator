# Paperly UI

Figma에서 만든 논문 업로드 화면을 JavaScript + React 컴포넌트로 모듈화한 프런트엔드입니다. Vite와 Tailwind CSS를 사용합니다.

## 실행

Node.js 22.12 이상과 pnpm이 필요합니다.

```sh
pnpm install
pnpm dev
```

배포용 빌드와 미리보기:

```sh
pnpm build
pnpm preview
```

## 구성

- `src/App.jsx`: 문단 길이 상태와 화면 구성
- `src/components/Header.jsx`: 상단 내비게이션
- `src/components/BrandIcon.jsx`: 브랜드 아이콘
- `src/components/Hero.jsx`: 안내 문구와 번역 방향
- `src/components/UploadPanel.jsx`: PDF 선택·드래그 앤 드롭, 20MB 제한, 번역 버튼
- `src/components/LengthSelector.jsx`: SHORT / MEDIUM / LONG 선택
- `src/components/PreviewPanel.jsx`: 선택에 따른 예시 문단 표시
- `src/components/Footer.jsx`: 하단 안내
- `src/index.css`: 색상, 타이포그래피, 공통 스타일
- `public/assets/`: 화면에서 사용하는 SVG 리소스

## 현재 범위

파일 선택과 검증은 브라우저에서만 수행합니다. 선택한 PDF가 서버로 전송되거나 실제 번역되는 기능은 아직 연결되어 있지 않습니다. 번역 버튼은 준비 안내만 표시하며, 우측 내용은 고정된 예시입니다. 내 보관함·가이드·도움말·개인정보 링크는 연결 전 자리표시자입니다.

독립 실행을 위해 Figma 전용 개발 설정 대신 표준 `vite.config.js`와 한국어 `index.html`을 제공합니다. 원래 저장소의 README는 유지합니다.
