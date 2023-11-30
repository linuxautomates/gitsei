import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalModulePathFilters from "dashboard/graph-filters/components/GenericFilterComponents/UniversalModulePathFilters";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const CrossAggregationModulePathFilterConfig: LevelOpsFilter = {
  id: "module_path_filter",
  renderComponent: UniversalModulePathFilters,
  label: "Module Paths",
  beKey: "module",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: [],
    uri: "jira_zendesk_files_report",
    method: "list"
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateCrossAggregationModulePathFilterConfig = (
  uri?: string,
  hideFilter?: (args: any) => boolean | boolean,
  isSelected?: (args: any) => boolean | boolean
): LevelOpsFilter => ({
  ...CrossAggregationModulePathFilterConfig,
  hideFilter: hideFilter,
  isSelected,
  filterMetaData: {
    ...CrossAggregationModulePathFilterConfig.filterMetaData,
    uri: uri ?? "jira_zendesk_files_report"
  } as any
});
