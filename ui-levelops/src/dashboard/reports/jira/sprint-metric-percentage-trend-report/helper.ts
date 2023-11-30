import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const sprintMetricPerTrendReportChartClickPayload = (props: { data: basicMappingType<any> }) => {
  const filter = {
    sprint_name: props.data?.activeLabel,
    sprint_id: props.data?.activePayload?.[0]?.payload?.sprint_id
  };
  return filter;
};
