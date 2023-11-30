import { REPORT_FILTERS_CONFIG, TIME_FILTER_RANGE_CHOICE_MAPPER } from "dashboard/constants/applications/names";
import { SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { CustomFieldMappingKey } from "dashboard/constants/helper";
import { Dict } from "types/dict";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { SupportDrillDownType } from "dashboard/constants/drilldown.constants";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export interface JiraZendeskReportTypes {
  xaxis: boolean;
  blockTimeFilterTransformation: (key: any) => boolean;
  [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: Dict<string, string>;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [TIME_FILTER_RANGE_CHOICE_MAPPER]: { jira_issue_created_at: string };
  [SHOW_SETTINGS_TAB]: boolean;
  name: string;
  application: string;
  chart_type: ChartType;
  chart_container: ChartContainerType;
  method: string;
  supported_filters: supportedFilterType;
  drilldown: SupportDrillDownType;
  uri: string;
  chart_props?: basicMappingType<any>;
  [REPORT_FILTERS_CONFIG]: LevelOpsFilter[];
}
