import React, { useCallback, useEffect, useMemo, useState } from "react";
import { RelativeTimeRangeDropDownPayload } from "../../../model/time/time-range";

import "./relative-time-range-dropdown.styles.scss";
import { Input, Select } from "antd";
import { RelativeTimeRangeUnits } from "../relative-time-range/constants";
import { capitalize, debounce } from "lodash";

interface RelativeTimeRangeDropdownProps {
  title: string;
  width?: number;
  data: RelativeTimeRangeDropDownPayload;
  onChange: (data: RelativeTimeRangeDropDownPayload) => void;
  disable?: boolean;
}

const RelativeTimeRangeDropDownComponent: React.FC<RelativeTimeRangeDropdownProps> = ({
  title,
  data,
  width = 200,
  onChange,
  disable
}) => {
  const [value, setValue] = useState<number | string | undefined>("");
  const style = useMemo(() => ({ width }), [width]);

  useEffect(() => {
    if (data && data.num !== value) {
      setValue(data.num);
    }
  }, [data?.num]);

  const _onChange = (key: "num" | "unit", value: number | RelativeTimeRangeUnits) => {
    onChange({ ...data, [key]: value });
  };

  const _debouncedOnChange = useCallback(debounce(_onChange, 350), [data, onChange]);

  const _onInputChange = useCallback(
    (key: "num" | "unit", value: number | RelativeTimeRangeUnits) => {
      _debouncedOnChange(key, value);
      setValue(value);
    },
    [data, onChange]
  );

  return (
    <div style={style} className="relative-time-range-dropdown">
      <span className="relative-time-range-dropdown--title">{title}</span>
      <div className="flex relative-time-range-dropdown__input-dropdown">
        {data?.unit !== RelativeTimeRangeUnits.TODAY && (
          <Input
            disabled={disable}
            value={value}
            type="number"
            min={0}
            onChange={(event: any) => _onInputChange("num", event.target.value)}
          />
        )}
        <Select
          disabled={disable}
          value={data?.unit}
          defaultValue={RelativeTimeRangeUnits.DAYS}
          onChange={(value: RelativeTimeRangeUnits) => _onChange("unit", value)}>
          {Object.values(RelativeTimeRangeUnits).map((key: string) => (
            <Select.Option key={key} value={key}>
              {capitalize(key)}
            </Select.Option>
          ))}
        </Select>
      </div>
    </div>
  );
};

export default React.memo(RelativeTimeRangeDropDownComponent);
