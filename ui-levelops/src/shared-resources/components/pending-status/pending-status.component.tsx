import React from "react";
import { Icon } from "antd";
import "./pending-status.style.scss";

interface PendingStatusProps {
  text?: string;
  style?: Object;
  className?: string;
  iconStyle?: Object;
  textStyle?: Object;
}

const PendingStatusComponent: React.FC<PendingStatusProps> = ({
  text = "Pending",
  style,
  className,
  iconStyle,
  textStyle
}) => {
  return (
    <div className={`pending-status ${className} flex`} style={style}>
      <Icon
        className="pending-status--icon flex align-center"
        type="clock-circle"
        style={{ ...iconStyle, color: "#FF5310" }}
      />
      <span className="pending-status--text" style={textStyle}>
        {text}
      </span>
    </div>
  );
};

export default React.memo(PendingStatusComponent);
