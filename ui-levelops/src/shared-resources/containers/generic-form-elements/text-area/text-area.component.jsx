import React from "react";
import { Input } from "antd";

const { TextArea } = Input;
export const TextAreaWrapper = props => (
  <TextArea
    {...props}
    autoSize={{ minRows: 3, maxRows: 5 }}
    onChange={event => {
      props.onChange(event.target.value);
    }}
  />
);
