import React from "react";
import { AntSlider } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

const marks = {
  0: "0°C",
  26: "26°C",
  37: "37°C",
  100: {
    style: {
      color: "#f50"
    },
    label: <strong>100°C</strong>
  }
};

export default {
  title: "Ant Slider"
};

export const Slider = () => <AntSlider defaultValue={30} />;

export const SliderRange = () => <AntSlider range defaultValue={[20, 50]} />;

export const SliderMarksStepWithCustomStyle = () => <AntSlider marks={marks} step={10} defaultValue={37} />;
