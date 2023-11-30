import React from "react";
import AntIcon from "../ant-icon/ant-icon.component";

import "./AntInputClearButton.style.scss";

interface AntInputClearButtonProps {
  onClick?: (...args: any) => any;
}

const AntInputClearButton: React.FC<AntInputClearButtonProps> = props => {
  return (
    <div className="ant-input-clear-button">
      <AntIcon type="close-circle" theme="filled" onClick={props.onClick} />
    </div>
  );
};

export default AntInputClearButton;
