import React, { useEffect, useState, useCallback } from "react";
import { isEmpty } from "lodash";
import { default as CustomRangePicker } from "../range-picker/CustomRangePicker";
import { default as RelativeTimeRange } from "../relative-time-range/relative-time-range.component";
import { TimeRangeFilterType } from "shared-resources/components/relative-time-range/constants";
import {
  AbsoluteTimeRange,
  RelativeTimeRangePayload,
  TimeRangeAbsoluteRelativePayload
} from "../../../model/time/time-range";

import "./time-range-abs-rel.styles.scss";
import { Button, Radio } from "antd";
import { RadioChangeEvent } from "antd/lib/radio";
import { TimeRangeLimit } from "../range-picker/CustomRangePickerTypes";

const ENABLE_CLEAR_BUTTON = false;

interface TimeRangeAbsoluteRelativeProps {
  data: TimeRangeAbsoluteRelativePayload;
  onChange: (value: TimeRangeAbsoluteRelativePayload) => void;
  maxRange?: TimeRangeLimit;
  onTypeChange: (value: TimeRangeFilterType) => void;
  onClear: () => void;
  required?: boolean;
  disable?: boolean;
}

const TimeRangeAbsoluteRelativeComponent: React.FC<TimeRangeAbsoluteRelativeProps> = ({
  data,
  onChange,
  maxRange,
  onTypeChange,
  onClear,
  required,
  disable
}) => {
  const [type, setType] = useState<TimeRangeFilterType>(TimeRangeFilterType.RELATIVE);

  useEffect(() => {
    if (data && data.type !== type) {
      setType(data.type);
    }
  }, [data?.type]);

  const _onTypeChange = useCallback(
    (value: RadioChangeEvent) => {
      onTypeChange(value.target.value);
    },
    [onTypeChange, data]
  );

  const _onChange = (key: "absolute" | "relative", value: RelativeTimeRangePayload | AbsoluteTimeRange) => {
    if (key === "relative") {
      value = value as RelativeTimeRangePayload;
      value.last.num = !isEmpty(value.last.num) ? value.last.num : 0;
    }
    onChange({ ...data, [key]: value, type });
  };

  return (
    <div className="time-range-abs-rel">
      <div className="flex time-range-abs-rel__header">
        <span className="time-range-abs-rel__header--title">Time Range :</span>
        <Radio.Group disabled={disable} onChange={_onTypeChange} value={type}>
          <Radio value={TimeRangeFilterType.RELATIVE}>Relative</Radio>
          <Radio value={TimeRangeFilterType.ABSOLUTE}>Absolute</Radio>
        </Radio.Group>
        {!required && ENABLE_CLEAR_BUTTON && (
          <Button
            disabled={disable}
            className="align-self-end time-range-abs-rel__header--clear-button"
            onClick={onClear}
            type={"link"}>
            Clear
          </Button>
        )}
      </div>
      {type === TimeRangeFilterType.RELATIVE && (
        <RelativeTimeRange disable={disable} data={data?.relative} onChange={value => _onChange("relative", value)} />
      )}
      {type === TimeRangeFilterType.ABSOLUTE && (
        <CustomRangePicker
          disable={disable}
          type={"string"}
          value={data?.absolute}
          maxRange={maxRange}
          onChange={(value: AbsoluteTimeRange) => _onChange("absolute", value)}
        />
      )}
    </div>
  );
};

export default React.memo(TimeRangeAbsoluteRelativeComponent);
