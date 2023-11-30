import { Icon } from "antd";
import React, { FC, memo } from "react";
import "./AntWarning.scss";
import { AntText } from "..";

type AntWarningProps = {
  label: string;
};

const AntWarning: FC<AntWarningProps> = ({ label }) => {
  return (
    <div className="custom-warning-wrapper">
      <Icon type="warning" />
      <AntText>{label}</AntText>
    </div>
  );
};

export default memo(AntWarning);
