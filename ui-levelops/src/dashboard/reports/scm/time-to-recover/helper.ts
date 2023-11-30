import { getTimeAndUnit } from "custom-hooks/helpers/statReport.helper";

export const scmDoraTimeToRecoverSingleStatTransformer = (data: any, key: string) => {
  const { apiData, reportType } = data;
  const { time: statTime, unit: statUnit } = getTimeAndUnit(apiData?.[0]?.[key] ?? 0, reportType);
  return {
    stat: statTime,
    unit: statUnit,
    ...(apiData?.[0] || {})
  };
};
