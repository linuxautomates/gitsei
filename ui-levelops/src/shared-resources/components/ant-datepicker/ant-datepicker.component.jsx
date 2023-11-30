import React from "react";
import { DatePicker } from "antd";

const { MonthPicker, RangePicker, WeekPicker } = DatePicker;

export const AntDatepickerComponent = props => {
  const renderPicker = () => {
    if (props.type === "date") return <DatePicker {...props} />;
    if (props.type === "range") return <RangePicker {...props} />;
    if (props.type === "month") return <MonthPicker {...props} />;
    if (props.type === "week") return <WeekPicker {...props} />;
    return <DatePicker {...props} />;
  };

  return <> {renderPicker()} </>;
};
