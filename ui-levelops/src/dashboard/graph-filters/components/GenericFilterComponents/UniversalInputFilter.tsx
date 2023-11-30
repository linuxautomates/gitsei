import { Form } from "antd";
import { getFilterValue } from "helper/widgetFilter.helper";
import { debounce } from "lodash";
import { InputData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AntInput, NewCustomFormItemLabel } from "shared-resources/components";

interface HygieneFilterWrapperProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  handleRemoveFilter: (key: string) => void;
}

const UniversalInputFilterWrapper: React.FC<HygieneFilterWrapperProps> = props => {
  const { filterProps, onFilterValueChange } = props;
  const { label, beKey, allFilters, apiFilterProps, filterMetaData, defaultValue } = filterProps;

  const debounceUpdate = debounce(onFilterValueChange, 300);

  const { type, placeholder } = filterMetaData as InputData;

  const value = getFilterValue(allFilters, beKey, true)?.value ?? defaultValue;

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
      <Form.Item key={beKey} label={<NewCustomFormItemLabel label={label || ""} withDelete={apiFilters.withDelete} />}>
        <AntInput
          type={type}
          placeholder={placeholder}
          value={value}
          defaultValue={defaultValue}
          className={"w-100"}
          onChange={(e: number) => debounceUpdate(e, beKey)}
        />
      </Form.Item>
    </>
  );
};

export default UniversalInputFilterWrapper;
