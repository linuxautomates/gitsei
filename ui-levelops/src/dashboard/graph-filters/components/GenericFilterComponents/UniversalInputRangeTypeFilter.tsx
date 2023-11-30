import React, { useMemo, useState } from "react";
import { Form } from "antd";
import { DropDownData, InputData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { AntInput, NewCustomFormItemLabel } from "shared-resources/components";

interface UniversalNumericInputRangeTypeFilterWrapperProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  handleRemoveFilter: (key: string) => void;
}

const UniversalInputRangeTypeFilterWrapper: React.FC<UniversalNumericInputRangeTypeFilterWrapperProps> = props => {
  const { filterProps, onFilterValueChange } = props;
  const { label, beKey, apiFilterProps, filterMetaData, defaultValue, helpText, allFilters, getMappedValue } =
    filterProps;
  const { mapFilterValueForBE } = filterMetaData as DropDownData;
  const ageValue = getMappedValue?.({ allFilters }) ?? defaultValue;

  const [val, setVal] = useState<any>(ageValue);

  //on Press Enter
  const onHandleChange = (e: any) => {
    let _val = val;
    if (mapFilterValueForBE) {
      _val = mapFilterValueForBE(e.currentTarget.value);
    }
    return onFilterValueChange(_val, beKey);
  };

  const { type, placeholder } = filterMetaData as InputData;

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter: props.handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  return (
    <>
      <Form.Item
        key={beKey}
        label={<NewCustomFormItemLabel label={label || ""} withDelete={apiFilters.withDelete} />}
        help={helpText}>
        <AntInput
          type={type}
          placeholder={placeholder}
          value={val}
          defaultValue={defaultValue}
          className={"w-100"}
          onChange={(e: any) => setVal(e)}
          onPressEnter={(e: any) => onHandleChange(e)}
        />
      </Form.Item>
    </>
  );
};

export default UniversalInputRangeTypeFilterWrapper;
