import { CONTAINS, STARTS_WITH } from "dashboard/constants/constants";
import { FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import {
  WithSwitchProps,
  switchWithDropdownProps,
  withDeleteProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const withDeleteAPIProps = (args: any): withDeleteProps => ({
  showDelete: args?.deleteSupport,
  key: args?.beKey,
  onDelete: args.handleRemoveFilter
});

export const WithSwitchFilterProps = (args: any): WithSwitchProps => ({
  showSwitch: true,
  showSwitchText: true,
  switchText: "Exclude",
  switchValue: !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined),
  onSwitchValueChange: (value: any) => args.handleSwitchValueChange(args?.excludeKey ?? args?.beKey, value, args?.filterMetaData?.selectMode)
});

const genericApiFilterProps = (args: any) => {

  let partialValue = get(
    args.allFilters,
    [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
    {}
  );

  if (Object.keys(partialValue).length === 0 && args?.allowExcludeWithPartialMatch) {
    partialValue = get(
      args.allFilters,
      ["exclude", "partial_match", args?.partialKey ?? args?.excludeKey ?? args?.beKey],
      {}
    );
  }

  let switchValue = !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined);
  if (!switchValue && args?.allowExcludeWithPartialMatch) {
    switchValue = !!get(args?.allFilters || {}, ["exclude", "partial_match", args?.partialKey ?? args?.excludeKey ?? args?.beKey], undefined)
  }

  const selectPropOptions = [
    {
      label: "Start With",
      value: STARTS_WITH
    },
    {
      label: "Contain",
      value: CONTAINS
    }
  ];
  const withSwitch: WithSwitchProps = {
    showSwitch: true,
    showSwitchText: true,
    switchText: "Exclude",
    switchValue,
    onSwitchValueChange: (value: any) => args.handleSwitchValueChange(
      args?.excludeKey ?? args?.beKey,
      value, args?.filterMetaData?.selectMode,
      {
        allowExcludeWithPartialMatch: args?.allowExcludeWithPartialMatch,
        partialKeyName: args?.partialKey ?? args?.excludeKey ?? args?.beKey,
        excludeKeyName: args?.excludeKey ?? args?.beKey
      }
    )
  };

  const filtersNotSupportingPartialFilter: string[] = getWidgetConstant(
    args?.report,
    FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
    []
  );

  const switchWithDropdown: switchWithDropdownProps = {
    showSwitchWithDropdown: filtersNotSupportingPartialFilter.length
      ? !filtersNotSupportingPartialFilter.includes(args?.beKey)
      : true,
    checkboxProps: {
      text: "Include all values that",
      disabled: args?.allowExcludeWithPartialMatch ? false : switchValue,
      value: Object.keys(partialValue).length > 0
    },
    selectProps: {
      options: selectPropOptions,
      value: Object.keys(partialValue).length > 0 ? Object.keys(partialValue)[0] : selectPropOptions[0].value,
      disabled: args?.allowExcludeWithPartialMatch ? false : switchValue || !(Object.keys(partialValue).length > 0),
      onSelectChange: (key: any) =>
        args?.handlePartialValueChange?.(
          args?.partialKey ?? args?.beKey,
          key ? { [key]: Object.values(partialValue)[0] || "" } : undefined,
          {
            allowExcludeWithPartialMatch: args?.allowExcludeWithPartialMatch,
            partialKeyName: args?.partialKey ?? args?.excludeKey ?? args?.beKey,
            excludeKeyName: args?.excludeKey ?? args?.beKey
          }
        )
    }
  };
  return { withSwitch, switchWithDropdown, withDelete: withDeleteAPIProps(args), allowExcludeWithPartialMatch: args?.allowExcludeWithPartialMatch };
};

export default genericApiFilterProps;

export const genericCustomFieldApiProps = (args: any) => {
  const partialValue = get(
    args.allFilters,
    [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
    {}
  );
  const switchValue = !!get(
    args?.allFilters || {},
    ["exclude", "custom_fields", args?.excludeKey ?? args?.beKey],
    undefined
  );
  const selectPropOptions = [
    {
      label: "Start With",
      value: STARTS_WITH
    },
    {
      label: "Contain",
      value: CONTAINS
    }
  ];
  const withSwitch: WithSwitchProps = {
    showSwitch: true,
    showSwitchText: true,
    switchText: "Exclude",
    switchValue,
    onSwitchValueChange: (value: any) => args.handleSwitchValueChange(args?.excludeKey ?? args?.beKey, value)
  };

  const filtersNotSupportingPartialFilter: string[] = getWidgetConstant(
    args?.report,
    FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
    []
  );

  const switchWithDropdown: switchWithDropdownProps = {
    showSwitchWithDropdown: filtersNotSupportingPartialFilter.length
      ? !filtersNotSupportingPartialFilter.includes(args?.beKey)
      : args?.partialSupport ?? true,
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
  return { withSwitch, switchWithDropdown, withDelete: withDeleteAPIProps(args) };
};
