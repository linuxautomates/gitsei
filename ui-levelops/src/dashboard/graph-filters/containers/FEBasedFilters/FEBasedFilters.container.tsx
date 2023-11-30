import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import {
  FEBasedDropDownFilterConfig,
  FEBasedFilterMap,
  FEBasedToggleFilterConfig,
  FEBasedInputFilterConfig,
  FEBasedRadioFilterConfig,
  FEBasedCheckboxConfig
} from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { WidgetTabsContext } from "dashboard/pages/context";
import { forEach, map, get } from "lodash";
import React, { useContext, useMemo } from "react";
import { sanitizeObject } from "utils/commonUtils";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "../../components/time-range-abs-rel-wrapper.component";
import { Checkbox, Form, Input, Radio } from "antd";
import {
  AntSelect,
  AntSwitch,
  AntText,
  CustomFormItemLabel,
  InputRangeFilter
} from "../../../../shared-resources/components";
import { JIRA_SCM_COMMON_PARTIAL_FILTER_KEY, PARTIAL_FILTER_KEY } from "../../../constants/filter-key.mapping";

interface FEBasedFiltersContainerProps {
  metadata: any;
  filters: any;
  report: string;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  onFilterValueChange?: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  filterFEBasedFilters: (filterConfig: FEBasedFilterMap) => FEBasedFilterMap;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const FEBasedFiltersContainer: React.FC<FEBasedFiltersContainerProps> = (props: FEBasedFiltersContainerProps) => {
  const { isVisibleOnTab } = useContext(WidgetTabsContext);

  const FEBasedFilters: FEBasedFilterMap = getWidgetConstant(props.report, FE_BASED_FILTERS, {});

  const reportHasFEBasedFilters: boolean = useMemo(() => {
    const feBasedFilters = sanitizeObject(props.filterFEBasedFilters(FEBasedFilters));
    return Object.keys(feBasedFilters).length > 0;
  }, [props.report, props.filterFEBasedFilters]);

  const isVisible = (key: string, FEBasedFilters: FEBasedFilterMap, data: any): boolean => {
    const isVisibleFunc = get(FEBasedFilters, [key, "isVisible"]);
    return isVisibleFunc ? isVisibleFunc(data) : true;
  };

  const FETimeBasedFilters: FEBasedFilterMap = useMemo(() => {
    const timeBasedFilters: FEBasedFilterMap = {};
    forEach(Object.keys(FEBasedFilters), (config: string) => {
      if (
        FEBasedFilters[config].type === WidgetFilterType.TIME_BASED_FILTERS &&
        isVisible(config, FEBasedFilters, props.filters)
      ) {
        timeBasedFilters[config] = FEBasedFilters[config];
      }
    });
    return timeBasedFilters;
  }, [props.report, props.filters]);

  const FEToggleBasedFilters: FEBasedFilterMap = useMemo(() => {
    const toggleBasedFilters: FEBasedFilterMap = {};
    Object.keys(FEBasedFilters).forEach((key: string) => {
      if (
        FEBasedFilters[key].type === WidgetFilterType.TOGGLE_BASED_FILTERS &&
        isVisible(key, FEBasedFilters, props.filters)
      ) {
        toggleBasedFilters[key] = FEBasedFilters[key];
      }
    });
    return toggleBasedFilters;
  }, [props.report, props.filters]);

  const FEDropDownBasedFilters: FEBasedFilterMap = useMemo(() => {
    const dropDownBasedFilters: FEBasedFilterMap = {};
    const feBasedFilters = sanitizeObject(props.filterFEBasedFilters(FEBasedFilters));
    Object.keys(feBasedFilters).forEach((key: string) => {
      if (
        feBasedFilters[key].type === WidgetFilterType.DROPDOWN_BASED_FILTERS &&
        isVisible(key, feBasedFilters, props.filters)
      ) {
        dropDownBasedFilters[key] = feBasedFilters[key];
      }
    });
    return dropDownBasedFilters;
  }, [props.report, props.filters, props.filterFEBasedFilters]);

  const FEInputBasedFilters: FEBasedFilterMap = useMemo(() => {
    const dropDownBasedFilters: FEBasedFilterMap = {};
    Object.keys(FEBasedFilters).forEach((key: string) => {
      if (
        FEBasedFilters[key].type === WidgetFilterType.INPUT_BASED_FILTERS &&
        isVisible(key, FEBasedFilters, props.filters)
      ) {
        dropDownBasedFilters[key] = FEBasedFilters[key];
      }
    });
    return dropDownBasedFilters;
  }, [props.report, props.filters]);

  const FECheckBoxBasedFilters: FEBasedFilterMap = useMemo(() => {
    const checkBoxBasedFilters: FEBasedFilterMap = {};
    Object.keys(FEBasedFilters).forEach((key: string) => {
      if (
        FEBasedFilters[key].type === WidgetFilterType.CHECKBOX_BASED_FILTERS &&
        isVisible(key, FEBasedFilters, props.filters)
      ) {
        checkBoxBasedFilters[key] = FEBasedFilters[key];
      }
    });
    return checkBoxBasedFilters;
  }, [props.report, props.filters]);

  const FERangeBasedFilters: FEBasedFilterMap = useMemo(() => {
    const dropDownBasedFilters: FEBasedFilterMap = {};
    Object.keys(FEBasedFilters).forEach((key: string) => {
      if (
        FEBasedFilters[key].type === WidgetFilterType.RANGE_BASED_FILTERS &&
        isVisible(key, FEBasedFilters, props.filters)
      ) {
        dropDownBasedFilters[key] = FEBasedFilters[key];
      }
    });
    return dropDownBasedFilters;
  }, [props.report, props.filters]);

  const FERadioBasedFilters: FEBasedFilterMap = useMemo(() => {
    const dropDownBasedFilters: FEBasedFilterMap = {};
    Object.keys(FEBasedFilters).forEach((key: string) => {
      if (
        FEBasedFilters[key].type === WidgetFilterType.RADIO_BASED_FILTERS &&
        isVisible(key, FEBasedFilters, props.filters)
      ) {
        dropDownBasedFilters[key] = FEBasedFilters[key];
      }
    });
    return dropDownBasedFilters;
  }, [props.report, props.filters]);

  const reportHasFETimeBasedFilters: boolean = useMemo(() => {
    return Object.keys(FETimeBasedFilters).length > 0;
  }, [FETimeBasedFilters]);

  const reportHasFEToggleBasedFilters: boolean = useMemo(() => {
    return Object.keys(FEToggleBasedFilters).length > 0;
  }, [FEToggleBasedFilters]);

  const reportHasFEDropDownBasedFilters: boolean = useMemo(() => {
    return Object.keys(FEDropDownBasedFilters).length > 0;
  }, [FEDropDownBasedFilters]);

  const reportHasFEInputBasedFilters: boolean = useMemo(() => {
    return Object.keys(FEInputBasedFilters).length > 0;
  }, [FEInputBasedFilters]);

  const reportHasFERangeBasedFilters: boolean = useMemo(() => {
    return Object.keys(FERangeBasedFilters).length > 0;
  }, [FERangeBasedFilters]);

  const reportHasFERadioBasedFilters: boolean = useMemo(() => {
    return Object.keys(FERadioBasedFilters).length > 0;
  }, [FERadioBasedFilters]);

  const reportHasFECheckBoxBasedFilters: boolean = useMemo(() => {
    return Object.keys(FECheckBoxBasedFilters).length > 0;
  }, [FECheckBoxBasedFilters]);

  const getRadioValue = (config: FEBasedRadioFilterConfig, filters: any) => {
    return config.getValue(filters, config.BE_key);
  };

  if (
    !reportHasFEBasedFilters &&
    !reportHasFEToggleBasedFilters &&
    !reportHasFEDropDownBasedFilters &&
    !reportHasFERadioBasedFilters &&
    !reportHasFECheckBoxBasedFilters
  )
    return null;

  const hasFilterValue = (keys: string[]) => {
    let hasValue = false;
    (keys || []).forEach((key: string) => {
      const val = get(props.filters, [key], null);
      const partialFilterKey = getWidgetConstant(props.report, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
      const partial_val = get(props.filters, [partialFilterKey, key], null);
      const exclude_val = get(props.filters, ["exclude", key], null);
      hasValue = hasValue || val || partial_val || exclude_val;
    });
    return hasValue;
  };

  return (
    <>
      {reportHasFEToggleBasedFilters &&
        map(FEToggleBasedFilters, (filter: FEBasedToggleFilterConfig, index) => {
          if (isVisibleOnTab(filter.configTab)) {
            return (
              <Form.Item label={filter.label} key={`toggle_${index}`}>
                <div className="flex justify-space-between align-center mt-5">
                  <span>{filter.toggleLabel}</span>
                  <AntSwitch
                    checked={props.filters[filter.BE_key]}
                    onChange={(checked: any) =>
                      props.onFilterValueChange && props.onFilterValueChange(checked, filter.BE_key)
                    }
                    value={props.filters[filter.BE_key] || filter.defaultValue}
                  />
                </div>
              </Form.Item>
            );
          }
          return null;
        })}
      {reportHasFEDropDownBasedFilters &&
        map(FEDropDownBasedFilters, (filter: FEBasedDropDownFilterConfig, index) => {
          if (isVisibleOnTab(filter.configTab)) {
            const options = filter?.optionsTransformFn
              ? filter.optionsTransformFn({ filters: props.filters })
              : filter.options;
            return (
              <Form.Item
                label={
                  <CustomFormItemLabel
                    label={filter.label}
                    withInfo={{
                      showInfo: !!filter.filterInfo,
                      description: filter.filterInfo!
                    }}
                  />
                }
                key={`toggle_${index}`}>
                <AntSelect
                  options={options}
                  mode={filter.select_mode ? filter.select_mode : "single"}
                  onChange={(option: string) => {
                    props.onFilterValueChange && props.onFilterValueChange(option, filter.BE_key);
                  }}
                  value={props.filters[filter.BE_key] || filter.defaultValue}
                />
              </Form.Item>
            );
          }
          return null;
        })}
      {reportHasFECheckBoxBasedFilters &&
        map(FECheckBoxBasedFilters, (filter: FEBasedCheckboxConfig, index) => {
          if (isVisibleOnTab(filter.configTab)) {
            return (
              <Form.Item
                label={
                  <CustomFormItemLabel
                    label={filter.label}
                    withInfo={{
                      showInfo: !!filter.filterInfo,
                      description: filter.filterInfo!
                    }}
                  />
                }
                key={`toggle_${index}`}>
                <Checkbox
                  checked={props.metadata[filter.BE_key]}
                  onChange={e => props.onMetadataChange?.(e.target.checked, filter.BE_key)}>
                  {filter.options.checkboxLabel && <AntText className="mr-1">{filter.options.checkboxLabel}</AntText>}
                </Checkbox>
              </Form.Item>
            );
          }
        })}
      {reportHasFEInputBasedFilters &&
        map(FEInputBasedFilters, (filter: FEBasedInputFilterConfig, index) => {
          if (isVisibleOnTab(filter.configTab)) {
            const options = filter?.optionsTransformFn
              ? filter.optionsTransformFn({ filters: props.filters })
              : filter.options;
            return (
              <Form.Item
                label={
                  <CustomFormItemLabel
                    label={filter.label}
                    withInfo={{
                      showInfo: !!filter.filterInfo,
                      description: filter.filterInfo!
                    }}
                  />
                }
                key={`toggle_${index}`}>
                <Input
                  onChange={(e: any) => {
                    props.onFilterValueChange && props.onFilterValueChange(e.target.value, filter.BE_key);
                  }}
                  type={filter.input_type}
                  value={props.filters[filter.BE_key] || filter.defaultValue}
                />
              </Form.Item>
            );
          }
          return null;
        })}
      {reportHasFETimeBasedFilters &&
        map(FETimeBasedFilters, (filter, index) => {
          if (isVisibleOnTab(filter.configTab)) {
            if (
              !props.onTimeRangeTypeChange ||
              !props.onTimeFilterValueChange ||
              hasFilterValue(filter.hideOnFilterValueKeys || [])
            ) {
              return null;
            }

            return (
              <TimeRangeAbsoluteRelativeWrapperComponent
                key={`FETimeBasedFilters_${index}`}
                label={filter?.label || "Last Closed Date"}
                filterKey={filter.BE_key}
                metaData={props.metadata}
                filters={props.filters}
                required={!!filter?.required}
                onFilterValueChange={(data: any, key: string) => {
                  props.onTimeFilterValueChange && props.onTimeFilterValueChange(data, key);
                }}
                onTypeChange={props.onTimeRangeTypeChange}
                onMetadataChange={props.onMetadataChange}
                dashboardMetaData={props.dashboardMetaData}
              />
            );
          }
          return null;
        })}
      {reportHasFERangeBasedFilters &&
        map(FERangeBasedFilters, (filter: FEBasedInputFilterConfig, index) => {
          if (isVisibleOnTab(filter.configTab)) {
            return (
              <InputRangeFilter
                value={props.filters[filter.BE_key] || filter.defaultValue}
                label={filter.getLabel ? filter.getLabel(props.filters) : filter.label}
                onChange={(option: any) => {
                  props.onFilterValueChange && props.onFilterValueChange(option, filter.BE_key);
                }}
              />
            );
          }
          return null;
        })}
      {reportHasFERadioBasedFilters &&
        map(FERadioBasedFilters, (filter: FEBasedRadioFilterConfig, index: number) => {
          if (isVisibleOnTab(filter.configTab)) {
            return (
              <Form.Item label={<CustomFormItemLabel label={filter.label} />} key={`radio_${index}`}>
                <Radio.Group
                  onChange={(e: any) => {
                    const query = filter.getFilter(e.target.value);
                    props.onFilterValueChange && props.onFilterValueChange(query, filter.BE_key);
                  }}
                  value={getRadioValue(filter, props.filters)}>
                  {filter.options.map((option: any) => (
                    <Radio value={option.value}>{option.label}</Radio>
                  ))}
                </Radio.Group>
              </Form.Item>
            );
          }
          return null;
        })}
    </>
  );
};

export default FEBasedFiltersContainer;
