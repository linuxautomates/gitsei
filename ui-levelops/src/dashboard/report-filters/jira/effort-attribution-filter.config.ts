import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import {
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  EffortAttributionOptions,
  effortAttributionOptions
} from "dashboard/constants/bussiness-alignment-applications/constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const EffortAttributionFilterConfig: LevelOpsFilter = {
  id: "effort-attribution",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Effort attribution",
  beKey: BA_EFFORT_ATTRIBUTION_BE_KEY,
  labelCase: "title_case",
  defaultValue: EffortAttributionOptions.CURRENT_ASSIGNEE,
  filterMetaData: {
    options: effortAttributionOptions,
    selectMode: "default",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
