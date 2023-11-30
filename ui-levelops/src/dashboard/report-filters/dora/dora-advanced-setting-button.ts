import { WIDGET_CONFIGURATION_PARENT_KEYS } from "constants/widgets";
import DoraAdvanceSettingsButton from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/DoraAdvancedSettingButton";
import { AdvancedSettingButton, LevelOpsFilter } from "model/filters/levelopsFilters";

export const DoraAdvanceSettingsButtonConfig: LevelOpsFilter = {
  id: "hide_advance_settings",
  renderComponent: DoraAdvanceSettingsButton,
  tab: WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS,
  label: "Hide Advanced Settings",
  beKey: "",
  isParentTab: true,
  filterMetaData: {
    getLabel: (args: any) => {
      const { value } = args;
      return value ? "- Hide Advanced Settings" : "+ Show Advanced Settings";
    },
    onClick: (args: any) => {
      const { value, callback } = args;
      callback?.(!value);
    }
  } as AdvancedSettingButton
};
