import { TimeFilterOptionType } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { omit } from "lodash";
import { RestAPIMetadata } from "model/APIMetadata.type";
import {
  switchWithDropdownProps,
  withDeleteProps,
  WithSwitchProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { WIDGET_CONFIGURATION_KEYS, WIDGET_CONFIGURATION_PARENT_KEYS } from "../../constants/widgets";

export type SelectModeType = "default" | "multiple" | "tags";

export enum LevelOpsFilterTypes {
  DROPDOWN = "DROPDOWN",
  API_DROPDOWN = "API_DROPDOWN",
  TIME_RANGE = "TIME_RANGE",
  CUSTOM = "CUSTOM",
  TAGS = "TAGS",
  INPUT = "INPUT",
  SEARCH = "SEARCH",
  BINARY = "BINARY",
  DATE_RANGE = "DATE_RANGE"
}

export interface FilterData {
  placeholder?: string;
  dashboardMetaData?: any;
  reportType?: string;
  customFieldsRecords?: any[];
  alwaysExclude?: boolean;
  sprintApiData?: any[];
  customHygienes?: any[];
}

export interface CommonDropDownData extends FilterData {
  selectMode: SelectModeType;
  selectModeFunction?: ((args: any) => SelectModeType);
  clearSupport?: boolean;
  createOption?: boolean;
  sortOptions?: boolean;
  checkValueWithApiResponse?: boolean;
}

export interface DropDownData extends CommonDropDownData {
  valueKey?: string;
  labelKey?: string;
  options: Array<{ label: string; value: any }> | ((args: any) => Array<{ label: string; value: any }>);
  mapFilterValueForBE?: (value: any, args?: Record<string, any>) => any;
  customEpics?: any;
}

export type APIFilterConfigType = {
  data: Array<any> | undefined;
  loading: boolean;
  error: string | boolean | undefined;
  metadata: RestAPIMetadata | undefined;
};

export interface ApiDropDownData extends DropDownData {
  uri: string;
  integration_ids?: Array<string>;
  method?: string;
  searchField?: string;
  defaultValue?: string | number | Object;
  specialKey?: string;
  payload?: Record<string, any> | ((args: Record<string, any>) => Record<string, any>);
  apiConfig?: APIFilterConfigType;
  additionalFilter?: any;
  childComponentFilter?: any;
  callApiOnParentValueChange?: boolean;
  filters?: any;
}

export interface TimeRangeData extends CommonDropDownData {
  options: Array<TimeFilterOptionType>;
  slicing_value_support?: boolean;
  maxRange?: TimeRangeData;
}

export interface TagsData extends FilterData {}

export interface InputData {
  placeholder?: string;
  type?: string;
}

export interface CheckboxData {
  checkboxLabel?: string;
}

export interface AdvancedSettingButton {
  onClick: (args: any) => any;
  getLabel: (args: any) => string;
}
export interface InputRangeFilterData extends FilterData {
  greaterThanKey?: string;
  lessThanKey?: string;
}

export interface SearchData extends FilterData {}

export interface BinaryData extends FilterData {}

export interface DateRangeData extends FilterData {}

export interface StatTimeRangeFilterData extends DropDownData {
  filterLabel?: string | ((args: any) => string);
  filterKey?: string | ((args: any) => string);
}

export type OUFilterByApplicationType = Record<
  string,
  { options: Array<{ label: string; value: string }> | ((args: any) => Array<{ label: string; value: string }>) }
>;

export interface OUFilterData extends CommonDropDownData {
  filtersByApplications: OUFilterByApplicationType;
}

export interface EffortInvestmentProfileFilterData extends CommonDropDownData {
  showDefaultScheme?: boolean;
  withProfileCategory?: boolean;
  categorySelectionMode?: "default" | "multiple";
  isCategoryRequired?: boolean | ((args: any) => boolean);
  categoryBEKey?: string;
  allowClearEffortInvestmentProfile?: boolean;
}

export interface LevelOpsFilter {
  id: string;
  type?: LevelOpsFilterTypes;
  label: string;
  subtitle?: string;
  tab?: WIDGET_CONFIGURATION_KEYS | WIDGET_CONFIGURATION_PARENT_KEYS;
  isFEBased?: boolean;
  beKey: string;
  parentKey?: string;
  filterInfo?: string | ((args: any) => string);
  value?: string | number | Object;
  defaultValue?: string | number | Object;
  labelCase?: "none" | "lower_case" | "upper_case" | "title_case";
  deleteSupport?: boolean;
  partialSupport?: boolean;
  partialKey?: string;
  partialFilterKey?: string;
  excludeSupport?: boolean;
  placeholder?: string;
  excludeKey?: string;
  metadata?: { [key: string]: any };
  customFilter?: Element;
  renderComponent: React.FC<any>;
  apiContainer?: React.FC<any>;
  renderConditionsComponent?: React.FC<any>;
  allFilters?: any;
  disabled?: boolean | ((args: any) => boolean);
  required?: boolean | ((args: any) => boolean);
  modifiedFilterValueChange?: (args: any) => void;
  modifyFilterValue?: (args: any) => void;
  modifiedFilterRemove?: (args: any) => void;
  getMappedValue?: (args: any) => string | number | Object | undefined;
  supportPaginatedSelect?: boolean;
  updateInWidgetMetadata?: boolean;
  helpText?: string;
  apiFilterProps?: (args: any) => {
    withSwitch?: WithSwitchProps;
    switchWithDropdown?: switchWithDropdownProps;
    withDelete?: withDeleteProps;
    allowExcludeWithPartialMatch?: boolean
  };
  isSelected?: boolean | ((args: any) => boolean);
  hideFilter?: boolean | ((args: any) => boolean);
  filterMetaData?:
    | DropDownData
    | ApiDropDownData
    | TimeRangeData
    | TagsData
    | InputData
    | SearchData
    | BinaryData
    | DateRangeData
    | InputRangeFilterData
    | StatTimeRangeFilterData
    | OUFilterData
    | CheckboxData
    | EffortInvestmentProfileFilterData
    | AdvancedSettingButton;
  isParentTab?: boolean;
  renderChildComponent?: any;
  childFilterKeys?: (param: any) => any;
  renderAddChildComponent?: any;
  childButtonLableName?: string | ((param: any) => any);
  parentKeyData?: string[];
}

export type BaseFilterConfigOptions = {
  renderComponent: React.FC<any>;
  apiContainer?: React.FC<any>;
  label: string;
  type?: LevelOpsFilterTypes;
  tab?: WIDGET_CONFIGURATION_KEYS;
  isFEBased?: boolean;
  filterInfo?: string | ((args: any) => string);
  labelCase?: "none" | "lower_case" | "upper_case" | "title_case";
  deleteSupport?: boolean;
  partialSupport?: boolean;
  partialKey?: string;
  partialFilterKey?: string;
  excludeSupport?: boolean;
  updateInWidgetMetadata?: boolean;
  excludeKey?: string;
  placeholder?: string;
  modifiedFilterValueChange?: (args: any) => void;
  modifiedFilterRemove?: (args: any) => void;
  modifyFilterValue?: (args: any) => void;
  apiFilterProps?: (args: any) => {
    withSwitch?: WithSwitchProps;
    switchWithDropdown?: switchWithDropdownProps;
    withDelete?: withDeleteProps;
  };
  disabled?: boolean | ((args: any) => boolean);
  required?: boolean | ((args: any) => boolean);
  getMappedValue?: (args: any) => string | number | Object;
  supportPaginatedSelect?: boolean;
  renderConditionsComponent?: React.FC<any>;
  isSelected?: boolean | ((args: any) => boolean);
  hideFilter?: boolean | ((args: any) => boolean);
  filterMetaData?:
    | DropDownData
    | ApiDropDownData
    | TimeRangeData
    | TagsData
    | InputData
    | SearchData
    | BinaryData
    | DateRangeData
    | InputRangeFilterData
    | StatTimeRangeFilterData
    | OUFilterData
    | CheckboxData
    | EffortInvestmentProfileFilterData;
  BEType?: string; // type for fields list api,
  isParentTab?: boolean;
  renderChildComponent?: any;
  childFilterKeys?: (param: any) => any;
  renderAddChildComponent?: any;
  childButtonLableName?: string | ((param: any) => any);
};

const defaultValues: Record<"tab" | "labelCase", any> = {
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  labelCase: "none"
};

// Used for making single filter config
export const baseFilterConfig = (key: string, options: BaseFilterConfigOptions): LevelOpsFilter => ({
  id: key,
  beKey: key,
  ...options,
  tab: options.tab ?? defaultValues["tab"],
  labelCase: options.labelCase ?? defaultValues["labelCase"],
  type: options.type ?? LevelOpsFilterTypes.DROPDOWN
});

// Used for making multiple filter config with same options but
// different key , label , tab etc.
export const baseManyFilterConfig = (
  filters: { key: string; label: string; tab?: WIDGET_CONFIGURATION_KEYS }[],
  options: BaseFilterConfigOptions
): LevelOpsFilter[] => {
  return filters.map((filter: { key: string; label: string; tab?: WIDGET_CONFIGURATION_KEYS }) =>
    baseFilterConfig(filter.key, {
      ...options,
      ...omit(filter, "key"),
      tab: filter.tab ?? options.tab
    })
  );
};

export type WidgetActionFilterType = {
  metrics: {
    datakey: string;
    options: { label: string; value: string; headerText?: string; tooltip?: string }[];
    showArrow: boolean;
    prefixLabel: string;
    defaultValue: string;
  };
};
