import React, { useMemo } from "react";
import { useRanger } from "react-ranger";
import SliderTick from "./slider-tick.component";
import SliderSegment from "./slider-segment.component";
import SliderHandle from "./slider-handle.component";
import "./acceptance-time-slider.styles.scss";
interface AcceptanceTimeSliderProps {
  values: any[];
  onChange: (v: any) => void;
  min?: number;
  max?: number;
  stepSize?: number;
  formatter: (v: any) => string;
}

const AcceptanceTimeSlider: React.FC<AcceptanceTimeSliderProps> = (props: AcceptanceTimeSliderProps) => {
  const { min, max, values, stepSize, onChange, formatter } = props;

  const ticksArr = useMemo(() => {
    let arr = [];
    for (let i = min || 1; i <= (max || 11); i++) {
      arr.push(i);
    }
    return arr;
  }, [min, max]);

  const { getTrackProps, ticks, segments, handles } = useRanger({
    min: min || 1,
    max: max || 7,
    stepSize: stepSize || 1,
    ticks: ticksArr,
    values: values || [],
    onChange: onChange
  });

  const renderTicks = useMemo(() => {
    return ticks.map(({ value, getTickProps }) => (
      <SliderTick value={value} getTickProps={getTickProps} showLabel={values.includes(value)} formatter={formatter} />
    ));
  }, [values, ticks, max, min]);

  const renderSegments = useMemo(() => {
    return segments.map(({ getSegmentProps }, value) => {
      const background = value === 0 ? "#61BA14" : value === 1 ? "#789FE9" : value === 2 ? "#CF1322" : "#ff6050";

      return <SliderSegment key={background} background={background} value={value} getSegmentProps={getSegmentProps} />;
    });
  }, [segments]);

  const renderHandles = useMemo(() => {
    return handles.map(({ value, active, getHandleProps }) => (
      <SliderHandle value={value} active={active} getHandleProps={getHandleProps} />
    ));
  }, [handles]);

  return (
    <div className="acceptance-time-slider">
      <div className="track" {...getTrackProps()}>
        {renderTicks}
        {renderSegments}
        {renderHandles}
      </div>
    </div>
  );
};

export default React.memo(AcceptanceTimeSlider);
