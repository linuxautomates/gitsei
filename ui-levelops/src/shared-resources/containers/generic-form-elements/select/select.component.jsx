import React from "react";
import { Select } from "antd";

const { Option } = Select;

export const SelectComponent = props => {
  const options = () => {
    if (!props.options) {
      return null;
    }
    return props.options.map(option => <Option key={option}>{option}</Option>);
  };
  return (
    <Select {...props} allowClear={!props.required}>
      {options()}
    </Select>
  );
};
