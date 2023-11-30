import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { BA_COMPLETED_WORK_STATUS_BE_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import BAStatusBasedFilterContainer from "dashboard/graph-filters/components/BAStatusBasedFilter";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const StatusOfTheCompletedIssuesConfig: LevelOpsFilter = {
  id: "status-of-completed-issues",
  renderComponent: BAStatusBasedFilterContainer,
  label: "Statuses of completed issues",
  beKey: BA_COMPLETED_WORK_STATUS_BE_KEY,
  labelCase: "title_case",
  filterInfo: 'By default, all statuses in the "Done" category will be used',
  filterMetaData: {
    options: [],
    selectMode: "multiple",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
