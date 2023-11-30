import React from "react";
import { Form } from "antd";
import { CustomSelect } from "shared-resources/components";
import { v1 as uuid } from "uuid";
import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";

interface SelectFilterProps {
  filter: DynamicGraphFilter;
  onChange: (v: string) => void;
}

export const SelectFilter: React.FC<SelectFilterProps> = props => {
  const { filter } = props;

  const getValue = () => {
    if (filter.filterType === "multiSelect") {
      return (filter.selectedValue || []).filter((value: any) => !!value);
    }
    return filter.selectedValue;
  };

  return (
    <Form.Item key={uuid()} label={filter.label}>
      <CustomSelect
        valueKey="value"
        labelKey="label"
        createOption={false}
        labelCase={"title_case"}
        options={filter.options || []}
        mode={filter.filterType === "multiSelect" ? "multiple" : "default"}
        showArrow={true}
        value={
          filter.filterField === "completed"
            ? getValue() !== undefined
              ? getValue() === true
                ? "true"
                : "false"
              : undefined
            : getValue()
        }
        onChange={props.onChange}
        truncateOptions={true}
        placeholder={filter.label}
      />
    </Form.Item>
  );
};

export default SelectFilter;
