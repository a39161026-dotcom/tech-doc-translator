import { BrandIcon } from "./BrandIcon";

export function Header() {
  return (
    <header className="h-20 bg-white">
      <div className="mx-auto flex h-full w-full max-w-[1200px] items-center px-6 xl:px-0">
        <a className="flex items-center gap-3" href="#" aria-label="paperly 홈">
          <span className="flex size-[34px] items-center justify-center rounded-2xl bg-blue-soft">
            <BrandIcon />
          </span>
          <span className="font-bold-face text-2xl leading-[1.35] text-ink">paperly</span>
          <span className="font-medium-face rounded-full bg-navy-faint px-[7px] py-[3px] text-[13px] leading-[1.35] text-muted">
            BETA
          </span>
        </a>

        <nav className="ml-9 hidden items-center gap-[50px] sm:flex" aria-label="주요 메뉴">
          <a className="font-semibold-face text-[15px] leading-[1.252] text-blue" href="#translate">
            논문 번역
          </a>
          <a className="font-semibold-face text-[15px] leading-[1.252] text-muted" href="#archive">
            내 보관함
          </a>
        </nav>

        <a
          className="font-semibold-face ml-auto text-[15px] leading-[1.252] text-muted"
          href="#guide"
        >
          이용 가이드
        </a>
      </div>
    </header>
  );
}
