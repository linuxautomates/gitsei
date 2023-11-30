import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { BA_IN_PROGRESS_STATUS_BE_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import BAStatusBasedFilterContainer from "dashboard/graph-filters/components/BAStatusBasedFilter";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const BAInProgressStatusFilterConfig: LevelOpsFilter = {
  id: "ba-in-progress-status-filter",
  renderComponent: BAStatusBasedFilterContainer,
  label: "Statuses of in progress issues",
  beKey: BA_IN_PROGRESS_STATUS_BE_KEY,
  labelCase: "title_case",
  filterInfo: 'By default, all statuses in the "In Progress" category will be used',
  filterMetaData: {
    options: [],
    selectMode: "multiple",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
