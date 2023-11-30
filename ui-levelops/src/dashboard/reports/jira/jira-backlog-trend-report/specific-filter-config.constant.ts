import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputTimeRangeWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputTimeRangeWrapper";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { get, uniqBy } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { ACROSS_OPTIONS } from "./constants";

export const SnapshotTimeRangeFilterConfig: LevelOpsFilter = {
  id: "snapshot_range",
  renderComponent: UniversalTimeBasedFilter,
  label: "SNAPSHOT TIME RANGE",
  beKey: "snapshot_range",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  filterMetaData: {
    clearSupport: true,
    options: (args: any) => {
      const commonOptions = ACROSS_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({ label: item.name, value: item.key }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const SampleIntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    options: [
      { value: "week", label: "Weekly on Monday" },
      { value: "month", label: "First day of the month" },
      { value: "quarter", label: "First day of the quarter" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const LeftYAxisFilterConfig: LevelOpsFilter = {
  id: "leftYAxis",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Left Y-Axis",
  beKey: "leftYAxis",
  labelCase: "title_case",
  defaultValue: "total_tickets",
  updateInWidgetMetadata: true,
  filterMetaData: {
    options: [
      { value: "total_tickets", label: "Number of tickets" },
      { value: "total_story_points", label: "Sum of Story Points" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const RightYAxisFilterConfig: LevelOpsFilter = {
  id: "rightYAxis",
  renderComponent: UniversalSelectFilterWrapper,
  label: "right Y-Axis",
  beKey: "rightYAxis",
  labelCase: "title_case",
  updateInWidgetMetadata: true,
  defaultValue: "median",
  filterMetaData: {
    options: [
      { value: "median", label: "Median Age of Tickets" },
      { value: "p90", label: "90th percentile Age of Tickets" },
      { value: "mean", label: "Average Age of Tickets" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const MinimumAgeFilterConfig: LevelOpsFilter = {
  id: "age",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Minimum Age",
  beKey: "age",
  labelCase: "none",
  filterMetaData: {},
  deleteSupport: true,
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
