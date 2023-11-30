import React from "react";
import cx from "classnames";

interface ScorePopoverLegendsProps {
  asc: boolean;
  value: Array<number>;
  maxScore: number;
  unit: string;
}

const ScorePopoverLegends: React.FC<ScorePopoverLegendsProps> = (props: ScorePopoverLegendsProps) => {
  const { asc, unit, value, maxScore } = props;

  return (
    <div className={cx("slider-legend-container", { "row-reverse": !asc })}>
      <div className="slider-legend">
        <span className="slider-legend-mark poor-legend" />
        <span className="slider-label slider-label-need-improvement">Needs Improvement</span>{" "}
        <span className="slider-label slider-label-legend">
          {asc ? `< ${((value[0] / 100) * maxScore).toFixed(2)}` : `> ${((value[1] / 100) * maxScore).toFixed(2)}`}{" "}
          {unit && unit}
        </span>{" "}
      </div>
      <div className="slider-legend">
        <span className="slider-legend-mark acceptable-legend" />
        <span className="slider-label slider-label-acceptable">Acceptable</span>{" "}
        <span className="slider-label slider-label-legend">
          {`${((value[0] / 100) * maxScore).toFixed(2)} - ${((value[1] / 100) * maxScore).toFixed(2)} `} {unit && unit}
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
  );
};

export default ScorePopoverLegends;
