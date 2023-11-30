import React from "react";
import { Select } from "antd";
import { AntSelectComponent as AntSelect } from "shared-resources/components/ant-select/ant-select.component";

interface SelectCommaMultiSelectProps {
  className?: string;
  value: string | string[];
  disabled?: boolean;
  onChange: (value: string) => void;
}

const CommaMultiSelectWrapper = (props: SelectCommaMultiSelectProps) => {
  let githubReposOptions: { key: string; label: string }[] = [];
  let value: string | string[] = props.value || [];

  if (props.value) {
    // also covering the case if value is array
    if (typeof props.value === "string") {
      value = props.value.split(",");
    }

    githubReposOptions = (value as string[])
      .map((item: string) => ({ key: item, label: item }))
      .filter((item: any) => item.key !== "" || item.label !== "");
  }

  return (
    <AntSelect
      className={props.className ?? ""}
      allowClear={true}
      mode="tags"
      notFoundContent={""}
      disabled={props.disabled}
      value={value}
      maxTagCount={2}
      onChange={(value: any) => props.onChange(value.length ? value.join(",") : "")}>
      {githubReposOptions.map((val: { key: string; label: string }) => (
        <Select.Option key={val?.key} value={val.key}>
          {val?.label}
        </Select.Option>
      ))}
    </AntSelect>
  );
};

export default CommaMultiSelectWrapper;
