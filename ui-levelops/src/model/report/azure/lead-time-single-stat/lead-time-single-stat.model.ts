import { PREVIEW_DISABLED, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { CSV_DRILLDOWN_TRANSFORMER, SHOW_AGGREGATIONS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureLeadTimeStatReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_WITH_INFO_MAPPING]: any;
  [FILTER_NAME_MAPPING]: any;
  compareField: string;
  chart_click_enable: boolean;
}
