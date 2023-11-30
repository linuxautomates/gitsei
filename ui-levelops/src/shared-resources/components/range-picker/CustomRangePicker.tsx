import React from "react";
import { TimeRangeLimit } from "./CustomRangePickerTypes";
import { createOnRangePickerChange } from "dashboard/graph-filters/components/utils/createOnRangePickerChange";
import DateRange from "shared-resources/components/DateRange/DateRange";
import moment from "moment";

export interface CustomRangePickerProps {
  value: any;
  onChange: (value: any) => void;
  type?: "string" | "number";

  // time range limit
  maxRange?: TimeRangeLimit;
  disable?: boolean;
}

/**
 * Considering Value is in UTC, converting into local timezone
 * Output value is in UTC format
 * @param value
 */
function turnIntoMomentValue(value: any) {
  let momentValue: any[] = [];

  if (value) {
    momentValue = [
      moment.unix(parseInt(value.$gt)).subtract(moment().utcOffset(), "m"),
      moment.unix(parseInt(value.$lt)).subtract(moment().utcOffset(), "m")
    ];
  }

  return momentValue;
}

const CustomRangePicker: React.FC<CustomRangePickerProps> = (props: CustomRangePickerProps) => {
  const { value, onChange, type, maxRange, disable } = props;

  const selected = turnIntoMomentValue(value);
  const onRangePickerChange = createOnRangePickerChange(maxRange, onChange, type);

  return (
    <DateRange
      disable={disable}
      onChange={onRangePickerChange}
      value={selected}
      rangeLimit={maxRange}
      style={{ width: "100%" }}
    />
  );
};

export default CustomRangePicker;
