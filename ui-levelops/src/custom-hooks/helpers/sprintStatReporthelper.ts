import { get, isArray, isNaN } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import {
  IDEAL_FILTER_MAX,
  IDEAL_FILTER_MIN,
  IDEAL_RANGE_FILTER_KEY,
  metricSTDMapping,
  sprintMetricUnitKeys,
  sprintReportDataKeyTypes,
  spritnMetricKeyTypes,
  statSprintMetricsOptions
} from "dashboard/graph-filters/components/sprintFilters.constant";

const transformStatDataForSprintMetric = (data: number, metric: string) => {
  if (metric === sprintReportDataKeyTypes.AVG_CHURN_RATE) {
    return Number(data.toFixed(2));
  } else if (data >= 1 && metric !== spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT) {
    return Math.trunc(Math.round(data));
  } else {
    return Math.round(data * 10) / 10;
  }
};

const getStandardDeviation = (metricsArray: number[], usePopulation = false) => {
  if (metricsArray.length <= 1) {
    return 0;
  }
  const mean = metricsArray.reduce((acc: number, val: number) => acc + val, 0) / metricsArray.length;
  return Math.sqrt(
    metricsArray
      .reduce((acc: number[], val: number) => acc.concat((val - mean) ** 2), [])
      .reduce((acc: number, val: number) => acc + val, 0) /
      (metricsArray.length - (usePopulation ? 0 : 1))
  );
};

