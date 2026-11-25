import { useRef, useState } from "react";
import { LengthSelector } from "./LengthSelector";

const MAX_FILE_SIZE = 20 * 1024 * 1024;

export function UploadPanel({ length, onLengthChange }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const acceptFile = (nextFile) => {
    setSubmitted(false);
    if (!nextFile) return;
    if (nextFile.type !== "application/pdf" && !nextFile.name.toLowerCase().endsWith(".pdf")) {
      setFile(null);
      setError("PDF 파일만 업로드할 수 있어요.");
      return;
    }
    if (nextFile.size > MAX_FILE_SIZE) {
      setFile(null);
      setError("파일 크기는 최대 20MB까지 가능해요.");
      return;
    }
    setError("");
    setFile(nextFile);
  };

  return (
    <section className="rounded-[20px] border border-line bg-white p-[31px]">
      <div className="flex items-baseline gap-4">
        <span className="font-semibold-face text-[17px] leading-[1.252] text-blue">01</span>
        <h2 className="font-bold-face text-xl leading-[1.35] text-ink">논문 업로드</h2>
      </div>
      <p className="font-regular-face mt-1.5 text-[15px] leading-[1.35] text-muted">
        번역할 영문 IT 논문을 추가해주세요.
      </p>

      <div
        className={`mt-[21px] flex h-60 flex-col items-center justify-center rounded-[18px] border border-dashed transition ${
          isDragging ? "border-blue bg-[#f7faff]" : "border-line bg-white"
        }`}
        onDragEnter={(event) => {
          event.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={(event) => {
          event.preventDefault();
          setIsDragging(false);
        }}
        onDragOver={(event) => event.preventDefault()}
        onDrop={(event) => {
          event.preventDefault();
          setIsDragging(false);
          acceptFile(event.dataTransfer.files[0]);
        }}
      >
        <span className="flex size-14 items-center justify-center rounded-[18px] bg-blue-soft">
          <img alt="" height="24" src="/assets/1e36b.svg" width="24" />
        </span>
        <p className="font-semibold-face mt-4 max-w-[90%] truncate text-[17px] leading-[1.252] text-ink">
          {file ? file.name : "여기에 논문 파일을 끌어다 놓으세요"}
        </p>
        <input
          accept=".pdf,application/pdf"
          className="sr-only"
          onChange={(event) => acceptFile(event.target.files[0])}
          ref={inputRef}
          type="file"
        />
        <button
          className="font-semibold-face mt-3 min-h-12 min-w-40 rounded-[14px] bg-blue-soft px-4 text-[17px] leading-[1.252] text-blue transition hover:bg-[#c9defb]"
          onClick={() => inputRef.current?.click()}
          type="button"
        >
          {file ? "파일 변경하기" : "파일 선택하기"}
        </button>
        <p
          className={`font-regular-face mt-3 text-[13px] leading-[1.35] ${error ? "text-red-600" : "text-muted"}`}
        >
          {error || "PDF · 최대 20MB · 파일 1개"}
        </p>
      </div>

      <div className="mt-[23px] flex items-baseline gap-4">
        <span className="font-semibold-face text-[17px] leading-[1.252] text-blue">02</span>
        <h2 className="font-bold-face text-xl leading-[1.35] text-ink">문단 길이</h2>
      </div>
      <p className="font-regular-face mt-1.5 text-[15px] leading-[1.35] text-muted">
        내용은 그대로, 읽기 편한 길이로 문단을 나눠요.
      </p>

      <div className="mt-[17px]">
        <LengthSelector onChange={onLengthChange} value={length} />
      </div>

      <button
        className="font-semibold-face mt-[30px] min-h-12 w-full rounded-[14px] bg-blue px-4 text-[17px] leading-[1.252] text-white transition enabled:hover:bg-[#1b64d8] disabled:cursor-not-allowed disabled:opacity-30"
        disabled={!file}
        onClick={() => setSubmitted(true)}
        type="button"
      >
        한국어로 번역하기
      </button>
      <p
        className="font-regular-face mt-4 text-center text-[13px] leading-[1.35] text-muted"
        aria-live="polite"
      >
        {submitted
          ? "번역 요청이 준비되었어요."
          : file
            ? "파일이 준비되었어요. 번역을 시작해보세요."
            : "논문 파일을 업로드하면 번역을 시작할 수 있어요."}
      </p>
    </section>
  );
}
