import React, { useMemo } from "react";
import { TrackProps } from "react-ranger";
import { toTitleCase } from "utils/stringUtils";

interface SliderTickProps {
  value: number;
  getTickProps: <T>(props?: T) => T & TrackProps;
  showLabel?: boolean;
  formatter: (v: any) => string;
}

const SliderTick: React.FC<SliderTickProps> = (props: SliderTickProps) => {
  const { value, getTickProps, showLabel, formatter } = props;

  const tickProps = useMemo(() => ({ ...getTickProps() }), [getTickProps]);

  return (
    <div className="tick" {...tickProps}>
      {showLabel && <div className="tick-label">{toTitleCase(formatter(value))}</div>}
    </div>
  );
};

export default SliderTick;
