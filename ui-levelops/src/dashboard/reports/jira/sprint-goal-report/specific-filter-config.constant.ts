import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { CONTAINS } from "dashboard/constants/constants";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import SprintGoalSprintSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/SprintGoalSprintFilter";
import { getFilterValue } from "helper/widgetFilter.helper";
import { get } from "lodash";
import { ApiDropDownData, LevelOpsFilter, LevelOpsFilterTypes, baseFilterConfig } from "model/filters/levelopsFilters";
import {
  switchWithDropdownProps,
  withDeleteProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { isSanitizedValue } from "utils/commonUtils";

export const SprintFilterConfig: LevelOpsFilter = baseFilterConfig("sprint", {
  renderComponent: SprintGoalSprintSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "Sprint",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.DROPDOWN,
  labelCase: "none",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "sprint",
  apiFilterProps: (args: any) => {
    const partialValue = get(
      args.allFilters,
      [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
      {}
    );
    const switchValue = !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined);
    const selectPropOptions = [
      {
        label: "Contain",
        value: CONTAINS
      }
    ];
    const switchWithDropdown: switchWithDropdownProps = {
      showSwitchWithDropdown: true,
      checkboxProps: {
        text: "Include all values that",
        disabled: switchValue,
        value: Object.keys(partialValue).length > 0
      },
      selectProps: {
        options: selectPropOptions,
        value: Object.keys(partialValue).length > 0 ? Object.keys(partialValue)[0] : selectPropOptions[0].value,
        disabled: switchValue || !(Object.keys(partialValue).length > 0),
        onSelectChange: (key: any) =>
          args?.handlePartialValueChange?.(
            args?.partialKey ?? args?.beKey,
            key ? { [key]: Object.values(partialValue)[0] || "" } : undefined
          )
      }
    };
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { switchWithDropdown, withDelete };
  },
  filterMetaData: {
    uri: "jira_filter_values",
    method: "list",
    selectMode: "default",
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "sprint");
      if (currData) {
        return (Object.values(currData)[0] as Array<any>)?.map((item: any) => ({
          label: item.key,
          value: item.key
        }));
      }
      return [];
    },
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["sprint"],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    sortOptions: true
  } as ApiDropDownData,
  isSelected: (args: any) => {
    const { filters } = args || {};
    const hasValue = isSanitizedValue(getFilterValue(filters, "jira_sprint_states", true, undefined)?.value);
    const hasLastSprint = !!filters?.last_sprint;
    const hasSprintStates = !!filters?.sprint_states || !!filters?.jira_sprint_states;
    if (hasValue || hasLastSprint || hasSprintStates) {
      return true;
    }
    return false;
  }
});
