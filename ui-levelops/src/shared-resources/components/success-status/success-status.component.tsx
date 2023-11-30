import React from "react";
import { Icon } from "antd";
import "./success-status.style.scss";
import { capitalize } from "lodash";

interface SuccessStatusProps {
  text?: string;
  style?: Object;
  className?: string;
  iconStyle?: Object;
  textStyle?: Object;
}

const SuccessStatusComponent: React.FC<SuccessStatusProps> = ({
  text = "Active",
  style,
  className,
  iconStyle,
  textStyle
}) => {
  return (
    <div className={`success-status ${className} flex`} style={style}>
      <Icon
        className="success-status--icon flex align-center"
        type="check-circle"
        theme="filled"
        style={{ ...iconStyle, color: "#299B2C" }}
      />
      <span className="success-status--text" style={textStyle}>
        {capitalize(text)}
      </span>
    </div>
  );
};

export default React.memo(SuccessStatusComponent);
