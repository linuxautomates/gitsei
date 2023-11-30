import React from "react";
import { Radio } from "antd";

export const RadioGroupWrapper = props => (
  <Radio.Group
    {...props}
    onChange={event => {
      props.onChange(event.target.value);
    }}
  />
);
