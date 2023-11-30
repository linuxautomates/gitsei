import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

const MAX_ENTRIES_OPTIONS = [
  { label: "10", value: 10 },
  { label: "20", value: 20 },
  { label: "50", value: 50 },
  { label: "100", value: 100 }
];

export const MaxRecordsFilterConfig: LevelOpsFilter = {
  id: "max_records",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Max X-Axis Entries",
  beKey: "max_records",
  labelCase: "none",
  updateInWidgetMetadata: true,
  filterMetaData: {
    selectMode: "default",
    options: MAX_ENTRIES_OPTIONS,
    sortOptions: false
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateMaxRecordsFilterConfig = (
  options?: { label: string; value: any }[],
  label?: string
): LevelOpsFilter => ({
  ...MaxRecordsFilterConfig,
  label: label ?? MaxRecordsFilterConfig.label,
  filterMetaData: {
    ...MaxRecordsFilterConfig.filterMetaData,
    options: options ?? MAX_ENTRIES_OPTIONS
  } as DropDownData
});
