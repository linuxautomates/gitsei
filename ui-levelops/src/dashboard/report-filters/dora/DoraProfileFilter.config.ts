import { WIDGET_CONFIGURATION_PARENT_KEYS } from "constants/widgets";
import DoraOrgUnitDataView from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/DoraOrgUnitDataView";
import DoraProfileApiContainer from "dashboard/graph-filters/containers/Dora/DoraProfileFilterContainer";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const ProfileFilterViewConfig: LevelOpsFilter = {
  id: "profile_view",
  renderComponent: DoraOrgUnitDataView,
  apiContainer: DoraProfileApiContainer,
  tab: WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS,
  isParentTab: true,
  label: "PROFILE",
  beKey: "profile_view"
};
