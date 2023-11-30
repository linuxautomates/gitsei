import { ScmCommitsMetricTypes, IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { get } from "lodash";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { convertEpochToDate, DEFAULT_DATE_FORMAT } from "utils/dateUtils";

export const getAcrossValue = (args: any) => {
  const filters = args?.allFilters;
  let across = filters?.across;
  if (["trend"].includes(across)) {
    const interval = get(filters, ["interval"]);
    if (interval) across = `${across}_${interval}`;
  }
  return across;
};

export const scmCommitsReportOnChartClickPayloadHelper = (params: any) => {
  const { data, across } = params;
  if (params?.chart_type === ChartType.CIRCLE) {
    if (across === "trend") {
      const key = !parseInt(data.key) ? data.key : convertEpochToDate(data.key, DEFAULT_DATE_FORMAT, true);
      return key;
    }
    const name = data?.name ?? data.key;
    return { name: name, id: data.key };
  }
  if (across === "trend") {
    const moreData = params?.data?.activePayload?.[0]?.payload;
    const key = !parseInt(moreData.key) ? moreData.key : convertEpochToDate(moreData.key, DEFAULT_DATE_FORMAT, true);
    return key;
  }
  const newData = params?.data?.activePayload?.[0]?.payload;
  const name = newData?.name ?? newData?.key;
  const id = newData?.key;
  return { name, id };
};

export const scmWidgetChartPropsHelper = (data: any) => {
  const { filters = {} } = data;
  const metric = get(filters, ["filter", "metric"], [ScmCommitsMetricTypes.NO_OF_COMMITS]);
  const visualization = get(filters, ["filter", "visualization"], IssueVisualizationTypes.AREA_CHART);
  const barProps = metric.map((met: string) => ({
    name: met,
    dataKey: met,
    unit: met
  }));
  switch (visualization) {
    case IssueVisualizationTypes.STACKED_AREA_CHART:
      return {
        barProps,
        stackedArea: true
      };
    case IssueVisualizationTypes.STACKED_BAR_CHART:
      return {
        barProps,
        stacked: true
      };
    default:
      return { barProps };
  }
};
