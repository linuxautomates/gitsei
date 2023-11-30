import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalOUFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalOUFilterWrapper";
import { LevelOpsFilter, OUFilterByApplicationType, OUFilterData } from "model/filters/levelopsFilters";

export const OUFilterConfig: LevelOpsFilter = {
  id: "ou_filter",
  renderComponent: UniversalOUFilterWrapper,
  filterInfo: "Propelo uses the default field for Collection base aggregations. Override Collection fields here.",
  label: "",
  beKey: "ou_user_filter_designation",
  labelCase: "none",
  filterMetaData: {},
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateOUFilterConfig = (
  filterByApplications: OUFilterByApplicationType,
  defaultValue?: string[]
): LevelOpsFilter => ({
  ...OUFilterConfig,
  defaultValue,
  filterMetaData: { ...OUFilterConfig.filterMetaData, filtersByApplications: filterByApplications } as OUFilterData
});
