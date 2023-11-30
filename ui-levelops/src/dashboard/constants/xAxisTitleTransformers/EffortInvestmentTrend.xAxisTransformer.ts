export const effortInvestmentXAxisTitleTransformer = (
  currentValue: string,
  payload: { [x: string]: any },
  dataKey: string
) => {
  return payload?.start_date ?? payload?.[dataKey] ?? currentValue ?? "";
};
