import { STARTS_WITH, CONTAINS } from "dashboard/constants/constants";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { get } from "lodash";
import {
  WithSwitchProps,
  withDeleteProps,
  switchWithDropdownProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const apiFilterProps = (args: any) => {
  const options = args.options || [];
  let newOptions = [];
  if (options.length as any[]) {
    newOptions = (options || [])
      .filter((item: any) => !!item["key"])
      .map((item: any, index: number) => ({
        label: item["key"]?.replace("_", " "),
        value: item["key"],
        ...(item?.hasOwnProperty("parent_key") ? { parent_key: item["parent_key"] } : {})
      }))
      ?.sort(stringSortingComparator("label"));
  }
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
  const partialValue = get(
    args.allFilters,
    [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
    {}
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
  return { withDelete, options: newOptions, withSwitch, switchWithDropdown };
};

export const getMappedValue = (args: any) => {
  const allFilters = args.allFilters;
  let azureIterationValues =
    get(allFilters, ["azure_iteration"], undefined) || get(allFilters, ["exclude", "azure_iteration"], undefined) || [];

  if (azureIterationValues.length && typeof azureIterationValues[0] === "object") {
    azureIterationValues = azureIterationValues.map(
      (rec: { child: string; parent: string }) => `${rec?.parent}\\${rec?.child}`
    );
  }
  return azureIterationValues;
};

export const isSelected = (args: any) => {
  const { filters } = args;
  return (
    !!get(filters, ["azure_iteration"], undefined) ||
    !!get(filters, ["exclude", "azure_iteration"], undefined) ||
    !!get(filters, ["partial_match", "azure_iteration"], undefined) ||
    !!get(filters, ["workitem_sprint_states"], undefined)
  );
};
