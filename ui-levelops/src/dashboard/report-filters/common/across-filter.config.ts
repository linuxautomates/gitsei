import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

// This is a base config don't use this , use generator function
const BaseAcrossFilterConfig: LevelOpsFilter = {
  id: "across",
  renderComponent: UniversalSelectFilterWrapper,
  label: "X-Axis",
  beKey: "across",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    options: [{}],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const generateAcrossFilterConfig = (
  options: Array<{ label: string; value: string | number }> | ((args: any) => Array<{ label: string; value: string }>),
  defaultValue?: string
): LevelOpsFilter => ({
  ...BaseAcrossFilterConfig,
  defaultValue: defaultValue ?? "",
  filterMetaData: { ...BaseAcrossFilterConfig.filterMetaData, options } as DropDownData
});
