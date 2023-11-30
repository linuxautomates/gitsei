import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import CodingDaysCountFilterWrapperComponent from "dashboard/graph-filters/components/CodingDaysCountFilterWrapperComponent";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const DaysCountFilterConfig: LevelOpsFilter = {
  id: "days_count",
  renderComponent: CodingDaysCountFilterWrapperComponent,
  label: "Show Authors With",
  beKey: "days_count",
  labelCase: "none",
  filterMetaData: {},
  deleteSupport: false,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  }
};
