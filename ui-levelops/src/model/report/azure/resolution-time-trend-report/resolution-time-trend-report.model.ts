import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "../../../../dashboard/constants/filter-name.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { DEFAULT_METADATA, FILTER_KEY_MAPPING } from "dashboard/constants/filter-key.mapping";

export interface AzureResolutionTimeTrendReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_WITH_INFO_MAPPING]: any;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  valuesToFilters: any;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: any;
}
