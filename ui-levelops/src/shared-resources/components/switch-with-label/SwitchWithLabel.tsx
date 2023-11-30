import React from "react";
import { AntSwitchComponent as AntSwitch } from "../ant-switch/ant-switch.component";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import "./SwitchWithLabel.style.scss";

interface SwitchWithLabelProps {
  showSwitch: boolean;
  showSwitchText?: boolean;
  switchText?: string;
  switchValue?: boolean;
  disabled?: boolean;
  onSwitchValueChange?: (value: boolean) => void;
}

const SwitchWithLabel: React.FC<SwitchWithLabelProps> = props => {
  return (
    <div className={"flex direction-column switch-with-label"} style={{ alignItems: "center" }}>
      <AntSwitch
        data-testid="custom-form-item-label-withSwitch-switch"
        title={props?.switchText || ""}
        onChange={(checked: any) => props?.onSwitchValueChange?.(checked)}
        checked={props?.switchValue}
        disabled={props?.disabled}
        size={"small"}
      />
      {props?.showSwitchText && props?.switchText && <AntText className="action-text">{props.switchText}</AntText>}
    </div>
  );
};

export default SwitchWithLabel;
