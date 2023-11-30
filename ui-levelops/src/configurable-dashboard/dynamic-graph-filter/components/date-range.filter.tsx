import React from "react";
import { Form } from "antd";
import { v1 as uuid } from "uuid";
import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import { TimeRangeLimit } from "shared-resources/components/range-picker/CustomRangePickerTypes";
import CustomRangePicker from "shared-resources/components/range-picker/CustomRangePicker";

interface DateRangeFilterProps {
  filter: DynamicGraphFilter;
  onChange: (v: any) => void;
  maxRange?: TimeRangeLimit;
}

// Used by Levelops Assessment reports.
export const DateRangeFilter: React.FC<DateRangeFilterProps> = props => {
  const { filter, onChange, maxRange } = props;

  return (
    <Form.Item key={uuid()} label={filter.label}>
      <CustomRangePicker value={filter.selectedValue} onChange={onChange} maxRange={maxRange} />
    </Form.Item>
  );
};

export default DateRangeFilter;
