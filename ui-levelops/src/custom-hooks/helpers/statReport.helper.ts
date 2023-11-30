import { get } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import { statTimeAndUnitDatatype } from "dashboard/constants/helper";
import {
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  LEAD_TIME_REPORTS
} from "dashboard/constants/applications/names";
import { getDaysAndTimeWithUnit } from "utils/timeUtils";
import moment from "moment";

const GET_AVERAGE_WITH_COUNT = ["sum"];

// for modifying epoch on the basis of reports and other conditions
const modifyTimeAndUnitOnCondition = (epoch: number, data: statTimeAndUnitDatatype, reportType: string) => {
  if (JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT === reportType) {
    const time: number = Math.floor(epoch / 60);
    const extraTime: number = epoch % 60;
    data = {
      time,
      unit: time === 1 ? "minute" : "minutes",
      extraTime,
      extraUnit: extraTime === 0 || extraTime === 1 ? "second" : "seconds"
    };
  }
  return data;
};

const getTimePeriod = (filter: { $gt: string; $lt: string }) => {
  const gt = parseInt(filter?.$gt || "0");
  const lt = parseInt(filter?.$lt || "0");
  if (gt <= 0 || lt <= 0) {
    return 30;
  }
  return (lt - gt) / 86400;
};

export const getTimeAndUnit = (epoch: number, reportType: string, precisionValue: number = 0) => {
  const alwaysInDays = get(widgetConstants, [reportType, "show_in_days"], undefined);
  let data = getDaysAndTimeWithUnit(epoch, precisionValue, alwaysInDays) as statTimeAndUnitDatatype;
  return modifyTimeAndUnitOnCondition(epoch, data, reportType);
};

export const statTrendTimeAndUnit = (epoch: number, unit: string) => {
  if (unit === "seconds" || unit === "second") {
    return epoch;
  } else if (unit === "minutes" || unit === "minute") {
    return Math.round(epoch / 60);
  } else if (unit === "hours" || unit === "hours") {
    return Math.round(epoch / 3600);
  } else if (unit === "days" || unit === "day") {
    return Math.round(epoch / 86400);
  }
};

const convertCumulativeToAbs = (
  data: Array<any>,
  prev: number,
  now: number,
  comparePrev: number,
  compareField: string
) => {
  if (!data) {
    return [];
  }

  let newDataAbs = data
    .filter((record: { key: string }) => parseInt(record.key) >= prev && parseInt(record.key) <= now)
    .sort((a: any, b: any) => parseInt(a.key) - parseInt(b.key));

  if (newDataAbs.length) {
    newDataAbs[newDataAbs.length - 1][compareField] =
      newDataAbs[newDataAbs.length - 1][compareField] - newDataAbs[0][compareField];
    newDataAbs[0][compareField] = 0;
    for (let i = 1; i < newDataAbs.length - 1; i++) {
      newDataAbs[i][compareField] = 0;
    }
  }

  let newPrev = data
    .filter((record: { key: string }) => parseInt(record.key) >= comparePrev && parseInt(record.key) <= prev)
    .sort((a: any, b: any) => parseInt(a.key) - parseInt(b.key));

  if (newPrev.length) {
    newPrev[newPrev.length - 1][compareField] = newPrev[newPrev.length - 1][compareField] - newPrev[0][compareField];
    newPrev[0][compareField] = 0;
    for (let i = 1; i < newPrev.length - 1; i++) {
      newPrev[i][compareField] = 0;
    }
  }

  return [...newDataAbs, ...newPrev];
};

export const azureStatReportTransformerWrapper = (data: any) => {
  const { apiData, widgetFilters } = data;
  const { across } = widgetFilters;
  const newApiData = get(apiData, ["0", across, "records"], []);
  return statReportTransformer({ ...data, apiData: newApiData });
};

