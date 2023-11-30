import React, { useState } from "react";
import { AntDatepickerComponent as AntDatePicker } from "shared-resources/components/ant-datepicker/ant-datepicker.component";
import { TimeRangeLimit } from "shared-resources/components/range-picker/CustomRangePickerTypes";
import { AntFormItemComponent as AntFormItem } from "shared-resources/components/ant-form-item/ant-form-item.component";
import { AntPopoverComponent as AntPopover } from "shared-resources/components/ant-popover/ant-popover.component";
import moment from "moment";
import { getStartValue, getEndValue, isDisabledDate, validateRange, getHelpText, DateFormats } from "utils/dateUtils";
import "./DateRange.style.scss";

interface DateRangeProps {
  use_labels?: boolean;
  onChange: (...args: any) => any;
  value: moment.Moment[];
  rangeLimit: TimeRangeLimit | undefined;
  style?: any;
  disable?: boolean;
}

const DateRange: React.FC<DateRangeProps> = props => {
  const { use_labels, onChange, value, rangeLimit, disable } = props;

  const start_date = getStartValue(value);
  const end_date = getEndValue(value);

  const [startIsOpen, setStartIsOpen] = useState<boolean>(false);
  const [endIsOpen, setEndIsOpen] = useState<boolean>(false);

  const onStartChange = (value: moment.Moment) => {
    if (!value && !end_date) {
      onChange([]);
    } else {
      onChange([value, end_date]);
    }
  };

  const onEndChange = (value: moment.Moment) => {
    if (!value && !start_date) {
      onChange([]);
    } else {
      onChange([start_date, value]);
    }
  };

  const isDisabledDateStart = (current_date: moment.Moment) => {
    return isDisabledDate(current_date, "start", start_date, end_date, rangeLimit).is_disabled;
  };

  const isDisabledDateEnd = (current_date: moment.Moment) => {
    return isDisabledDate(current_date, "end", start_date, end_date, rangeLimit).is_disabled;
  };

  const onOpenChange = (isOpen: boolean, picker_source: "start" | "end") => {
    if (picker_source === "start") {
      setStartIsOpen(isOpen);
    } else {
      setEndIsOpen(isOpen);
    }
  };

  const dateRender = (current_date: moment.Moment, picker_source: "start" | "end") => {
    const style = {};
    const isDisabledResult = isDisabledDate(current_date, picker_source, start_date, end_date, rangeLimit);

    const insides = (
      <div className="ant-calendar-date" style={style}>
        {current_date.date()}
      </div>
    );

    if (isDisabledResult.is_disabled) {
      return (
        <AntPopover
          overlayClassName="date-range-component__date-popover"
          content={isDisabledResult.reason}
          // trigger={['click']}
        >
          {insides}
        </AntPopover>
      );
    } else {
      return insides;
    }
  };

  return (
    <AntFormItem
      className="date-range-component__form-item"
      validateStatus={validateRange(start_date, end_date)}
      help={getHelpText(start_date, end_date)}>
      <div className="date-range-component" style={props.style}>
        <div className="date-range-component__left-side">
          <AntDatePicker
            disabled={disable}
            open={startIsOpen}
            value={start_date}
            onChange={onStartChange}
            disabledDate={isDisabledDateStart}
            dateRender={(current: moment.Moment) => dateRender(current, "start")}
            placeholder={"Start date"}
            showToday={false}
            allowClear
            onOpenChange={(isOpen: boolean) => onOpenChange(isOpen, "start")}
            style={{ width: "100%", minWidth: "none" }}
            showTime={false}
            format={DateFormats.DAY}
          />
        </div>
        <div className="date-range-component__divider" />
        <div className="date-range-component__right-side">
          <AntDatePicker
            disabled={disable}
            open={endIsOpen}
            value={end_date}
            onChange={onEndChange}
            dateRender={(current: moment.Moment) => dateRender(current, "end")}
            disabledDate={isDisabledDateEnd}
            placeholder={"End date"}
            style={{ width: "100%", minWidth: "none" }}
            onOpenChange={(isOpen: boolean) => onOpenChange(isOpen, "end")}
            allowClear
            showToday={false}
            showTime={false}
            format={DateFormats.DAY}
          />
        </div>
      </div>
    </AntFormItem>
  );
};

DateRange.defaultProps = {
  value: []
};

export default DateRange;
