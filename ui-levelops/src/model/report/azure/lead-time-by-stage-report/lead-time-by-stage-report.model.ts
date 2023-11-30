import { PREVIEW_DISABLED, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { CSV_DRILLDOWN_TRANSFORMER, SHOW_AGGREGATIONS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureLeadTimeByStageReportType extends BaseAzureReportTypes {
  dataKey: string;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_WITH_INFO_MAPPING]: any;
  [CSV_DRILLDOWN_TRANSFORMER]: any;
  [FILTER_NAME_MAPPING]: any;
  [PREVIEW_DISABLED]: boolean;
  [WIDGET_MIN_HEIGHT]: string;
  drilldownFooter?: () => React.FC;
  drilldownCheckbox?: () => React.FC<any>;
  drilldownMissingAndOtherRatings?: boolean;
  drilldownTotalColCaseChange?: boolean;
}
