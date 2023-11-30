import React from "react";
import { AntText, AntTooltip } from "shared-resources/components";
import { Switch } from "antd";
import "./ToggleComponent.style.scss";
import { TOOLTIP_MESSAGE } from "./constant";
interface ToggleConfig {
  key: string;
  label: string;
  type: string;
  be_key: string;
}
interface ToggleComponentProps {
  toggleConfig: ToggleConfig;
  onchange: (value: any) => void;
  checked: boolean;
  disabled: boolean;
}

const ToggleComponent: React.FC<ToggleComponentProps> = (props: ToggleComponentProps) => {
  const { toggleConfig, onchange, checked, disabled } = props;
  const { label, be_key } = toggleConfig;

  const handleChange = (value: boolean) => {
    onchange({ [be_key]: { enabled: value } });
  };

  return (
    <div className="toggle-component-wrapper">
      <AntTooltip title={disabled && TOOLTIP_MESSAGE}>
        <Switch disabled={disabled} onChange={handleChange} className="toggle-switch" checked={checked} />
      </AntTooltip>
      <AntText>{label}</AntText>
    </div>
  );
};

export default ToggleComponent;
