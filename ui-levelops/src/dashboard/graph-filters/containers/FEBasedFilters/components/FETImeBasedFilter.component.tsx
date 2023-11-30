import { Form } from "antd";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { FEBasedTimeFilterConfig } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { getModificationValue, modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { getMaxRangeFromReportType } from "dashboard/graph-filters/components/utils/getMaxRangeFromReportType";
import { get } from "lodash";
import React, { useCallback } from "react";
import { AntSelect, CustomFormItemLabel, CustomRangePicker } from "shared-resources/components";

interface FETimeBasedFilterComponentProps {
  metadata: any;
  report: string;
  value: { $gt: string | number; $lt: string | number };
  filterConfig: FEBasedTimeFilterConfig;
  onFilterValueChange?: (
    value: { $gt?: string | number; $lt?: string | number },
    type?: any,
    rangeType?: string
  ) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
}

const FETimeBasedFilterComponent: React.FC<FETimeBasedFilterComponentProps> = (
  props: FETimeBasedFilterComponentProps
) => {
  const { value, metadata, report, onFilterValueChange, filterConfig, onTimeRangeTypeChange } = props;

  const maxRange = getMaxRangeFromReportType(report);
  const application = getWidgetConstant(props.report, "application");

  const handleChange = useCallback(
    (value: string) => {
      const mappedValue = modificationMappedValues(value, filterConfig.options);
      onFilterValueChange?.(mappedValue, filterConfig.BE_key, "slicing");
    },
    [filterConfig, metadata]
  );

  const handleCheckboxChange = (
    key: string,
    checkboxValue: boolean,
    timeRangeValue: { $gt?: string | number; $lt?: string | number }
  ) => {
    onTimeRangeTypeChange?.(key, checkboxValue ? "absolute" : "slicing");

    if (!checkboxValue) {
      onFilterValueChange?.(
        getModificationValue(timeRangeValue as any, filterConfig.options) ? timeRangeValue : {},
        filterConfig.BE_key,
        "slicing"
      );
    }
  };

  const getCheckboxValue = (key: string) => {
    const choices = metadata?.range_filter_choice || {};
    return get(choices, [key], "") === "absolute";
  };

  const options = filterConfig.options
    .map(filter => ({ label: filter.label, value: filter.id }))
    .sort(stringSortingComparator("label"));

  return (
    <Form.Item
      label={
        <CustomFormItemLabel
          label={filterConfig.label}
          withCheckBoxes={{
            showCheckboxes: true,
            checkboxes: [
              {
                label: "Range",
                key: `${application}_${filterConfig.BE_key}`,
                value: getCheckboxValue(`${application}_${filterConfig.BE_key}`)
              }
            ],
            onCheckBoxChange: (key, checkboxValue) => {
              handleCheckboxChange(key, checkboxValue, value);
            }
          }}
        />
      }>
      {get(metadata, ["range_filter_choice", `${application}_${filterConfig.BE_key}`], "slicing") === "absolute" ? (
        <CustomRangePicker
          type={"string"}
          value={value}
          maxRange={maxRange}
          onChange={(value: any) => {
            onFilterValueChange?.(value || {}, filterConfig.BE_key, "absolute");
          }}
        />
      ) : (
        <AntSelect
          placeholder="<any>"
          mode={"default"}
          value={getModificationValue(value as any, filterConfig.options)}
          options={options}
          allowClear={true}
          onChange={handleChange}
        />
      )}
    </Form.Item>
  );
};

export default FETimeBasedFilterComponent;
