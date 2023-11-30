import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { pagerdutyTimeToResolveAxisOptions } from "dashboard/graph-filters/components/Constants";
import PagerdutyOfficeHoursComponent from "dashboard/graph-filters/components/GenericFilterComponents/PagerdutyOfficeHoursComponent";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { STACK_OPTIONS } from "./constants";

export const OfficeHoursFilterConfig: LevelOpsFilter = {
  id: "office-hours-filter",
  renderComponent: PagerdutyOfficeHoursComponent,
  label: "Office Hours",
  beKey: "office_hours",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: [],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const StackFilterConfig: LevelOpsFilter = {
  id: "pagerduty-stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  filterMetaData: {
    clearSupport: true,
    options: STACK_OPTIONS,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const LeftYAxisFilterConfig: LevelOpsFilter = {
  id: "leftYAxis",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Left Y-Axis",
  beKey: "leftYAxis",
  labelCase: "title_case",
  defaultValue: "mean",
  updateInWidgetMetadata: true,
  filterMetaData: {
    options: pagerdutyTimeToResolveAxisOptions,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const IssueTypeFilterConfig: LevelOpsFilter = {
  id: "issue-type-filter",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Issue Type",
  beKey: "issue_type",
  labelCase: "title_case",
  deleteSupport: true,
  apiFilterProps: args => ({ withDelete: withDeleteAPIProps(args) }),
  filterMetaData: {
    selectMode: "default",
    options: [
      { label: "Alert", value: "alert" },
      { label: "Incident", value: "incident" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const SampleIntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  labelCase: "title_case",
  filterMetaData: {
    options: [
      { value: "day", label: "Day" },
      { value: "week", label: "Week" },
      { value: "month", label: "Month" },
      { value: "quarter", label: "Quarter" },
      { value: "year", label: "Year" }
    ],
    selectMode: "default",
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
