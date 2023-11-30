import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import LeadTimeConfigProfileFilter from "dashboard/graph-filters/components/GenericFilterComponents/LeadTimeConfigurationProfile";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const LeadTimeConfigurationProfileFilterConfig: LevelOpsFilter = {
  id: "velocity_config_id",
  renderComponent: LeadTimeConfigProfileFilter,
  label: "Workflow Configuration Profile",
  beKey: "velocity_config_id",
  labelCase: "title_case",
  filterMetaData: {},
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
