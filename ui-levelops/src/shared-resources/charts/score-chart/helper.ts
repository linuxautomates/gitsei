export const scoreChartColor = ["#d93865", "#EFD091", "#7491D6", "#BEDD85"];

export const scoreColor = (score: number) => {
  let index = 0;
  if (score >= 80) {
    index = 3;
  } else if (score >= 60) {
    index = 2;
  } else if (score >= 30) {
    index = 1;
  }

  return scoreChartColor[index];
};
