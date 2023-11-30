import { get } from "lodash";
import moment from "moment";
import { getPercantageValue } from "shared-resources/charts/graph-stat-chart/helper";
import { EICategoryTypes } from "dashboard/dashboard-types/BAReports.types";
import { getUnixStartofDay } from "utils/dateUtils";

export const getStatANDCompareStat = (data: any) => {
  const { widgetFilters, apiData } = data;
  const timePeriod = get(widgetFilters, ["filter", "time_period"], 30);

  const now = moment().utc().startOf("day").unix();
  const prev = getUnixStartofDay(now - timePeriod * 86400);
  const comparePrev = getUnixStartofDay(prev - timePeriod * 86400);

  const statData = (apiData || []).length
    ? apiData.filter((record: { key: string }) => parseInt(record.key) >= prev && parseInt(record.key) <= now)
    : [];

  const compareStatData = (apiData || []).length
    ? apiData.filter((record: { key: string }) => parseInt(record.key) >= comparePrev && parseInt(record.key) < prev)
    : [];

  return { statData, compareStatData };
};

export const aggregatedOutput = (data: any[], dataKey: string = "total_tickets") => {
  return data.length
    ? data.reduce(
        (
          acc: { total: number; average: number },
          record: {
            [x: string]: number;
          },
          index: number
        ) => {
          let total;
          total = Math.max(acc.total, record[dataKey]);
          if (dataKey === "total_tickets") {
            total = Math.max(acc.total, record?.total_tickets || 0);
          } else {
            total = acc.total + record?.[dataKey] || 0;
          }
          let avg = total / (index + 1);
          return { total: total, average: avg };
        },
        { total: 0, average: 0 }
      )
    : { total: 0, average: 0 };
};

export const graphStatReportTransformer = (
  filters: any,
  totalRecords: any[],
  totalCompletedRecords: any[],
  dataKey?: "total_tickets" | "total_story_points"
) => {
  const timePeriod = get(filters, ["filter", "time_period"], 30);
  const aggType = get(filters, ["filter", "agg_type"], "total");

  const now = moment().unix();
  const prev = now - timePeriod * 86400;
  const totalStatData =
    totalRecords && totalRecords.length
      ? totalRecords.filter((record: { key: string }) => {
          return parseInt(record.key) >= prev && parseInt(record.key) <= now;
        })
      : [];

  const totalCompletedStatData =
    totalCompletedRecords && totalCompletedRecords.length
      ? totalCompletedRecords.filter((record: { key: string }) => {
          return parseInt(record?.key) >= prev && parseInt(record?.key) <= now;
        })
      : [];
  const numeratorData = Math.floor((aggregatedOutput(totalCompletedStatData, dataKey) as any)[aggType]);
  const denominatorData = Math.floor((aggregatedOutput(totalStatData, dataKey) as any)[aggType]);

  return {
    epic: { numStat: numeratorData, denStat: denominatorData },
    unit: dataKey === "total_tickets" ? "Tickets" : "Points"
  };
};

export const newEffortInvestmentSingleStatTransformer = (
  curRangeData: Array<{ key: string; fte: number }>,
  beforeRangeData: Array<{ key: string; fte: number }>,
  categories: Array<EICategoryTypes>
) => {
  const totalCurRangeData = curRangeData.reduce((sum, data) => {
    sum += data?.fte ?? 0;
    return sum;
  }, 0);

  const totalBeforeRangeData = beforeRangeData.reduce((sum, data) => {
    sum += data?.fte ?? 0;
    return sum;
  }, 0);

  let categoryData = (categories ?? []).map((category: EICategoryTypes) => {
    const curCategoryRecord = (curRangeData ?? []).find(record => record?.key === category?.name);
    const beforeCategoryRecord = (beforeRangeData ?? []).find(record => record?.key === category?.name);

    let fte_cur = 0,
      fte_before = 0;

    if (totalBeforeRangeData !== 0) {
      fte_before = Math.round(((beforeCategoryRecord ? beforeCategoryRecord?.fte : 0) / totalBeforeRangeData) * 100);
    }

    if (totalCurRangeData !== 0) {
      fte_cur = Math.round(((curCategoryRecord ? curCategoryRecord?.fte : 0) / totalCurRangeData) * 100);
    }

    return {
      name: category?.name,
      id: category?.id,
      ideal_range: category?.goals?.ideal_range,
      stat: fte_cur,
      statTrend: fte_cur - fte_before
    };
  });

  return categoryData ?? [];
};
