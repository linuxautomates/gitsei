import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { DEFAULT_METADATA, FILTER_KEY_MAPPING, SHOW_METRICS_TAB } from "dashboard/constants/filter-key.mapping";
import { HIDE_CUSTOM_FIELDS } from "../../../../dashboard/constants/applications/names";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "../../../../dashboard/reports/jira/constant";

export interface SprintGoalReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [DEFAULT_METADATA]: { [key: string]: any };
  [FILTER_KEY_MAPPING]: { [key: string]: any };
  [SHOW_METRICS_TAB]: boolean;
  [HIDE_CUSTOM_FIELDS]: boolean;
  [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: any;
}
