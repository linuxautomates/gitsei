import React, { useMemo, useRef, useEffect } from "react";
import { Input, Slider } from "antd";
import { cloneDeep, forEach, isEqual } from "lodash";
import { allocationGoalsParameters } from "../types/ticketCategorization.types";
import { SliderValue } from "antd/lib/slider";
import "./idealRangeSlider.styles.scss";
import { allocationGoalsMappingColors } from "../constants-new/constants";

interface IdealRangeSliderProps {
  index: number;
  handleChangeToCategory: (value: string[]) => void;
  idealRange: string[];
  categoryId: string;
}

const IdealRangeSlider: React.FC<IdealRangeSliderProps> = ({ idealRange, handleChangeToCategory, categoryId }) => {
  const min = idealRange[0],
    max = idealRange[1];

  let rangeRef = useRef<string[]>([]);

  const calculatedLeftRightAcceptableRange = useMemo(() => {
    let lengthOfRange = Math.abs(parseInt(max) - parseInt(min));
    let leftAcceptableRange = [Math.max(0, parseInt(min) - lengthOfRange).toString(), min];
    let rightAcceptableRange = [max, Math.min(100, parseInt(max) + lengthOfRange).toString()];
    return { leftAcceptableRange, rightAcceptableRange };
  }, [min, max]);

  useEffect(() => {
    if (!isEqual(rangeRef.current, [min, max])) {
      rangeRef.current = [min, max];
      const sliderRef = document.getElementsByClassName(categoryId);
      const { leftAcceptableRange, rightAcceptableRange } = calculatedLeftRightAcceptableRange;
      if (sliderRef.length) {
        let slider = sliderRef[0] as any;
        if (slider) {
          const sliderFirstChild = slider.firstChild;
          sliderFirstChild.style.backgroundImage = `linear-gradient(to right, #e33f3f 0%, #e33f3f ${leftAcceptableRange[0]}%, #fcb132 ${leftAcceptableRange[0]}%, #fcb132 ${leftAcceptableRange[1]}%,#fcb132 ${rightAcceptableRange[0]}%, #fcb132 ${rightAcceptableRange[1]}%, #e33f3f ${rightAcceptableRange[1]}%)`;
        }
      }
    }
  });

  const getMarks = useMemo(() => {
    let newMarks = { 0: "0%", 100: "100%" };
    forEach(idealRange, v => {
      if (!v) v = "0";
      newMarks = {
        ...(newMarks || {}),
        [parseInt(v)]: v + "%"
      };
    });

    const { leftAcceptableRange, rightAcceptableRange } = calculatedLeftRightAcceptableRange;
    const leftMark = isNaN(parseInt(leftAcceptableRange[0])) ? 0 : parseInt(leftAcceptableRange[0]);
    const rightMark = isNaN(parseInt(rightAcceptableRange[1])) ? 0 : parseInt(rightAcceptableRange[1]);
    newMarks = {
      ...newMarks,
      [leftMark]: leftMark + "%",
      [rightMark]: rightMark + "%"
    };

    return newMarks;
  }, [idealRange, calculatedLeftRightAcceptableRange]);

  const handleInputChange = (value: string, key: "min" | "max") => {
    let numberForm = parseInt(value);
    const reg = /^-?[0-9]*(\.[0-9]*)?$/;
    if ((!isNaN(numberForm) && reg.test(value)) || value === "") {
      if (key === "max") {
        handleChangeToCategory([min, value]);
      } else {
        handleChangeToCategory([value, max]);
      }
    }
  };

  const renderLegends = useMemo(() => {
    return (
      <div className="legend-container">
        {Object.values(allocationGoalsParameters).map(goal => {
          return (
            <div className="legend">
              <div className="legend-figure" style={{ backgroundColor: allocationGoalsMappingColors[goal] }} />
              <span className="legend-text">{goal}</span>
            </div>
          );
        })}
      </div>
    );
  }, []);

  const handleSliderValueChange = (value: SliderValue) => {
    if (Array.isArray(value)) {
      handleChangeToCategory([value[0] + "", value[1] + ""]);
    }
  };

  return (
    <div className="ideal-range-container" key={categoryId}>
      <div className="slider">
        <Slider
          range
          value={[parseInt(min), parseInt(max)]}
          marks={getMarks}
          onChange={handleSliderValueChange}
          className={categoryId}
        />
        {renderLegends}
        <p className="slider-text">Acceptable and poor ranges are computed automatically based on the ideal range.</p>
      </div>
      <div className="ideal-range-card">
        <p className="title">Ideal Range</p>
        <div className="range-input">
          <div className="text">Min</div>
          <Input value={min} onChange={(e: any) => handleInputChange(e.target.value, "min")} />
        </div>
        <div className="range-input">
          <div className="text">Max</div>
          <Input value={max} onChange={(e: any) => handleInputChange(e.target.value, "max")} />
        </div>
      </div>
    </div>
  );
};

export default IdealRangeSlider;
