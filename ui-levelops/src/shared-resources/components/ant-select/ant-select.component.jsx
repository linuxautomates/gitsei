import React, { useEffect, useState } from "react";
import { Select } from "antd";

const { Option } = Select;

export const AntSelectComponent = props => {
  const [load, setLoad] = useState(1);
  useEffect(() => {
    if (props.dropdownTestingKey) {
      const dropDownDiv = document.getElementsByClassName(props.dropdownTestingKey);
      if (
        dropDownDiv?.length &&
        !dropDownDiv.item(0)?.hasAttribute("data-filterselectornamekey") &&
        !dropDownDiv.item(0)?.hasAttribute("data-filtervaluesnamekey")
      ) {
        dropDownDiv.item(0).setAttribute("data-filterselectornamekey", props.dropdownTestingKey);
        dropDownDiv.item(0).setAttribute("data-filtervaluesnamekey", props.dropdownTestingKey);
      }
    }
  });

  const makeOption = () => {
    // children will be rendered as options.
    if (props.children && props.children.length) {
      return props.children;
    }
    const data = props.options;
    if (!data) {
      return null;
    }
    // eslint-disable-next-line array-callback-return
    return data.map(key => {
      if (key) {
        return (
          <Option
            disabled={key?.disabled || false}
            key={key?.value !== undefined ? key?.value : key}
            value={key.value !== undefined ? key?.value : key}>
            {/* fail safe check */}
            {key?.label || (props?.disableLabelTransform ? key : key?.toString()?.toUpperCase())}
          </Option>
        );
      }
    });
  };

  const onOptionFilter = (value, option) => {
    if (!props?.showSearch || !props.hasOwnProperty("onOptionFilter")) {
      return true;
    }
    return props?.onOptionFilter(value, { label: option?.props?.children, value: option?.props?.value });
  };

  return (
    <Select
      filterOption={onOptionFilter}
      showSearch={!!props?.showSearch}
      onBlur={() => setLoad(prev => prev + 1)}
      dropdownClassName={props.dropdownTestingKey || ""}
      {...props}>
      {makeOption()}
    </Select>
  );
};
