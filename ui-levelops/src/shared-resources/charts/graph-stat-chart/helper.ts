export const RISK_FACTOR = 40;
export const MINIMUM_PERCENT = 0.3;

export enum colorType {
  FRACTION_COLOR = "#1a6bb6",
  RISK_COLOR = "#b62c1a",
  SUCCESS_COLOR = "#3f8600",
  NEUTRAL_COLOR = "#c4bfbf",
  GRAY = "#8A94A5"
}

export const getValueStyle = () => {
  return {
    fontSize: "1.5rem",
    display: "flex",
    justifyContent: "center",
    marginBottom: "8px"
  };
};

export const getSuffixValueStyle = (displayFormat: "percentage" | "fraction", color: any) => {
  return {
    color: displayFormat === "percentage" ? color : colorType.GRAY,
    fontSize: "1.2rem"
  };
};

export const getPrefixValueStyle = (color: any) => {
  return {
    fontSize: "46px",
    color
  };
};

export const getPercantageValue = (num: number, den: number) => {
  if (den === 0) return "NaN";
  return Math.floor((num / den) * 100);
};

export const getDecimalPercantageValue = (num: number, den: number) => {
  if (den === 0) return "NaN";
  return (num / den) * 100;
};

export const getDecimalPercantageRoundValue = (num: number, den: number, roundOfValue?: number) => {
  if (den === 0) return "NaN";
  if (roundOfValue) {
    return ((num / den) * 100)?.toFixed(roundOfValue);
  }
  return (num / den) * 100;
};

export const getValueColor = (
  value: { numStat: number; denStat: number },
  displayFormat: "percentage" | "fraction"
) => {
  let resultantColor: colorType | "" = "";
  if (displayFormat === "fraction") {
    const numerator = value.numStat;
    const denomenator = value.denStat;
    const minimumPercentOfTotal = Math.floor(denomenator * MINIMUM_PERCENT);
    if (numerator >= minimumPercentOfTotal) {
      resultantColor = colorType.FRACTION_COLOR;
    } else {
      resultantColor = colorType.RISK_COLOR;
    }
  } else {
    const percent = getPercantageValue(value.numStat, value.denStat);
    if (percent >= RISK_FACTOR) {
      resultantColor = colorType.SUCCESS_COLOR;
    } else {
      resultantColor = colorType.RISK_COLOR;
    }
  }
  return resultantColor;
};

export const areaChartMapping: any = {
  [colorType.RISK_COLOR]: "#FEF1F2",
  [colorType.FRACTION_COLOR]: "#F0F0F0"
};
