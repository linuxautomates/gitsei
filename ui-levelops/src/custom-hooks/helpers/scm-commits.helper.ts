import { githubCommitsmetricsChartMapping } from "dashboard/constants/FE-BASED/github-commit.FEbased";
import { DEFAULT_MAX_RECORDS } from "dashboard/constants/constants";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants"
import { ScmCommitsMetricTypes } from "dashboard/constants/typeConstants";
import { forEach, get } from "lodash";
import { aggregatedOutput, getStatANDCompareStat } from "./graphStatHelper";
import { convertTimeData } from "utils/dateUtils";

const getAVGCommitSize = (item: any) => {
  const linesAdded = get(item, ScmCommitsMetricTypes.NO_OF_NEW_LINES, 0);
  const linesRemoved = get(item, ScmCommitsMetricTypes.NO_OF_LINES_REMOVED, 0);
  const linesChanged = get(item, ScmCommitsMetricTypes.NO_OF_LINES_CHANGED, 0);
  return parseFloat(((linesAdded + linesChanged + linesRemoved) / 3).toFixed(2));
};

export const scmCommitsReportTransformer = (data: any) => {
  const { records, reportType, widgetFilters } = data;
  let { apiData } = data;
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }
  const metrics = get(widgetFilters, ["filter", "metric"], [ScmCommitsMetricTypes.NO_OF_COMMITS]);
  let seriesData =
    apiData && apiData.length
      ? apiData.map((item: any) => {
          let name = item.additional_key ?? item.key;

          if (["trend"].includes(across)) {
            const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
            name = xAxisLabelTransform?.({ item, interval, across });
          }
          let result: any = {
            name: name || "",
            key: item.key
          };

          forEach(metrics, met => {
            if (met === ScmCommitsMetricTypes.AVERAGE_COMMIT_SIZE) {
              result[ScmCommitsMetricTypes.AVERAGE_COMMIT_SIZE] = getAVGCommitSize(item);
            } else {
              result[met] = item[met];
            }
          });

          return result;
        })
      : [];

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  seriesData = seriesData.slice(slice_start, slice_end);

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};

export const scmCommitsStatReportTransformer = (data: any) => {
  const { widgetFilters, apiData } = data;

  const mappedData = (apiData || []).map((item: any) => {
    return {
      ...item,
      [ScmCommitsMetricTypes.AVERAGE_COMMIT_SIZE]: getAVGCommitSize(item)
    };
  });

  const { statData, compareStatData } = getStatANDCompareStat({ ...data, apiData: mappedData });

  const metric = get(widgetFilters, ["filter", "metric"], ScmCommitsMetricTypes.NO_OF_COMMITS);
  const aggType = get(widgetFilters, ["filter", "agg_type"], "total");

  const stat = aggregatedOutput(statData, metric);
  const compareStat = aggregatedOutput(compareStatData, metric);

  return {
    stat: stat[aggType],
    unit: githubCommitsmetricsChartMapping[metric as ScmCommitsMetricTypes],
    statTrend: stat[aggType] - compareStat[aggType]
  };
};
