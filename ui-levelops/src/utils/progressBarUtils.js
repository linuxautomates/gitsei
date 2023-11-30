export const getProgressBarColor = (percent, active = true) => {
  if (!active) {
    return "#7f8fa4";
  }
  if (percent <= 30) {
    return "red";
  }
  if (percent > 30 && percent < 70) {
    return "blue";
  }
  return "#17D861";
};
