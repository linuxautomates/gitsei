import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import BAStatusBasedFilterContainer from "dashboard/graph-filters/components/BAStatusBasedFilter";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const FilterAssigneeByStatusFilterConfig: LevelOpsFilter = {
  id: "filter-assignee-by-status",
  renderComponent: BAStatusBasedFilterContainer,
  label: "Filter assignees by status",
  beKey: BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  labelCase: "title_case",
  filterInfo: "If empty, the effort will be attributed to all the assignees of a ticket",
  filterMetaData: {
    options: [],
    selectMode: "multiple",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
