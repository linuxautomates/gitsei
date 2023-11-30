import React from "react";
import { Icon } from "antd";
import "./warning-status.style.scss";
import { capitalize } from "lodash";

interface WarningStatusProps {
  text?: string;
  style?: Object;
  className?: string;
  iconStyle?: Object;
  textStyle?: Object;
}

const WarningStatusComponent: React.FC<WarningStatusProps> = ({
  text = "Warning",
  style,
  className,
  iconStyle,
  textStyle
}) => {
  return (
    <div className={`warning-status ${className} flex`} style={style}>
      <Icon className="warning-status--icon flex align-center" type="info-circle" style={iconStyle} />
      <span className="warning-status--text" style={textStyle}>
        {capitalize(text)}
      </span>
    </div>
  );
};

export default React.memo(WarningStatusComponent);
