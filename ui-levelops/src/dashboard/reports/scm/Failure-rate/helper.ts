import { get } from "lodash";

export const scmDoraFailureRateSingleStatTransformer = (data: any) => {
  const { apiData } = data;
  const { count, failure_rate } = apiData?.[0] || 0;
  const realValue = count * (failure_rate / 100);
  return {
    stat: apiData?.[0]?.failure_rate ?? 0,
    unit: "Failure Rate",
    unitSymbol: "%",
    realValue: Math.round(realValue) || 0,
    ...(apiData?.[0] || {})
  };
};
