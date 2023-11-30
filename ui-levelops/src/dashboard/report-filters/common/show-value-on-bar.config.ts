import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalCheckboxFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCheckboxFilter";
import { CheckboxData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const ShowValueOnBarConfig: LevelOpsFilter = {
  id: "show_value_on_bar",
  renderComponent: UniversalCheckboxFilter,
  label: "Bar Chart Options",
  beKey: "show_value_on_bar",
  labelCase: "title_case",
  updateInWidgetMetadata: true,
  isFEBased: true,
  defaultValue: true,
  filterMetaData: {
    checkboxLabel: "Show value above bar"
  } as CheckboxData,
  hideFilter: (args: any) => {
    return (
      !!args.filters?.visualization && (args.filters?.visualization !== "bar_chart" || args.filters?.stacks?.length > 0)
    );
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
