import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { sprintImpactIntervalOptions } from "dashboard/graph-filters/components/Constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const SampleIntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  required: true,
  labelCase: "title_case",
  defaultValue: "week",
  filterMetaData: {
    options: sprintImpactIntervalOptions,
    selectMode: "default",
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const MetricViewByFilterConfig: LevelOpsFilter = {
  id: "view_by",
  renderComponent: UniversalSelectFilterWrapper,
  label: "View By",
  beKey: "view_by",
  labelCase: "title_case",
  defaultValue: "Points",
  filterMetaData: {
    options: [
      { value: "Points", label: "Story Points" },
      { value: "Tickets", label: "tickets" }
    ],
    selectMode: "default"
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
