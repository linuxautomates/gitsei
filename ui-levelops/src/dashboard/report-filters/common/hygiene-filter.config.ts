import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { hygieneTypes } from "dashboard/constants/hygiene.constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  withDeleteProps,
  WithSwitchProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const HygieneFilterConfig: LevelOpsFilter = {
  id: "hygiene_types",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Hygiene",
  beKey: "hygiene_types",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    options: hygieneTypes.map((item: string) => ({ label: item, value: item })),
    sortOptions: true,
    selectMode: "multiple"
  } as DropDownData,
  apiFilterProps: (args: any) => {
    const switchValue = !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined);
    const withSwitch: WithSwitchProps = {
      showSwitch: true,
      showSwitchText: true,
      switchText: "Exclude",
      switchValue,
      onSwitchValueChange: (value: any) => args.handleSwitchValueChange(args?.excludeKey ?? args?.beKey, value)
    };
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withSwitch: args?.excludeSupport === false ? undefined : withSwitch, withDelete };
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateHygieneFilterConfig = (
  options: Array<{ label: string; value: string | number }> | ((args: any) => Array<{ label: string; value: string }>),
  label?: string,
  beKey?: string,
  excludeSupport?: boolean
): LevelOpsFilter => ({
  ...HygieneFilterConfig,
  label: label ?? HygieneFilterConfig.label,
  beKey: beKey ?? HygieneFilterConfig.beKey,
  excludeSupport: excludeSupport ?? true,
  filterMetaData: { ...HygieneFilterConfig.filterMetaData, options } as DropDownData
});
