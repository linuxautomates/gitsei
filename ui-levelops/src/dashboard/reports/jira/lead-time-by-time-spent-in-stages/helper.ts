import { dynamicColumnPrefix } from "custom-hooks/helpers/leadTime.helper";
import { get, set } from "lodash";
import { STAGE_DURATION_FILTERS_OPTIONS } from "./constant";

export const leadTimeByTimeSpentTransformDrilldownRecords = (data: { records: any[] }) => {
  if (!("records" in data)) return [];

  return data?.records.map((record: any) => {
    if (record?.velocity_stages?.length) {
      record?.velocity_stages?.map(({ stage, time_spent }: any) => {
        record[`${dynamicColumnPrefix}${stage}`] = time_spent;
      });
    }
    return record;
  });
};

export const getMetadataFiltersPreviewHelper = (metadata: any) => {
  const value = get(metadata, ["metrics"], "mean");
  const final_filters = [];
  const corsMetric = STAGE_DURATION_FILTERS_OPTIONS.find(item => item.value === value);
  if (corsMetric) {
    final_filters.push({
      label: "Metric",
      value: corsMetric.label
    });
  }
  return final_filters;
};

export const mapFiltersForWidgetApi = (filter : any, filterValue: any) => {
  set(filter, ["filter", "excludeVelocityStages"], filterValue);
  return filter;
}

export const getDrilldownTitleInStage = (params: any) => {
  const { xAxis } = params;
  return xAxis === 'Total_Time_In_stage' ? 'Total Time' : xAxis;
};

export const getDrillDownType = () => {
  return "Stage";
};
