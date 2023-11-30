import { WIDGET_CONFIGURATION_PARENT_KEYS } from "constants/widgets";
import UniversalCheckboxFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCheckboxFilter";
import { CheckboxData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const ShowDoraGradingConfig: LevelOpsFilter = {
  id: "show_dora_grading",
  renderComponent: UniversalCheckboxFilter,
  label: "",
  beKey: "show_dora_grading",
  labelCase: "title_case",
  updateInWidgetMetadata: true,
  isFEBased: true,
  defaultValue: true,
  filterMetaData: {
    checkboxLabel: "Show DORA grading"
  } as CheckboxData,
  tab: WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS,
  isParentTab: true
};
