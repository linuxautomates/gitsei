export const getTextWidth = (text: string, offset: number = 1) => {
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");
  if (context) {
    context.font = "17.5px Inter";
  }
  return Math.ceil((context?.measureText(text)?.width || 0) + (offset + offset - 2) * 16) as number;
};

export const getSubLength = (width: number, text: string) => {
  let i = 0;
  let j = text?.length - 1;
  while (i < j) {
    const substring = text?.slice(0, i ?? 0);
    const widthOfSubString = getTextWidth(substring, substring.split("\\").length);
    if (widthOfSubString < width) {
      i++;
    } else {
      break;
    }
  }
  return i;
};
