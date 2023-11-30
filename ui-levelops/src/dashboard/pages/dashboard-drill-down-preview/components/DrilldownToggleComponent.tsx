import React from "react";
import { AntSwitch, AntText } from "shared-resources/components";
import "./DrilldownToggleComponent.scss";
interface DrilldownToggleProps {
  title: string;
  value: boolean;
  onChange: (value: boolean) => void;
}

const DrilldownToggle: React.FC<DrilldownToggleProps> = props => {
  const { value, onChange, title } = props;
  return (
    <div className="drilldown-toggle-switch-wrapper">
      <AntSwitch onChange={onChange} checked={value} />
      <AntText className="switch-title">{title}</AntText>
    </div>
  );
};

export default DrilldownToggle;
