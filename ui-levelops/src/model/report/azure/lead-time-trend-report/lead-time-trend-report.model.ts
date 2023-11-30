import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { CSV_DRILLDOWN_TRANSFORMER } from "dashboard/constants/filter-key.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureLeadTimeTrendReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_WITH_INFO_MAPPING]: any;
  [WIDGET_MIN_HEIGHT]: string;
  [CSV_DRILLDOWN_TRANSFORMER]: any;
  shouldJsonParseXAxis: any;
}
