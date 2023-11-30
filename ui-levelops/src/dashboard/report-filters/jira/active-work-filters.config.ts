import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  jiraActiveWorkUnitOptions
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { ActiveEffortUnitType } from "dashboard/constants/enums/jira-ba-reports.enum";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";

export const activeWorkFilterConfig: LevelOpsFilter = {
  id: "active-work-filters",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Active Work Unit",
  beKey: ACTIVE_WORK_UNIT_FILTER_KEY,
  labelCase: "title_case",
  defaultValue: ActiveEffortUnitType.JIRA_TICKETS_COUNT,
  filterMetaData: {
    options: jiraActiveWorkUnitOptions,
    selectMode: "default",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateActiveWorkFilter = (metadata?: {
  activeWorkOptions: {
    label: string;
    value: string;
  }[];
  defaultValue: string;
}): LevelOpsFilter =>
  ({
    ...activeWorkFilterConfig,
    defaultValue: metadata?.defaultValue ?? ActiveEffortUnitType.JIRA_TICKETS_COUNT,
    filterMetaData: {
      ...(activeWorkFilterConfig.filterMetaData ?? {}),
      options: metadata?.activeWorkOptions ?? jiraActiveWorkUnitOptions
    }
  } as LevelOpsFilter);
