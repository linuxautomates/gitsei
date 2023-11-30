import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";

export const scmDoraDeploymentFrequencySingleStatTransformer = (data: any, key: string) => {
  const { apiData, reportType } = data;
  const unit = get(widgetConstants, [reportType, "chart_props", "unit"], "");
  const stat = apiData?.[0]?.[key] ?? 0;
  return {
    stat,
    unit,
    ...(apiData?.[0] || {})
  };
};
