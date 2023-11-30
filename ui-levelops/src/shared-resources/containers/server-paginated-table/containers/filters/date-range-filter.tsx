import React, { useCallback, useMemo } from "react";
import { AntCol } from "../../../../components";
import { DatePicker } from "antd";
import { FilterHeader } from "./header/filter-header";
import { RangePickerProps, RangePickerValue } from "antd/lib/date-picker/interface";
import { DATE_RANGE_FILTER_FORMAT } from "./constants";

const { RangePicker } = DatePicker;
import moment from "moment";

interface DateRangeFilterProps extends RangePickerProps {
  filter: any;
  datePickerClassName?: string;
}

/**
 * @param props
 * @constructor
 * INPUT: DateString as input. Ex - 2021-06-18 or timestamp in UTC
 * OUTPUT: Both array of moment (in local timezone) and dateStrings ( 2021-06-18 )
 */
export const DateRangeFilter: React.FC<DateRangeFilterProps> = props => {
  const { filter } = props;

  const value = useMemo(() => {
    if (!filter.selected || !Object.keys(filter.selected || {}).length) {
      return [];
    }

    return Object.keys(filter.selected).map(key => {
      if (!filter.selected[key].toString().includes("-")) {
        // timestamp
        return moment.unix(filter.selected[key]).subtract(moment().utcOffset(), "m");
      } else {
        return moment(filter.selected[key], DATE_RANGE_FILTER_FORMAT);
      }
    });
  }, [filter]);

  const handleChange = useCallback(
    (dates: RangePickerValue, dateStrings: [string, string]) => {
      props.onChange && props.onChange(dates, dateStrings);
    },
    [props.onChange]
  );

  return (
    <AntCol className={"gutter-row"} span={filter.span ? filter.span : 8}>
      <FilterHeader label={filter.label} />
      <RangePicker
        className={props.datePickerClassName}
        onChange={handleChange}
        allowClear={true}
        // @ts-ignore
        value={value}
      />
    </AntCol>
  );
};

export default React.memo(DateRangeFilter);
