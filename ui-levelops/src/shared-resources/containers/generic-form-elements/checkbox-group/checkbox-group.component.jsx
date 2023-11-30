import React from "react";
import { Checkbox } from "antd";

export const CheckboxGroupWrapper = props => (
  <Checkbox.Group
    {...props}
    onChange={value => {
      props.onChange(value);
    }}
  />
);
