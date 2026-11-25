import { useState } from "react";
import { Footer } from "./components/Footer";
import { Header } from "./components/Header";
import { Hero } from "./components/Hero";
import { PreviewPanel } from "./components/PreviewPanel";
import { UploadPanel } from "./components/UploadPanel";

export default function App() {
  const [length, setLength] = useState("medium");

  return (
    <div className="min-h-screen bg-canvas">
      <Header />
      <main id="translate">
        <Hero />
        <div className="mx-auto grid w-full max-w-[1200px] grid-cols-1 gap-7 px-6 lg:grid-cols-[minmax(0,740px)_minmax(320px,432px)] xl:px-0">
          <UploadPanel length={length} onLengthChange={setLength} />
          <PreviewPanel length={length} />
        </div>
      </main>
      <Footer />
    </div>
  );
}
