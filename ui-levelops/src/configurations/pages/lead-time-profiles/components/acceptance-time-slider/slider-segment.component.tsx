import React, { useMemo } from "react";
import { SegmentProps } from "react-ranger";

interface SliderSegmentProps {
  value: number;
  getSegmentProps: <T>(props?: T) => T & SegmentProps;
  background?: String;
}

const SliderSegment: React.FC<SliderSegmentProps> = (props: SliderSegmentProps) => {
  const { value, getSegmentProps, background } = props;
  const tickStyle = useMemo(
    () => ({
      background
    }),
    [value]
  );

  const tickProps = useMemo(
    () => ({
      ...getSegmentProps({
        style: tickStyle
      })
    }),
    [getSegmentProps, value]
  );

  return <div className="segment" {...tickProps} />;
};

export default SliderSegment;
