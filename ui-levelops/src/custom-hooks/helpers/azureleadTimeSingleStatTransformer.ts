import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import { getTimeAndUnit } from "./statReport.helper";

export const azureLeadTimeStatReportTransformer = (data: any) => {
  const { filters, apiData, reportType, metadata } = data;

  if (!apiData) return;

  const precisionValue = 1;
  let stat: { average: number; total: number; median?: number; mean?: number };

  let aggType = (filters || {}).agg_type || "total";

  let compareField = get(widgetConstants, [reportType, "compareField"], "count");
  compareField = get(metadata, "metrics", "mean");
  let newData = apiData;
  const statData =
    newData && newData.length
      ? newData.filter((record: { key: string }) => {
          // include all data for sprint metrics reports
          const hideStages = metadata?.hide_stages || [];
          if (hideStages.length) {
            return !hideStages.includes(record.key);
          }
          return true;
        })
      : [];

  stat = statData.length
    ? statData.reduce(
        (
          acc: { total: number; average: number; count: number; median: number; mean: number },
          record: {
            [x: string]: number;
            lines_added_count: any;
            lines_removed_count: any;
            files_changed_count: number;
          },
          index: number
        ) => {
          let num = record[compareField] || 0;
          let count = acc.count;
          let median = 0;
          let mean = 0;

          if (compareField === "median") {
            median = record.median + acc.median;
          }

          if (compareField === "mean") {
            mean = record.mean + acc.mean;
          }

          let total;
          total = acc.total + num;

          let avg = total / (index + 1);

          return { total: total, average: avg, count: count, median: median, mean: mean };
        },
        { total: 0, average: 0, count: 0, median: 0, mean: 0 }
      )
    : { total: 0, average: 0, count: 0, median: 0, mean: 0 };

  let statTrend: number | undefined = undefined;
  const { time: statTime, unit: statUnit } = getTimeAndUnit((stat as any)[aggType], reportType, precisionValue);
  let unit = statUnit;
  return {
    stat: statTime,
    unit: unit,
    statTrend
  };
};
