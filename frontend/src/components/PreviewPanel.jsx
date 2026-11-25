import { lengthOptions } from "./LengthSelector";

const paragraphs = {
  short: [
    "트랜스포머는 어텐션 메커니즘만을 기반으로 하는 새로운 신경망 구조입니다.",
    "순환 구조나 합성곱 없이도 문장 속 단어 간의 관계를 학습합니다.",
    "병렬 처리로 학습 효율을 높이며 다양한 자연어 처리 작업에 활용할 수 있습니다.",
  ],
  medium: [
    "트랜스포머는 어텐션 메커니즘만을 기반으로 하는 새로운 신경망 구조입니다. 순환 구조나 합성곱 없이도 문장 속 단어 간의 관계를 학습합니다.",
    "이 구조는 병렬 처리를 가능하게 하여 학습 효율을 높입니다. 기계 번역을 비롯한 다양한 자연어 처리 작업에 활용할 수 있습니다.",
  ],
  long: [
    "트랜스포머는 어텐션 메커니즘만을 기반으로 하는 새로운 신경망 구조입니다. 순환 구조나 합성곱 없이도 문장 속 단어 간의 관계를 학습합니다. 이 구조는 병렬 처리를 가능하게 하여 학습 효율을 높입니다. 기계 번역을 비롯한 다양한 자연어 처리 작업에 활용할 수 있습니다.",
  ],
};

export function PreviewPanel({ length }) {
  const option = lengthOptions[length];

  return (
    <aside className="rounded-[20px] bg-blue-soft p-7">
      <h2 className="font-bold-face text-xl leading-[1.35] text-ink">이렇게 읽게 돼요</h2>
      <p className="font-regular-face mt-1.5 text-[15px] leading-[1.35] text-muted">
        선택한 문단 길이를 미리 확인하세요.
      </p>

      <article className="mt-[25px] min-h-[425px] rounded-[18px] border border-line bg-white p-[23px]">
        <div className="flex items-center justify-between">
          <span className="font-semibold-face text-[15px] leading-[1.252] text-blue">
            번역 예시
          </span>
          <span className="font-medium-face rounded-[14px] bg-blue-soft px-[13px] py-[5px] text-[13px] leading-[1.35] text-blue">
            {option.label}
          </span>
        </div>
        <h3 className="font-bold-face mt-[21px] text-[17px] leading-[1.35] text-ink">
          Attention Is All You Need
        </h3>
        <p className="font-regular-face mt-1.5 text-[13px] leading-[1.35] text-muted">
          초록 · Abstract
        </p>
        <div className="mt-[17px] space-y-[13px]">
          {paragraphs[length].map((paragraph) => (
            <p className="font-regular-face text-[17px] leading-[1.5] text-ink" key={paragraph}>
              {paragraph}
            </p>
          ))}
        </div>
      </article>

      <p className="font-semibold-face mt-[21px] text-[15px] leading-[1.252] text-blue">
        {option.label}&nbsp;&nbsp; · &nbsp;&nbsp;{option.summary}
      </p>
      <div className="font-regular-face mt-2 text-[15px] leading-[1.35] text-muted">
        {option.description.map((line) => (
          <p key={line}>{line}</p>
        ))}
      </div>
    </aside>
  );
}
