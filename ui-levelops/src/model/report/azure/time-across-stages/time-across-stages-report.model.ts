import { filterWithInfoType } from "dashboard/constants/filterWithInfo.mapping";
import { optionType } from "dashboard/dashboard-types/common-types";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureTimeAcrossStagesReportType extends BaseAzureReportTypes {
  dataKey: string;
  appendAcrossOptions: Array<optionType>;
  filterWithInfoMapping: Array<filterWithInfoType>;
  valuesToFilters: { [x: string]: string };
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER: {
    [s: string]: boolean;
  };
  onChartClickPayload: (params: any) => any;
  xAxisLabelTransform: (params: any) => any;
}