export const sprintStatCalculateBasedOnMetric = (
  metric: string,
  num: number,
  record: any,
  count = 0,
  viewBy = "Points"
): { num: number; count: number } => {
  let val1: any = 0,
    val2: any = 0,
    val3: any = 0,
    val4: any = 0;
  const isViewByPoints = viewBy === "Points";
  switch (metric) {
    case "avg_creep":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.CREEP_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 > 0 && val2 > 0) {
        num = val1 / val2;
        count++;
      }
      return { num, count };
    case "avg_commit_to_done":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 > 0 && val2 > 0) {
        num = val1 / val2;
        count++;
      }
      return { num, count };
    case "avg_creep_done_to_commit":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 > 0 && val2 > 0) {
        num = val1 / val2;
        count++;
      }
      return { num, count };
    case "avg_creep_to_done":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 > 0 && val2 > 0) {
        num = val1 / val2;
        count++;
      }
      return { num, count };
    case "avg_creep_to_miss":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 >= 0 && val2 > 0) {
        num = Math.max(0, 1 - val1 / val2);
        count++;
      }
      return { num, count };
    case "avg_commit_to_miss":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMIT_DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 >= 0 && val2 > 0) {
        num = Math.max(0, 1 - val1 / val2);
        count++;
      }
      return { num, count };
    case "commit_to_miss_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMIT_DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 >= 0 && val2 >= 0) {
        num = Math.max(0, val2 - val1);
        count++;
      }
      return { num, count };
    case "creep_to_miss_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 >= 0 && val2 >= 0) {
        num = Math.max(0, val2 - val1);
        count++;
      }
      return { num, count };
    case "commit_to_done":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMIT_DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 > 0 && val2 > 0) {
        num = val1 / val2;
        count++;
      }
      return { num, count };
    case "commit_to_done_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMIT_DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      if (val1 && val1 > 0) {
        num = val1;
        count++;
      }
      return { num, count };
    case "missed_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMIT_DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val3 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      val4 = isViewByPoints
        ? record[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 >= 0 && val2 >= 0 && val3 >= 0 && val4 >= 0) {
        num = Math.max(0, val3 - val1) + Math.max(0, val4 - val2);
        count++;
      }
      return { num, count };
    case "commit_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 && val1 > 0) {
        num = val1;
        count++;
      }
      return { num, count };
    case "velocity_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_KEYS];
      if (val1 && val1 > 0) {
        num = val1;
        count++;
      }
      return { num, count };
    case "creep_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 && val1 > 0) {
        num = val1;
        count++;
      }
      return { num, count };
    case "creep_done_points":
      val1 = isViewByPoints
        ? record[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
        : record?.[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      if (val1 && val1 > 0) {
        num = val1;
        count++;
      }
      return { num, count };
    case "creep_tickets":
      val1 = record[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 && val1.length > 0) {
        num = val1.length;
        count++;
      }
      return { num, count };
    case "creep_done_tickets":
      val1 = record[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      if (val1 && val1.length > 0) {
        num = val1.length;
        count++;
      }
      return { num, count };
    case "creep_missed_tickets":
      val1 = record[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val2 = record[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 && val2) {
        num = Math.max(0, val2.length - val1.length);
        count++;
      }
      return { num, count };
    case "commit_tickets":
      val1 = record[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 && val1.length > 0) {
        num = val1.length;
        count++;
      }
      return { num, count };
    case "commit_done_tickets":
      val1 = record[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      if (val1 && val1.length > 0) {
        num = val1.length;
        count++;
      }
      return { num, count };
    case "commit_missed_tickets":
      val1 = record[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = record[sprintReportDataKeyTypes.COMMITTED_KEYS];
      if (val1 && val2) {
        num = Math.max(0, val2.length - val1.length);
        count++;
      }
      return { num, count };
    case "done_tickets":
      val1 = record[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = record[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      if (val1 && val2) {
        num = val1.length + val2.length;
        count++;
      }
      return { num, count };
    case "missed_tickets":
      val1 = record[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS];
      val2 = record[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS];
      val3 = record[sprintReportDataKeyTypes.COMMITTED_KEYS];
      val4 = record[sprintReportDataKeyTypes.CREEP_KEYS];
      if (val1 && val2 && val3 && val4) {
        num = Math.max(0, val3.length - val1.length) + Math.max(0, val4.length - val2.length);
        count++;
      }
      return { num, count };
    case spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT:
      const storyPointsByIssues: { before: number; after: number }[] = Object.values(
        record[sprintReportDataKeyTypes.STORY_POINTS_BY_ISSUE] ||
          record[sprintReportDataKeyTypes.STORY_POINTS_BY_WORKITEM] ||
          {}
      );
      const totalIssues: number =
        record[sprintReportDataKeyTypes.TOTAL_ISSUES] || record[sprintReportDataKeyTypes.TOTAL_WORKITEMS] || 0;
      if (totalIssues) {
        const totalStoryPointsByIssues = (storyPointsByIssues || []).reduce((sum, storyPoints) => {
          sum += get(storyPoints, ["after"], 0);
          return sum;
        }, 0);
        num = totalStoryPointsByIssues / totalIssues;
        count++;
      }
      return { num, count };
    case sprintReportDataKeyTypes.AVG_CHURN_RATE:
      num = (record[sprintReportDataKeyTypes.AVG_CHURN_RATE] || 0) * 100;
      return { num, count: 1 };
    default:
      return { num, count };
  }
};

const getSprintDataBasedOnMetric = (statData: any[], metric: string) => {
  const aggType = "average";
  const reducedData = statData.reduce(
    (acc: { total: number; average: number; count: number }, record: any) => {
      const { num, count } = sprintStatCalculateBasedOnMetric(metric, 0, record, acc.count);
      const total = acc.total + num;
      const avg = count ? total / count : 0;
      return { total: total, average: avg, count };
    },
    { total: 0, average: 0, count: 0 }
  );
  let aggregatedResult = reducedData[aggType];

  if (!(metric?.includes("points") || metric?.includes("tickets") || metric?.includes("average"))) {
    aggregatedResult *= 100;
  }
  return aggregatedResult;
};

const getSprintMetricSTDData = (statData: any[], metric: string) => {
  const reducedData = statData.reduce((acc: number[], record: any) => {
    let { num } = sprintStatCalculateBasedOnMetric(metricSTDMapping[metric], 0, record);
    if (metric !== "velocity_points_std") {
      num *= 100;
    }
    return [...acc, num];
  }, []);
  return getStandardDeviation(reducedData);
};

const getSprintSingleStatUnit = (metric: string, extraUnits: string, stat: number) => {
  const metricOption = statSprintMetricsOptions.filter(option => option?.value === metric);
  if (metricOption.length > 0) {
    if (metric === sprintReportDataKeyTypes.AVG_CHURN_RATE) return extraUnits;
    if (!(metric?.includes("points") || metric?.includes("tickets") || metric?.includes("average"))) return extraUnits;
    return metric?.includes("points")
      ? stat > 1
        ? "Points"
        : "Point"
      : metric?.includes("tickets")
      ? stat > 1
        ? "Tickets"
        : "Ticket"
      : "Story Points";
  }
  return extraUnits;
};

export const sprintMetricStatColumnSorterComparater = (sortingKey: string) => {
  return (sprintObj1: any, sprintObj2: any) => {
    const val1 = sprintObj1?.[sortingKey];
    const val2 = sprintObj2?.[sortingKey];
    if ((sprintMetricUnitKeys || []).includes(sortingKey as sprintReportDataKeyTypes)) {
      if ((sortingKey || "").includes("keys")) {
        return (val1?.length || 0) - (val2?.length || 0);
      }
      return (eval(val1) || 0) - (eval(val2) || 0); // Adding `eval` as for churn rate we need to sort based on ans of x/y.
    } else if (sortingKey === "key") {
      let parsedVal1 = parseInt(val1);
      let parsedVal2 = parseInt(val2);
      parsedVal1 = isNaN(parsedVal1) ? 0 : parsedVal1;
      parsedVal2 = isNaN(parsedVal2) ? 0 : parsedVal2;
      return parsedVal1 - parsedVal2;
    } else {
      const { num: calculatedValue1 } = sprintStatCalculateBasedOnMetric(sortingKey || "", 0, sprintObj1 || {}) || 0;
      const { num: calculatedValue2 } = sprintStatCalculateBasedOnMetric(sortingKey || "", 0, sprintObj2 || {}) || 0;
      return calculatedValue1 - calculatedValue2;
    }
  };
};

export const sprintStatReportTransformer = (data: any) => {
  const { filters, apiData, reportType, widgetMetaData } = data;
  if (!apiData) return {};
  const unit = get(widgetConstants, [reportType, "chart_props", "unit"], "");
  let statData = apiData || [];
  const metric = get(filters, ["metric"], undefined);
  let stat = 0;
  // For churn rate, we get the average churn rate value from BE response, so skipping the calculation method.
  if (metric === sprintReportDataKeyTypes.AVG_CHURN_RATE) {
    stat = isArray(statData) && statData.length ? sprintStatCalculateBasedOnMetric(metric, 0, statData[0], 0).num : 0;
  } else if (!metric?.includes("std")) {
    stat = statData.length ? getSprintDataBasedOnMetric(statData, metric) : 0;
  } else {
    stat = statData.length ? getSprintMetricSTDData(statData, metric) : 0;
  }
  let idealRange = get(widgetMetaData, [IDEAL_RANGE_FILTER_KEY], {
    [IDEAL_FILTER_MIN]: Number.MIN_SAFE_INTEGER,
    [IDEAL_FILTER_MAX]: Number.MAX_SAFE_INTEGER
  });
  if (typeof idealRange[IDEAL_FILTER_MAX] === "string" || typeof idealRange[IDEAL_FILTER_MIN] === "string") {
    idealRange = {
      [IDEAL_FILTER_MIN]: Number.MIN_SAFE_INTEGER,
      [IDEAL_FILTER_MAX]: Number.MAX_SAFE_INTEGER
    };
  }
  return {
    stat: transformStatDataForSprintMetric(stat, metric),
    unit: getSprintSingleStatUnit(metric, unit, transformStatDataForSprintMetric(stat, metric)),
    idealRange,
    metric
  };
};
