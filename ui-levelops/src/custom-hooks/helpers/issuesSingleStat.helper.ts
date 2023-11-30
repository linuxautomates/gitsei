import { statTrendTimeAndUnit, getTimeAndUnit, statReportTransformer } from "./statReport.helper";
import { ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import { get, cloneDeep } from "lodash";
import moment from "moment";
import { getValueFromTimeRange, rangeMap } from "dashboard/graph-filters/components/helper";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import widgetConstants from "dashboard/constants/widgetConstants";
import { isValidRelativeTime, getDashboardTimeRange } from "./statHelperFunctions";
import { COMPARE_FIELDS } from "./constants";
import { AZURE_METRIC } from "dashboard/reports/azure/issues-single-stat/constant";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";

const defaultTime = {
  last: { unit: "days", num: 30 },
  next: { unit: "today" }
};

const getFirstAndSecondHalf = (metaData: any, filterKey: string, dashMeta?: any) => {
  const metaKey = get(rangeMap, filterKey, filterKey);

  let relativeTime = cloneDeep(metaData?.[RANGE_FILTER_CHOICE]?.[metaKey] || {});

  const useDashboardTime = metaData?.dashBoard_time_keys?.[filterKey]?.use_dashboard_time || false;

  if (dashMeta && isDashboardTimerangeEnabled(dashMeta) && useDashboardTime) {
    return {
      firstHalf: getDashboardTimeRange(dashMeta, false),
      secondHalf: getDashboardTimeRange(dashMeta, false, true)
    };
  }

  // if not time_period, using 30 days as default
  if (!isValidRelativeTime(relativeTime?.relative)) {
    relativeTime = {
      type: "relative",
      relative: defaultTime
    };
  }

  const timeUnit = relativeTime?.relative?.last?.unit;
  const timeDiff = relativeTime?.relative?.last?.num;

  const firstHalf = getValueFromTimeRange(relativeTime?.relative || {});

  const timeGap =
    parseInt(relativeTime?.relative?.last?.num || "0") + parseInt(relativeTime?.relative?.next?.num || "0");

  const secondHalf = {
    $gt: moment
      .utc()
      .subtract((parseInt(timeDiff || "0") + timeGap - 1) as any, timeUnit)
      .startOf(timeUnit as any)
      .unix(),
    $lt: moment
      .utc()
      .subtract(timeDiff as any, timeUnit)
      .endOf(timeUnit as any)
      .unix()
  };

  return { firstHalf, secondHalf };
};

export const modifiedStatReportTransformer = (
  data: any,
  compareField: string,
  firstHalf: any,
  secondHalf: any,
  aggType = "total"
) => {
  const { apiData, reportType } = data;

  if (!apiData || (!!apiData && !apiData.length) || !firstHalf || !secondHalf) {
    return { stat: 0 };
  }

  let stat: { average: number; total: number; median?: number; mean?: number };
  let compareStat: { average: number; total: number; median?: number };

  let newData = apiData;

  const statData = newData.filter(
    (record: { key: string }) =>
      parseFloat(record.key) >= parseFloat(firstHalf?.$gt || 0) &&
      parseFloat(record.key) <= parseFloat(firstHalf?.$lt || 0)
  );

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

          if (compareField === COMPARE_FIELDS.SUM) {
            count += record.count;
          }

          if (compareField === COMPARE_FIELDS.MEDIAN) {
            median = record.median + acc.median;
          }

          let total;
          total = acc.total + num;

          let avg = total / (index + 1);

          return { total: total, average: avg, count: count, median: median, mean: mean };
        },
        { total: 0, average: 0, count: 0, median: 0, mean: 0 }
      )
    : { total: 0, average: 0, count: 0, median: 0, mean: 0 };

  const compareStatData = newData.filter(
    (record: { key: string }) =>
      parseFloat(record.key) >= parseFloat(secondHalf?.$gt || 0) &&
      parseFloat(record.key) <= parseFloat(secondHalf?.$lt || 0)
  );

  compareStat =
    compareStatData && compareStatData.length
      ? compareStatData.reduce(
          (
            acc: { total: any; average: any; count: any; median: any },
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
            let unit;

            if (compareField === COMPARE_FIELDS.SUM) {
              count += record.count;
            }

            if (compareField === COMPARE_FIELDS.MEDIAN) {
              median = record.median + acc.median;
            }

            const total = (acc.total || 0) + num;

            let avg = total / (index + 1);

            return { total: total, average: avg, count: count, unit, median: median };
          },
          { total: undefined, average: undefined, count: undefined, median: undefined }
        )
      : { total: undefined, average: undefined, count: undefined, median: undefined };
  const hasStatUnit = get(widgetConstants, [reportType, "hasStatUnit"], undefined);

  if (hasStatUnit && hasStatUnit(compareField)) {
    let statTrend: number | undefined = undefined;

    const { time: statTime, unit: statUnit } = getTimeAndUnit((stat as any)[aggType], reportType);

    if ((compareStat as any)[aggType]) {
      statTrend = statTrendTimeAndUnit((stat as any)[aggType] - (compareStat as any)[aggType], statUnit);
    }

    return {
      stat: statTime,
      unit: statUnit,
      statTrend
    };
  } else {
    let statValue = (stat as any)[aggType];
    return {
      stat: statValue,
      statTrend:
        (compareStat as any)[aggType] !== undefined ? (stat as any)[aggType] - (compareStat as any)[aggType] : undefined
    };
  }
};

