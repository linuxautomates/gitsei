import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const CicdJobEndDateFilterConfig: LevelOpsFilter = {
  id: "cicd_job_end_date",
  renderComponent: UniversalTimeBasedFilter,
  label: "CICD Job End Date",
  beKey: "cicd_job_run_end_time",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
