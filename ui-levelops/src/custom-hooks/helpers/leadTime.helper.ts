import { AcceptanceTimeUnit } from "classes/RestVelocityConfigs";
import {
  azureStageTimeColumn,
  leadTimeStageByTimeColumn,
  stageTimeColumn,
  timeSpentInStageColumnConfig
} from "dashboard/pages/dashboard-tickets/configs/leadTimeTableConfig";
import { getDaysAndTimeWithUnit } from "utils/timeUtils";
import { ColumnProps } from "antd/lib/table";
import { numberSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { SINLGE_STATE } from "shared-resources/charts/constant";

export enum VelocityRating {
  GOOD = "good",
  ACCEPTABLE = "acceptable",
  SLOW = "slow"
}
const DEFAULT_PRECISION_VALUE = 1;
export const velocityIndicators = [
  {
    title: "Good",
    hint: "(< 3d)",
    color: "#61BA14"
  },
  {
    title: "Acceptable",
    hint: "(3d-9d)",
    color: "#789FE9"
  },
  {
    title: "Slow",
    hint: "(> 9d)",
    color: "#CF1322"
  }
];

export const legendColorMapping: any = {
  good: "#61BA14",
  slow: "#CF1322",
  missing: "#808080",
  needs_attention: "#789FE9"
};

export const filterLabelMapping = {
  needs_attention: "acceptable",
  missing: "missing_correlation"
};

export const prVolumeIndicator = [
  {
    title: "S",
    hint: "(1-100)"
  },
  {
    title: "M",
    hint: "(100-500)"
  },
  {
    title: "L",
    hint: "(500-1000)"
  },
  {
    title: "XL",
    hint: "(>1000)"
  }
];

export const leadTimeMetricsMapping = {
  mean: "Average time",
  median: "Median time",
  p90: "90th percentile time",
  p95: "95th percentile time"
};

export const dynamicColumnPrefix = "dynamic_column_aggs_";

export const renderVelocityDynamicColumns = (key: string, activeKey?: string) => {
  if (!key.includes(dynamicColumnPrefix)) {
    return;
  }
  const updatedKey = key.replace(dynamicColumnPrefix, "");
  return {
    ...stageTimeColumn(updatedKey, key, "seconds", { sorter: true }),
    className: activeKey && activeKey.toLowerCase() === updatedKey.toLowerCase() ? "active-stage" : "",
    sortDirections: ["descend", "ascend"]
  };
};

export const renderVelocityStageDynamicColumns = (key: string, activeKey?: string) => {
  if (!key.includes(dynamicColumnPrefix)) {
    return;
  }
  const updatedKey = key.replace(dynamicColumnPrefix, "");
  return {
    ...leadTimeStageByTimeColumn(updatedKey, key, "seconds", { sorter: true }),
    className: activeKey && activeKey.toLowerCase() === updatedKey.toLowerCase() ? "active-stage" : "",
    sortDirections: ["descend", "ascend"]
  };
};

export const renderLeadTimeByTimeSpentInStageDynamicColumns = (key: string, activeKey?: string) => {
  if (!key.includes(dynamicColumnPrefix)) {
    return;
  }
  const updatedKey = key.replace(dynamicColumnPrefix, "");
  return {
    ...timeSpentInStageColumnConfig(updatedKey, key, "seconds", { sorter: true }),
    className: activeKey && activeKey.toLowerCase() === updatedKey.toLowerCase() ? "active-stage" : "",
    sortDirections: ["descend", "ascend"],
    sorter: numberSortingComparator(key)
  } as ColumnProps<any>;
};

export const azureRenderVelocityDynamicColumns = (key: string, activeKey?: string) => {
  if (!key.includes(dynamicColumnPrefix)) {
    return;
  }
  const updatedKey = key.replace(dynamicColumnPrefix, "");
  return {
    ...azureStageTimeColumn(updatedKey, key, "seconds", { sorter: true }),
    className: activeKey && activeKey.toLowerCase() === updatedKey.toLowerCase() ? "active-stage" : "",
    sortDirections: ["descend", "ascend"]
  };
};

export const transformVelocityRecords = (records: any[]) =>
  records.map((record: any) => {
    if (record && record.data && record.data.length) {
      record.data.map(({ key, mean }: any) => {
        record[`${dynamicColumnPrefix}${key}`] = mean;
      });
    }
    return record;
  });

export const convertToDay = (value: any, unit: AcceptanceTimeUnit) => {
  switch (unit) {
    case AcceptanceTimeUnit.SECONDS:
      return value / 86400;
    case AcceptanceTimeUnit.MINUTES:
      return value / 1440;
    case AcceptanceTimeUnit.DAYS:
      return value;
  }
};

export const getTimeAndUnit = (epoch: number, precisionValue: number = DEFAULT_PRECISION_VALUE) => {
  let data = getDaysAndTimeWithUnit(epoch, precisionValue);
  return data;
};

export const getClassByPrVolume = (prVolume: number) => {
  if (prVolume < 100) {
    return "s";
  } else if (prVolume >= 100 && prVolume < 500) {
    return "m";
  } else if (prVolume >= 500 && prVolume < 1000) {
    return "l";
  } else {
    return "xl";
  }
};

export const getTimeAndIndicator = (
  duration: number,
  lower_limit: number,
  upper_limit: number,
  precisionValue: number = DEFAULT_PRECISION_VALUE
) => {
  let rating;
  let tooltip;
  let color;
  let backgroudColor;

  const { time: updatedDuration, unit, extraUnit } = getTimeAndUnit(duration * 86400, precisionValue);
  const { time: updatedLowerLimit, extraUnit: lowerUnit } = getTimeAndUnit(lower_limit * 86400, precisionValue);
  const { time: updatedUpperLimit, extraUnit: upperUnit } = getTimeAndUnit(upper_limit * 86400, precisionValue);

  if (Math.round(duration) <= Math.round(lower_limit)) {
    rating = VelocityRating.GOOD;
    tooltip = `Good (< ${updatedLowerLimit}${lowerUnit})`;
    color = "#61BA14";
    backgroudColor = "rgba(97, 186, 20, 0.2)";
  } else if (Math.round(duration) >= Math.round(lower_limit) && Math.round(duration) <= Math.round(upper_limit)) {
    rating = VelocityRating.ACCEPTABLE;
    tooltip = `Needs Attention (${updatedLowerLimit}${lowerUnit} - ${updatedUpperLimit}${upperUnit})`;
    color = "#789FE9";
    backgroudColor = "rgba(120, 159, 233, 0.2)";
  } else {
    rating = VelocityRating.SLOW;
    tooltip = `Slow (> ${updatedUpperLimit}${upperUnit})`;
    color = "#CF1322";
    backgroudColor = "rgba(207, 19, 34, 0.2)";
  }

  return {
    duration: updatedDuration,
    rating,
    unit,
    extraUnit,
    tooltip,
    color,
    backgroudColor
  };
};

export const getAvgTimeAndIndicator = (data: any[]) => {
  const { duration, lower_limit, upper_limit } = data.reduce(
    (acc: any, next: any) => ({
      duration: acc["duration"] + next["duration"],
      lower_limit: acc["lower_limit"] + next["lower_limit"],
      upper_limit: acc["upper_limit"] + next["upper_limit"]
    }),
    {
      duration: 0,
      lower_limit: 0,
      upper_limit: 0
    }
  );
  // CHECK IF KEY EXISTS ONLY USE IN THIS WIDGET LEAD TIME BY TIME SPENT IN STAGES
  const durationNumber = data.filter((data: any) => data.name === SINLGE_STATE);
  let finalDuration = duration;
  if (durationNumber && durationNumber.length > 0) {
    finalDuration = durationNumber[0]?.duration ?? 0;
  }
  return getTimeAndIndicator(finalDuration, lower_limit, upper_limit);
};