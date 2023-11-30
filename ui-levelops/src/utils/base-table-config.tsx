import { Tooltip } from "antd";
import React from "react";

export type columnOptions = {
  width?: string | number;
  sorter?: boolean;
  ellipsis?: boolean;
  hidden?: boolean;
  align?: "left" | "right" | "center" | undefined;
  className?: string;
};

export type timeUnits =
  | "year"
  | "years"
  | "y"
  | "month"
  | "months"
  | "M"
  | "week"
  | "weeks"
  | "w"
  | "day"
  | "days"
  | "d"
  | "hour"
  | "hours"
  | "h"
  | "minute"
  | "minutes"
  | "m"
  | "second"
  | "seconds"
  | "s"
  | "millisecond"
  | "milliseconds"
  | "ms"
  | "quarter"
  | "quarters"
  | "Q";

export const defaultValueForColumnOptions: columnOptions = {
  width: "10%",
  sorter: false,
  ellipsis: true,
  hidden: false,
  align: "left",
  className: ""
};

export const baseColumnConfig = (title: string, key: string, options?: columnOptions) => ({
  title: <Tooltip title={title}>{title}</Tooltip>,
  titleForCSV: title,
  key,
  dataIndex: key,
  width: options?.width || defaultValueForColumnOptions.width,
  sorter: options?.sorter || defaultValueForColumnOptions.sorter,
  ellipsis: options?.ellipsis ?? defaultValueForColumnOptions.ellipsis,
  hidden: options?.hidden || defaultValueForColumnOptions.hidden,
  align: options?.align || defaultValueForColumnOptions.align,
  className: options?.className || defaultValueForColumnOptions.className
});
