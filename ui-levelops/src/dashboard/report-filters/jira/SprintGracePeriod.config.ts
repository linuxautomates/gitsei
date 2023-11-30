import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const SprintGracePeriodFilterConfig: LevelOpsFilter = {
  id: "creep_buffer",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sprint Creep Grace Period",
  beKey: "creep_buffer",
  labelCase: "title_case",
  filterInfo:
    "Issues added to an active sprint within the grace period will be considered as committed tickets instead of unplanned creep.",
  filterMetaData: {
    options: [
      { label: "30 minutes", value: 1800 },
      { label: "1 hour", value: 3600 },
      { label: "2 hours", value: 7200 },
      { label: "3 hours", value: 10800 },
      { label: "4 hours", value: 14400 },
      { label: "1 day", value: 86400 },
      { label: "2 days", value: 172800 }
    ],
    clearSupport: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
