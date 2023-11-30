import { GET_GRAPH_FILTERS, PREVIEW_DISABLED, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  PARTIAL_FILTER_KEY,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
export interface LeadTimeByStageReportTypes extends BaseJiraReportTypes {
  // xaxis: boolean;
  dataKey: string;
  supportExcludeFilters: boolean;
  supportPartialStringFilters: boolean;
  [PARTIAL_FILTER_KEY]: string;
  [PREVIEW_DISABLED]: boolean;
  [CSV_DRILLDOWN_TRANSFORMER]: (data: any) => any;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FILTER_WITH_INFO_MAPPING]: any;
  [SHOW_SETTINGS_TAB]: boolean;
  [SHOW_METRICS_TAB]: boolean;
  [SHOW_AGGREGATIONS_TAB]: boolean;
  valuesToFilters: Dict<string, string>;
  [GET_GRAPH_FILTERS]: (filtrs: any) => any;
  includeContextFilter?: boolean;
  drilldownFooter?: () => React.FC;
  drilldownCheckbox?: () => React.FC<any>;
  drilldownMissingAndOtherRatings?: boolean;
  drilldownTotalColCaseChange?: boolean;
}
