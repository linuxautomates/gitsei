import React, { useState } from "react";
import { SelectProps } from "antd/lib/select";
import { Select } from "antd";

const { Option } = Select;

interface TruncatedSelectComponentProps extends SelectProps {
  options: Array<any>;
}

export const TruncatedSelectComponent = (props: TruncatedSelectComponentProps) => {
  const { options } = props;
  const [search, setSearch] = useState("");

  const truncatedOptions = () => {
    return options.filter(opt => opt.value.includes(search)).slice(0, 10);
  };

  return (
    <Select
      {...props}
      onSearch={(value: any) => {
        setSearch(value);
      }}
      onSelect={(value: any) => {
        setSearch("");
      }}>
      {truncatedOptions().map(opt => (
        <Option key={opt.value}>{opt.label}</Option>
      ))}
    </Select>
  );
};
