import * as React from "react";
import { AntSlider } from "../index";

import "./ant-custom-range-select.style.scss";
import { useMemo } from "react";
import cx from "classnames";

interface AntCustomRangeSelectProps {
  value: number[];
  onChange: (value: number[]) => void;
  defaultValue?: number[];
  max: number;
  asc: boolean;
  maxScore: number;
  unit?: string;
  slow_to_good_is_ascending?: boolean;
}

const AntCustomRangeSelect: React.FC<AntCustomRangeSelectProps> = props => {
  const { value, max, asc, maxScore, unit } = props;

  const percentage = useMemo(() => {
    const mid = (props.value[0] + props.value[1]) / 2;
    const leftPercentage = (mid / max) * 100;
    const rightPercentage = leftPercentage - 1;
    return {
      rightPercentage: `${rightPercentage}%`,
      leftPercentage: `${leftPercentage}%`
    };
  }, [props.value, props.max]);

  const sliderMark = (value: number) => (
    <div className="slider-mark flex direction-column">
      <div className="label">{value}%</div>
    </div>
  );

  const marks = useMemo(
    () => ({
      [value[0]]: {
        style: {
          marginTop: "-36px"
        },
        label: sliderMark(value[0])
      },
      [value[1]]: {
        style: {
          marginTop: "-36px"
        },
        label: sliderMark(value[1])
      }
    }),
    [value]
  );

  const backGroundStyle = asc
    ? `linear-gradient(to right, #FFA940 ${percentage.leftPercentage}, #61BA14 ${percentage.rightPercentage})`
    : `linear-gradient(to right, #61BA14 ${percentage.leftPercentage},  #FFA940 ${percentage.rightPercentage})`;

  return (
    <div className="custom-ant-select-container">
      <AntSlider
        range
        className={"custom-ant-select"}
        tooltipVisible={false}
        marks={marks}
        style={{ backgroundImage: backGroundStyle }}
        {...props}></AntSlider>
      <div className={cx("slider-legend-container", { "row-reverse": !asc })}>
        <div className="slider-legend">
          <span className="slider-legend-mark poor-legend" />
          <span className="slider-label slider-label-need-improvement">Needs Improvement</span>{" "}
          <span className="slider-label slider-label-legend">
            {asc ? `< ${((value[0] / 100) * maxScore).toFixed(2)}` : `>${((value[1] / 100) * maxScore).toFixed(2)}`}{" "}
            {unit && unit}
          </span>{" "}
        </div>
        <div className="slider-legend">
          <span className="slider-legend-mark acceptable-legend" />
          <span className="slider-label slider-label-acceptable">Acceptable</span>{" "}
          <span className="slider-label slider-label-legend">
            {`${((value[0] / 100) * maxScore).toFixed(2)} - ${((value[1] / 100) * maxScore).toFixed(2)} `}{" "}
            {unit && unit}
          </span>{" "}
        </div>
        <div className="slider-legend">
          <span className="slider-legend-mark good-legend" />
          <span className="slider-label slider-label-good">Good </span>
          <span className="slider-label slider-label-legend">
            {asc ? `> ${((value[1] / 100) * maxScore).toFixed(2)}` : `< ${((value[0] / 100) * maxScore).toFixed(2)}`}{" "}
            {unit && unit}
          </span>{" "}
        </div>
      </div>
    </div>
  );
};

export default AntCustomRangeSelect;
