import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const bounceStatWidgetFilters: basicMappingType<any> = {
  across: "trend"
};

export const bounceSingleStatChartProps: basicMappingType<any> = {
  unit: "Bounces"
};

export const bounceSingleStatDefaultQuery: basicMappingType<any> = {
  time_period: 1,
  agg_type: "average"
};
