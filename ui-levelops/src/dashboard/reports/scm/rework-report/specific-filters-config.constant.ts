import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { lastFileUpdateIntervalOptions, LEGACY_CODE_INFO } from "dashboard/graph-filters/components/Constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const LegacyCodeFiltersConfig: LevelOpsFilter = {
  id: "legacy-code",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Legacy Code",
  beKey: "legacy_update_interval_config",
  subtitle: "LAST FILE UPDATE TIMESTAMP",
  defaultValue: 30,
  labelCase: "title_case",
  filterInfo: LEGACY_CODE_INFO,
  updateInWidgetMetadata: true,
  filterMetaData: {
    selectMode: "default",
    options: lastFileUpdateIntervalOptions,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
