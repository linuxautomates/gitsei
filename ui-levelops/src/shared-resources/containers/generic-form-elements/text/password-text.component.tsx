import React from "react";
import { Input } from "antd";

export const PasswordTextWrapper = (props: any) => (
  <Input
    {...props}
    type={"password"}
    onChange={event => {
      props.onChange(event.target.value);
    }}
  />
);
