import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const StageDurationFilterConfig: LevelOpsFilter = {
  id: "stage_duration",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stage Duration",
  beKey: "metrics",
  updateInWidgetMetadata: true,
  defaultValue: "mean",
  labelCase: "none",

  filterMetaData: {
    options: [
      { value: "p90", label: "90th percentile time in stage" },
      { value: "p95", label: "95th percentile time in stage" },
      { value: "mean", label: "Average time in stage" },
      { value: "median", label: "Median time in stage" }
    ],
    sortOptions: true
  } as DropDownData,

  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
