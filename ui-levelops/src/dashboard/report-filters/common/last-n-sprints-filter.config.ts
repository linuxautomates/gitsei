import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const LastNSprintsFilterConfig: LevelOpsFilter = {
  id: "sprint_count",
  renderComponent: UniversalInputFilterWrapper,
  label: "Last N Sprints",
  beKey: "sprint_count",
  labelCase: "title_case",
  filterMetaData: {
    type: "number"
  },
  deleteSupport: true,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
