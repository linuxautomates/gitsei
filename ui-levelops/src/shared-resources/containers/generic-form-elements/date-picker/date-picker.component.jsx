import React from "react";
import { DatePicker } from "antd";

import moment from "moment";

export const DatePickerWrapper = props => {
  const { value, onChange } = props;
  const handleOnChange = val => {
    const time = val ? Math.ceil(val.valueOf() / 1000) : null;
    onChange(time);
  };
  return <DatePicker {...props} onChange={handleOnChange} value={value ? moment.unix(value) : null}></DatePicker>;
};