export const statReportTransformer = (data: any) => {
  const { filters, apiData, reportType, metadata } = data;

  if (!apiData) return;

  let stat: { average: number; total: number; median?: number; mean?: number };
  let compareStat: { average: number; total: number; median?: number };

  let timePeriod = (filters || {}).time_period || 30;

  let aggType = (filters || {}).agg_type || "total";

  let compareField = get(widgetConstants, [reportType, "compareField"], "count");

  if (reportType === LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT) {
    compareField = get(metadata, "metrics", "mean");
    if ((filters || {}).calculation === "ticket_velocity") {
      timePeriod = getTimePeriod((filters || {}).jira_issue_resolved_at);
    } else {
      timePeriod = getTimePeriod((filters || {}).pr_merged_at);
    }
  }

  const _unit = get(widgetConstants, [reportType, "chart_props", "unit"], "");

  const now = filters?.now_value || moment().unix();
  const prev = now - timePeriod * 86400;
  const comparePrev = prev - timePeriod * 86400;

  let newData = apiData;

  if (reportType === JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT) {
    if (aggType === "files_changed") {
      compareField = "files";
    } else {
      compareField = "lines";
    }
  }

  if ([JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT].includes(reportType as any)) {
    if (aggType === "median") {
      compareField = "median";
    }
  }

  if (reportType === JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT) {
    const metrics = get(filters, "metrics", "average_coding_day");
    compareField = metrics.includes("average_") ? "mean" : "median";
  }

  if (reportType === JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_SINGLE_STAT) {
    const metrics = get(filters, "metrics", "average_author_response_time");
    compareField = metrics.includes("average") ? "mean" : "median";
  }

  if (reportType === JENKINS_REPORTS.SCM_PRS_SINGLE_STAT) {
    const metrics = get(metadata, "metrics", "num_of_prs");
    compareField = metrics === "num_of_prs" ? "count" : "pct_filtered_prs";
  }

  if (
    [
      ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_SINGLE_STAT,
      JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_SINGLE_STAT
    ].includes(reportType as any)
  ) {
    compareField = get(filters, "metric", "mean");
  }

  const statData =
    newData && newData.length
      ? newData.filter((record: { key: string }) => {
          // include all data for sprint metrics reports
          if (reportType === LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT) {
            const hideStages = metadata?.hide_stages || [];
            if (hideStages.length) {
              return !hideStages.includes(record.key);
            }
            return true;
          } else if (reportType === JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT) {
            return true;
          } else {
            return parseInt(record.key) >= prev && parseInt(record.key) <= now;
          }
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
          if (compareField === "sum") {
            count += record.count;
          }
          if (compareField === "lines") {
            num = (record.total_lines_added || 0) + (record.total_lines_removed || 0);
            //+ (record.lines_changed_count || 0);
          }

          if (compareField === "files") {
            num = record.total_files_changed || 0;
          }

          if (compareField === "median") {
            median = record.median + acc.median;
          }

          if (compareField === "mean") {
            mean = record.mean + acc.mean;
          }

          let total;
          total = acc.total + num;

          let avg = total / (index + 1);
          if (GET_AVERAGE_WITH_COUNT.includes(compareField)) {
            avg = count ? total / count : 0;
          }

          return { total: total, average: avg, count: count, median: median, mean: mean };
        },
        { total: 0, average: 0, count: 0, median: 0, mean: 0 }
      )
    : { total: 0, average: 0, count: 0, median: 0, mean: 0 };

  let compareStatData = [];

  if (
    ![LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT, JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT].includes(
      reportType as any
    )
  ) {
    compareStatData =
      newData && newData.length
        ? newData.filter(
            (record: { key: string }) => parseInt(record.key) >= comparePrev && parseInt(record.key) <= prev
          )
        : [];
  }

  // compareStat will be not called for jira sprint reports
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
            if (compareField === "sum") {
              count += record.count;
            }

            if (compareField === "lines") {
              num = (record.lines_added_count || 0) + (record.lines_removed_count || 0);
              //+ (record.lines_changed_count || 0);
              unit = "lines";
            }
            if (compareField === "files") {
              num = record.files_changed_count || 0;
              unit = "files";
            }

            if (compareField === "median") {
              median = record.median + (acc.median || 0);
            }

            const total = (acc.total || 0) + num;
            let avg = total / (index + 1);
            if (compareField === "sum") {
              avg = total / count;
            }

            return { total: total, average: avg, count: count, unit, median: median };
          },
          { total: undefined, average: undefined, count: undefined, median: undefined }
        )
      : { total: undefined, average: undefined, count: undefined, median: undefined };

  if ([JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT].includes(reportType as any)) {
    let unit = "lines";
    if (aggType === "files_changed") {
      unit = "files";
    }
    return {
      stat: (stat as any)["total"],
      unit: unit,
      statTrend:
        (compareStat as any)["total"] !== undefined ? (stat as any)["total"] - (compareStat as any)["total"] : undefined
    };
  }

  if (
    [
      ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_SINGLE_STAT,
      JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_SINGLE_STAT
    ].includes(reportType as any)
  ) {
    let statValue = (stat as any)[aggType];
    return {
      stat: statValue,
      unit: compareField === "total_tickets" ? "Tickets" : "Times",
      statTrend:
        (compareStat as any)[aggType] !== undefined ? (stat as any)[aggType] - (compareStat as any)[aggType] : undefined
    };
  }

  if (
    ["median", "sum"].includes(compareField) ||
    [
      ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_SINGLE_STAT_REPORT,
      "response_time_counts_stat",
      "resolution_time_counts_stat",
      ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_SINGLE_STAT_REPORT,
      LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
      JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT
    ].includes(reportType)
  ) {
    let statTrend: number | undefined = undefined;
    let precisionValue = 0;
    switch (reportType) {
      case LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT:
        precisionValue = 1;
        break;
      case JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT:
        precisionValue = 2;
        break;

      default:
        precisionValue = 0;
        break;
    }
    const { time: statTime, unit: statUnit } = getTimeAndUnit((stat as any)[aggType], reportType, precisionValue)

    if ((compareStat as any)[aggType]) {
      statTrend = statTrendTimeAndUnit((stat as any)[aggType] - (compareStat as any)[aggType], statUnit);
    }
    let unit = statUnit;
    if (["bounce_counts_stat", "azure_bounce_counts_stat"].includes(reportType)) {
      unit = _unit;
    }
    if (["hops_counts_stat", "azure_hops_counts_stat"].includes(reportType)) {
      unit = _unit;
    }
    return {
      stat: statTime,
      unit: unit,
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
