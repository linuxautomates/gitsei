import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { METRICS_OPTIONS } from "./constants";

export const VisualizationFilterConfig: LevelOpsFilter = {
  id: "visualization",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Visualization",
  beKey: "visualization",
  labelCase: "title_case",
  defaultValue: "stacked_area",
  filterMetaData: {
    selectMode: "default",
    options: [
      { value: "stacked_area", label: "Stacked Area" },
      { value: "unstacked_area", label: "Unstacked Area" },
      { value: "line", label: "Line" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metric",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metric",
  labelCase: "title_case",
  filterMetaData: {
    options: METRICS_OPTIONS,
    selectMode: "multiple",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
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
export const AcrossFilterConfig: LevelOpsFilter = {
  id: "across",
  renderComponent: UniversalSelectFilterWrapper,
  label: "X-Axis",
  beKey: "across",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    selectMode: "default",
    options: [
      {
        label: "Weekly by Sprint end date",
        value: "week"
      },
      {
        label: "Bi-weekly by Sprint end date",
        value: "bi_week"
      },
      {
        label: "Monthly",
        value: "month"
      },
      {
        label: "Sprint",
        value: "sprint"
      }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};
