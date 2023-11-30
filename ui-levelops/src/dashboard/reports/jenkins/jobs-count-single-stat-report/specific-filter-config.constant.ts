import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import JenkinsParametersFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/jenkinsParametersFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JobEndDateFilterConfig: LevelOpsFilter = {
  id: "job_end_date",
  renderComponent: UniversalTimeBasedFilter,
  label: "End Date",
  beKey: "end_time",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const JobStartDateFilterConfig: LevelOpsFilter = {
  id: "job_start_date",
  renderComponent: UniversalTimeBasedFilter,
  label: "Job Start Date",
  beKey: "start_time",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
