export const lengthOptions = {
  short: {
    label: "SHORT",
    caption: "짧게, 한눈에 읽기",
    summary: "짧고 명확한 문단",
    description: ["핵심 내용을 빠르게.", "한눈에 내용을 파악하기 좋아요."],
  },
  medium: {
    label: "MEDIUM",
    caption: "적당하게, 균형 있게",
    summary: "균형 잡힌 문단",
    description: ["너무 짧지도, 길지도 않게.", "연구 내용을 차근차근 따라가기 좋아요."],
  },
  long: {
    label: "LONG",
    caption: "길게, 흐름에 집중하기",
    summary: "흐름을 살린 문단",
    description: ["문맥을 길게 이어서.", "전체 연구의 흐름에 집중하기 좋아요."],
  },
};

export function LengthSelector({ value, onChange }) {
  return (
    <div>
      <div
        className="grid h-12 grid-cols-3 gap-1.5 rounded-2xl bg-navy-faint p-1"
        role="radiogroup"
        aria-label="문단 길이"
      >
        {Object.entries(lengthOptions).map(([key, option]) => {
          const selected = value === key;
          return (
            <button
              aria-checked={selected}
              className={`font-medium-face rounded-[10px] px-3 py-2 text-[17px] leading-[1.252] transition ${
                selected
                  ? "bg-white text-ink shadow-[0_1px_1px_rgba(0,0,0,0.09)]"
                  : "text-subtle hover:text-ink"
              }`}
              key={key}
              onClick={() => onChange(key)}
              role="radio"
              type="button"
            >
              {option.label}
            </button>
          );
        })}
      </div>
      <div className="mt-2 grid grid-cols-3 text-center">
        {Object.entries(lengthOptions).map(([key, option]) => (
          <p
            className={`text-[13px] leading-[1.35] ${
              value === key ? "font-medium-face text-blue" : "font-regular-face text-muted"
            }`}
            key={key}
          >
            {option.caption}
          </p>
        ))}
      </div>
    </div>
  );
}
