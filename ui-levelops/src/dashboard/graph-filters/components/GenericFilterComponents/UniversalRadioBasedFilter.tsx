import { Form, Radio } from "antd";
import { getFilterValue } from "helper/widgetFilter.helper";
import { filter } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { CustomFormItemLabel, NewCustomFormItemLabel } from "shared-resources/components";

interface UniversalRadioBasedFilterProps {
  filterProps: LevelOpsFilter;
  onChildFilterRemove?: (filterpayload: any, returnUpdatedQuery?: boolean | undefined) => any;
  onModifiedFilterValueChange?: (args: any) => void;
  handleRemoveFilter: (key: string) => void;
}

const UniversalRadioBasedFilter: React.FC<UniversalRadioBasedFilterProps> = ({
  filterProps,
  onChildFilterRemove,
  handleRemoveFilter,
  onModifiedFilterValueChange
}) => {
  const {
    label,
    beKey,
    allFilters,
    defaultValue,
    filterMetaData,
    getMappedValue,
    apiFilterProps,
    modifiedFilterValueChange,
    modifiedFilterRemove
  } = filterProps;
  const { options } = filterMetaData as DropDownData;
  const value = getMappedValue?.({ allFilters }) ?? getFilterValue(allFilters, beKey, true)?.value ?? defaultValue;

  const onFilterRemove = (key: string) => {
    if (!!modifiedFilterRemove) {
      modifiedFilterRemove({ onChildFilterRemove, key });
      return;
    }
    handleRemoveFilter(key);
  };

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter: onFilterRemove
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  return (
    <>
      <Form.Item label={<NewCustomFormItemLabel label={label} {...apiFilters} />} key="radio-filter">
        <Radio.Group
          onChange={(e: any) => {
            modifiedFilterValueChange?.({ value: e.target.value, onModifiedFilterValueChange });
          }}
          value={value}>
          {(options as any).map((option: any) => (
            <Radio value={option.value}>{option.label}</Radio>
          ))}
        </Radio.Group>
      </Form.Item>
    </>
  );
};

export default UniversalRadioBasedFilter;
