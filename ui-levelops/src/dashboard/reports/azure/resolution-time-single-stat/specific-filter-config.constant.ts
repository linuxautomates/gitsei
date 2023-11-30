import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalStatTimeRangeFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalStatTimeRangeFilterWrapper";
import { LevelOpsFilter, StatTimeRangeFilterData } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const TimeRangeTypeFilterConfig: LevelOpsFilter = {
  id: "stat_time_based",
  renderComponent: UniversalStatTimeRangeFilterWrapper,
  label: "Time Range Type",
  beKey: "stat_time_based",
  labelCase: "none",
  required: true,
  defaultValue: "workitem_created_at",
  filterMetaData: {
    options: [
      { value: "workitem_created_at", label: "Workitem Created" },
      { value: "workitem_resolved_at", label: "Workitem Resolved" },
      { value: "workitem_updated_at", label: "Workitem Updated" }
    ],
    filterLabel: (data: any) => {
      const { allFilters } = data;
      return allFilters.across ? `${allFilters.across.replace("_at", "").replace(/_/g, " ")} in` : "";
    },
    filterKey: (data: any) => {
      const { allFilters } = data;
      return allFilters.across ? `${allFilters.across}` : "";
    }
  } as StatTimeRangeFilterData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  }
};