export const issuesSingleStatReportTransformer = (data: any) => {
  const { filters, metadata, dashMeta } = data;
  const key = `${filters.across}_at`;

  const timeRange = getFirstAndSecondHalf(metadata, key, dashMeta);

  const compareField = get(metadata, "metrics", "total_tickets");

  return modifiedStatReportTransformer(data, compareField, timeRange.firstHalf, timeRange.secondHalf);
};

export const azureIssuesSingleStatReportTransformer = (data: any) => {
  const { filters, metadata, reportType, apiData, dashMeta } = data;

  const unit = AZURE_METRIC[filters?.metric || ""]?.unit || AZURE_METRIC?.ticket?.unit;
  if (!apiData || !apiData.length) {
    return { stat: 0, unit };
  }

  const key = filters.across;

  const timeRange = getFirstAndSecondHalf(metadata, key, dashMeta);
  const compareField = filters?.metric ? AZURE_METRIC[filters?.metric || ""]?.key : AZURE_METRIC?.ticket?.key;
  const allData = modifiedStatReportTransformer(data, compareField, timeRange.firstHalf, timeRange.secondHalf);
  allData.unit = unit;
  return allData;
};

export const jiraIssueResolutionTimeReportStatTransformer = (data: any) => {
  const { filters, metadata, reportType, dashMeta } = data;

  const key = `${filters.across}_at` || "issue_created_at";

  const timeRange = getFirstAndSecondHalf(metadata, key, dashMeta);

  const compareField = get(widgetConstants, [reportType, "compareField"], "median");

  const aggType = filters.agg_type;

  return modifiedStatReportTransformer(data, compareField, timeRange.firstHalf, timeRange.secondHalf, aggType);
};

export const azureIssueResolutionTimeReportStatTransformer = (data: any) => {
  const { filters, metadata, reportType, dashMeta } = data;

  const key = filters?.across || "workitem_created_at";

  const timeRange = getFirstAndSecondHalf(metadata, key, dashMeta);

  const compareField = get(widgetConstants, [reportType, "compareField"], "median");

  const aggType = filters.agg_type;

  const newData = {
    ...data,
    apiData: get(data, ["apiData", "0", key, "records"], [])
  };

  return modifiedStatReportTransformer(newData, compareField, timeRange.firstHalf, timeRange.secondHalf, aggType);
};

export const sCMCommitToCICDJobLeadTimeSingleStatReportTransformerWrapper = (data: any) => {
  const { filters, metadata, reportType, dashMeta } = data;

  const key = "start_time";

  const timeRange = getFirstAndSecondHalf(metadata, key, dashMeta);

  let compareField = get(widgetConstants, [reportType, "compareField"], "median");

  const aggType = filters.agg_type;

  if (aggType === "median") {
    compareField = "median";
  }

  return modifiedStatReportTransformer(data, compareField, timeRange.firstHalf, timeRange.secondHalf, aggType);
};
