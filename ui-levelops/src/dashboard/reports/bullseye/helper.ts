import { CONTAINS, STARTS_WITH } from "dashboard/constants/constants";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { get } from "lodash";
import { switchWithDropdownProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const apiFilterProps = (args: any) => {
  const partialValue = get(
    args.allFilters,
    [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
    {}
  );
  const switchValue = !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined);
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
  return { switchWithDropdown, withDelete: withDeleteAPIProps(args) };
};
