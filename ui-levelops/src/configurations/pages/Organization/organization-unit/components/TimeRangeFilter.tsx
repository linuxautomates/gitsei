import React, { useCallback, useEffect, useMemo, useState } from "react";
import { RelativeTimeRangeUnits, TimeRangeFilterType } from "shared-resources/components/relative-time-range/constants";
import { CustomRangePicker, RelativeTimeRangeDropDown } from "shared-resources/components";
import { getRelativeValueFromTimeRange, getValueFromTimeRange } from "./helpers";
import "./TimeRangeFilter.style.scss";
import { Radio } from "antd";
import moment from "moment";

interface TimeRangeFilterProps {
  value: any;
  onFilterValueChange: (value: any) => void;
}

const initialData = {
  type: "relative",
  relative: {
    unit: "days"
  },
  absolute: {
    $lt: undefined,
    $gt: undefined
  }
};

const TimeRangeFilter: React.FC<TimeRangeFilterProps> = ({ value, onFilterValueChange }) => {
  const [data, setData] = useState<any>(initialData);

  const type = useMemo(() => data?.type || "relative", [data]);

  useEffect(() => {
    if (Object.keys(value).length > 0) {
      let numOfDays = 0;
      if (value.hasOwnProperty("$age")) {
        numOfDays = Math.round(parseInt(value?.["$age"]) / 86400);
        setData({
          ...data,
          type: "relative",
          relative: { num: numOfDays, unit: RelativeTimeRangeUnits.DAYS }
        });
      } else {
        setData({
          ...data,
          type: "absolute",
          absolute: Object.keys(value).reduce((acc: any, key: string) => ({ ...acc, [key]: parseInt(value[key]) }), {})
        });
      }
    }
  }, []);

  const handleTypeChange = useCallback(
    (e: any) => {
      const value = e.target.value;
      let relative: any = data.relative;
      let absolute: any = data.absolute;
      if (value === TimeRangeFilterType.RELATIVE) {
        const updatedRelative = getRelativeValueFromTimeRange(data.absolute);
        if (updatedRelative) {
          relative = updatedRelative;
          onFilterValueChange({ $age: `${(updatedRelative?.num || 0) * 86400}` });
        }
      } else {
        if (data?.relative?.num) {
          absolute = {
            $lt: moment.utc().unix(),
            $gt: moment.utc().unix() - parseInt(data?.relative?.num) * 86400
          };
          onFilterValueChange({ $lt: absolute?.$lt?.toString(), $gt: absolute?.$gt?.toString() });
        }
      }
      setData({
        absolute,
        relative,
        type: value
      });
    },
    [data]
  );

  const handleRangeChange = useCallback(
    (key: string) => {
      return (value: any) => {
        setData({
          ...data,
          [key]: value
        });

        if (type === TimeRangeFilterType.RELATIVE) {
          const age = getValueFromTimeRange(value);
          onFilterValueChange({ $age: `${age * 86400}` });
        } else {
          onFilterValueChange({
            $lt: value?.$lt?.toString(),
            $gt: value?.$gt?.toString()
          });
        }
      };
    },
    [data]
  );

  return (
    <div className="time-range-filter">
      <div className="time-range-abs-rel">
        <div className="flex time-range-abs-rel__header">
          <span className="time-range-abs-rel__header--title">Time Range :</span>
          <Radio.Group onChange={handleTypeChange} value={type}>
            <Radio value={TimeRangeFilterType.RELATIVE}>Relative</Radio>
            <Radio value={TimeRangeFilterType.ABSOLUTE}>Absolute</Radio>
          </Radio.Group>
        </div>
        {type === TimeRangeFilterType.RELATIVE && (
          <div className="flex align-center relative-time-range">
            <RelativeTimeRangeDropDown title={"Last"} data={data?.relative} onChange={handleRangeChange("relative")} />
          </div>
        )}
        {type === TimeRangeFilterType.ABSOLUTE && (
          <CustomRangePicker type={"string"} value={data?.absolute} onChange={handleRangeChange("absolute")} />
        )}
      </div>
    </div>
  );
};

export default TimeRangeFilter;
