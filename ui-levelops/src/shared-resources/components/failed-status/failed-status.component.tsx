import React from "react";
import { Icon } from "antd";
import "./failed-status.style.scss";
import { capitalize } from "lodash";

interface FailedStatusProps {
  text?: string;
  style?: Object;
  className?: string;
  iconStyle?: Object;
  textStyle?: Object;
}

const FailedStatusComponent: React.FC<FailedStatusProps> = ({
  text = "Failed",
  style,
  className,
  iconStyle,
  textStyle
}) => {
  return (
    <div className={`failed-status ${className} flex`} style={style}>
      <Icon
        className="failed-status--icon flex align-center"
        type="exclamation-circle"
        theme="filled"
        style={{ ...iconStyle, color: "#B41710" }}
      />
      <span className="failed-status--text" style={textStyle}>
        {capitalize(text)}
      </span>
    </div>
  );
};

export default React.memo(FailedStatusComponent);
