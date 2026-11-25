export function Hero() {
  return (
    <section className="mx-auto flex min-h-[204px] w-full max-w-[1200px] items-start justify-between gap-8 px-6 pt-8 xl:px-0">
      <div>
        <p className="font-semibold-face text-[15px] leading-[1.252] text-blue">
          RESEARCH WITHOUT BARRIERS
        </p>
        <h1 className="font-bold-face mt-[14px] text-[26px] leading-[1.35] text-[#191f28]">
          영문 IT 논문, 이제 한국어로 읽으세요.
        </h1>
        <p className="font-regular-face mt-[13px] text-[17px] leading-[1.5] text-muted">
          논문을 업로드하고 읽기 좋은 문단 길이를 선택하세요. 복잡한 연구가 더 가까워집니다.
        </p>
      </div>

      <div className="font-semibold-face mt-[49px] hidden h-[46px] min-w-[252px] items-center justify-center rounded-[18px] bg-white px-5 text-[15px] leading-[1.252] text-ink md:flex">
        EN&nbsp; 영어{" "}
        <span className="mx-4" aria-hidden="true">
          →
        </span>{" "}
        KO&nbsp; 한국어
      </div>
    </section>
  );
}
