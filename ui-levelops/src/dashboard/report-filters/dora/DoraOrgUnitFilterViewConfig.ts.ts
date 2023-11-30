import { WIDGET_CONFIGURATION_PARENT_KEYS } from "constants/widgets";
import DoraOrgUnitDataView from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/DoraOrgUnitDataView";
import OrgUnitFilterViewApiContainer from "dashboard/graph-filters/containers/Dora/DoraOrgUnitView";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const OrgUnitDataViewFilterConfig: LevelOpsFilter = {
  id: "org_view",
  renderComponent: DoraOrgUnitDataView,
  apiContainer: OrgUnitFilterViewApiContainer,
  tab: WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS,
  isParentTab: true,
  label: "COLLECTION LEVEL FILTERS",
  beKey: "org_view"
};
