import React from "react";
import { RelativeTimeRangePayload, RelativeTimeRangeDropDownPayload } from "model/time/time-range";
import RelativeTimeRangeDropDown from "../relative-time-range-dropdown/relative-time-range-dropdown.component";

import "./relative-time-range.styles.scss";

interface RelativeTimeRangeProps {
  data: RelativeTimeRangePayload;
  onChange: (value: RelativeTimeRangePayload) => void;
  disable?: boolean;
}

const RelativeTimeRangeComponent: React.FC<RelativeTimeRangeProps> = ({ data, onChange, disable }) => {
  const _onChange = (key: "last" | "next", value: RelativeTimeRangeDropDownPayload) => {
    onChange({ ...data, [key]: value });
  };

  return (
    <div className="flex align-center relative-time-range">
      <RelativeTimeRangeDropDown
        disable={disable}
        title={"Last"}
        data={data?.last}
        onChange={value => _onChange("last", value)}
      />
      <div className="relative-time-range--to">to</div>
      <RelativeTimeRangeDropDown
        disable={disable}
        title={"Next"}
        data={data?.next}
        onChange={value => _onChange("next", value)}
      />
    </div>
  );
};

export default RelativeTimeRangeComponent;
