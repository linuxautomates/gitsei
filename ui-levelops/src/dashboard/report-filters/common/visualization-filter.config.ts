import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

// This is a base config don't use this , use generator function
const BaseVisualizationFilterConfig: LevelOpsFilter = {
  id: "visualization",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Visualization",
  beKey: "visualization",
  labelCase: "title_case",
  updateInWidgetMetadata: true,
  filterMetaData: {
    selectMode: "default",
    options: [{}],
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateVisualizationFilterConfig = (
  options: Array<{ label: string; value: string | number }> | ((args: any) => Array<{ label: string; value: string }>),
  defaultValue?: string | number | Object,
  updateInWidgetMetadata?: boolean
): LevelOpsFilter => ({
  ...BaseVisualizationFilterConfig,
  defaultValue,
  updateInWidgetMetadata: updateInWidgetMetadata ?? BaseVisualizationFilterConfig.updateInWidgetMetadata,
  filterMetaData: { ...BaseVisualizationFilterConfig.filterMetaData, options } as DropDownData
});
