import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";

export type TimeFilterOptionType = {
  id: string;
  mFactor: number;
  label: string;
  value: { $gt: string | number; $lt: string | number };
};

export type FEBasedFilterConfig = {
  type: WidgetFilterType;
  label: string;
  BE_key: string;
  configTab: WIDGET_CONFIGURATION_KEYS;
  // hideOnFilterValueKeys key hides filter if value for particular filter is present
  hideOnFilterValueKeys?: string[];
  defaultValue?: string | string[];
  filterInfo?: string;
  required?: boolean;
  optionsTransformFn?: any;
  isVisible?: (filters: any) => boolean;
  getLabel?: (filters: any) => string;
};

export type FEBasedSelectFilterConfig = FEBasedFilterConfig & {
  options: Array<{ label: string; value: any }>;
  select_mode: "multiple" | "default";
};

export type FEBasedTimeFilterConfig = FEBasedFilterConfig & {
  options: Array<TimeFilterOptionType>;
  slicing_value_support?: boolean;
};

export type FEBasedToggleFilterConfig = FEBasedFilterConfig & {
  toggleLabel?: string;
};

export type FEBasedDropDownFilterConfig = FEBasedFilterConfig & {
  options: Array<{ label: string; value: string }>;
  select_mode?: "multiple" | "default";
};

export type FEBasedCheckboxConfig = FEBasedFilterConfig & {
  options: { checkboxLabel: string };
};

export type FEBasedInputFilterConfig = FEBasedTimeFilterConfig & {
  input_type: any;
};

export type FEBasedRadioFilterConfig = FEBasedFilterConfig & {
  getFilter: (value: boolean) => any;
  getValue: (filters: any, beKey: string) => boolean;
  options: Array<{ label: string; value: any }>;
};

export type FEBasedFilterMap = {
  [x: string]:
    | FEBasedToggleFilterConfig
    | FEBasedTimeFilterConfig
    | FEBasedSelectFilterConfig
    | FEBasedDropDownFilterConfig
    | FEBasedInputFilterConfig
    | FEBasedRadioFilterConfig;
};
