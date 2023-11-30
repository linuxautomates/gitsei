import { lineChartColors } from "./chart-themes";

const sumOfAsciiCharacters = (str: string) => {
  return str
    .split("")
    .map((ch: string) => ch.charCodeAt(0))
    .reduce((acc: number, item: number) => {
      acc += item;
      return acc;
    }, 0);
};

const getColorFromString = (str: string, excludeColors?: string[]) => {
  const subStr1 = str.substring(0, str.length / 2);
  const subStr2 = str.substring(str.length / 2);
  const sumStr1 = subStr1.length ? Math.floor(sumOfAsciiCharacters(subStr1) / subStr1.length - 1) : 0;
  const sumStr2 = subStr2.length ? sumOfAsciiCharacters(subStr2) : 0;
  const total = sumStr1 + sumStr2 + str.charCodeAt(0) + str.charCodeAt(str.length - 1);
  let colors = lineChartColors.map((color: string) => color.toLowerCase());
  // JUST IN CASE WE HAVE SELECTED ALL THE COLORS IN THE COLOR SCHEME SETTING
  if (excludeColors?.length && excludeColors.length !== lineChartColors.length) {
    const mappedExclude = excludeColors.map((color: string) => color.toLowerCase());
    colors = colors.filter((color: string) => !mappedExclude.includes(color));
  }
  const index = total % (colors.length - 1);
  return colors[index];
};

export const getDynamicColor = (
  key: string | number,
  index: number,
  colorSchema?: Record<string, string>,
  callback?: (args: any) => string
) => {
  const keyToCheck = typeof key === "number" ? key.toString().toLowerCase() : key.toLowerCase();
  let excludeColors: string[] = [];
  if (colorSchema) {
    excludeColors = Object.values(colorSchema);
    const allKeys = Object.keys(colorSchema);
    if (allKeys.includes(keyToCheck)) {
      return colorSchema[keyToCheck];
    }
    if (!callback) {
      return getColorFromString(keyToCheck);
    }
    return callback?.({ index, key });
  }
  if (!callback) {
    return getColorFromString(keyToCheck);
  }
  return callback?.({ index, key });
};
