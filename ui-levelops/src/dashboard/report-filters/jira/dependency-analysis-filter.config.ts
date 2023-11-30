import { isSanitizedValue } from "./../../../utils/commonUtils";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, isArray } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  withDeleteProps,
  WithSwitchProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

const dependencyAnalysisFilterOptions = [
  { label: "Select issues blocked by filtered issues", value: "blocks" },
  { label: "Select issues blocking filtered issues", value: "is blocked by" },
  { label: "Select issues related to filtered issues", value: "relates to" }
];

export const DependencyAnalysisFilterConfig: LevelOpsFilter = {
  id: "links",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Dependency Analysis",
  beKey: "links",
  labelCase: "none",
  deleteSupport: true,
  getMappedValue: (args: any) => {
    const { allFilters } = args;
    const value = get(allFilters, "links");
    return isArray(value) ? value[0] : value;
  },
  filterMetaData: {
    options: dependencyAnalysisFilterOptions,
    selectMode: "default",
    sortOptions: false,
    clearSupport: true,
    mapFilterValueForBE: (current: any) => (isSanitizedValue(current) ? [current] : undefined)
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
    return { withSwitch, withDelete };
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
