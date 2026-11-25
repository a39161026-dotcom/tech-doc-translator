export function Footer() {
  return (
    <footer className="mx-auto w-full max-w-[1200px] px-6 pb-5 pt-8 text-muted xl:px-0">
      <div className="flex items-center justify-between gap-6">
        <p className="font-regular-face text-[15px] leading-[1.35]">
          원문의 의미는 유지하고, 읽는 부담은 줄이세요.
        </p>
        <p className="font-regular-face hidden text-[13px] leading-[1.35] sm:block">
          IT · 컴퓨터공학 · AI · 소프트웨어
        </p>
      </div>
      <div className="mt-10 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-8">
          <strong className="font-semibold-face text-[15px] leading-[1.252]">paperly</strong>
          <span className="font-regular-face text-[13px] leading-[1.35]">
            연구를 이해하는 더 쉬운 방법
          </span>
        </div>
        <div className="font-regular-face flex gap-6 text-[13px] leading-[1.35]">
          <a href="#help">도움말</a>
          <span aria-hidden="true">·</span>
          <a href="#privacy">개인정보 처리방침</a>
        </div>
      </div>
    </footer>
  );
}
