const assetPath = "/assets";

export function BrandIcon() {
  return (
    <span aria-hidden="true" className="relative block size-6 overflow-hidden">
      <img alt="" className="absolute left-[57.07%] top-[36.58%]" src={`${assetPath}/68526.svg`} />
      <img alt="" className="absolute left-[28.59%] top-[16.83%]" src={`${assetPath}/68fe1.svg`} />
      <span
        className="absolute left-[4.39%] top-[37.71%] h-[11.001px] w-[9.215px]"
        style={{
          maskImage: `url("${assetPath}/84405.svg")`,
          maskRepeat: "no-repeat",
          maskSize: "9.215px 11.001px",
        }}
      >
        <img alt="" src={`${assetPath}/98990.svg`} />
      </span>
    </span>
  );
}
