import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

const defaultTimePeriodOptions = [
  {
    label: "Last day",
    value: 1
  },
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last 30 days",
    value: 30
  }
];
export const TimePeriodFilterConfig: LevelOpsFilter = {
  id: "time_period",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Time Period",
  beKey: "time_period",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    options: defaultTimePeriodOptions,
    selectMode: "default"
  } as DropDownData,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateTimePeriodFilterConfig = (
  filterOptions?: Array<{ label: string; value: number }>,
  args?: { required?: boolean },
  label?: string
): LevelOpsFilter => ({
  ...TimePeriodFilterConfig,
  required: args?.required ?? false,
  label: label ? label : "Time Period",
  filterMetaData: {
    ...TimePeriodFilterConfig.filterMetaData,
    options: filterOptions ?? defaultTimePeriodOptions
  } as DropDownData
});
