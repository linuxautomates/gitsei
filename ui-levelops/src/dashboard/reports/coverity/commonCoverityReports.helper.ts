import { get } from "lodash";
import {
  WithSwitchProps,
  withDeleteProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const coverityReportXaxisLabelTransform = (params: any) => {
  const { across, item = {} } = params;
  const { key, additional_key } = item;

  let newLabel = key;

  if (["first_detected", "last_detected", "first_detected_stream", "last_detected_stream"].includes(across)) {
    newLabel = additional_key;
  }

  return newLabel;
};

export const apiFilterProps = (args: any) => {
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
};
