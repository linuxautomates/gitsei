import React from "react";
import { Input } from "antd";

export const TextWrapper = props => (
  <Input
    {...props}
    onChange={event => {
      props.onChange(event.target.value);
    }}
  />
);
