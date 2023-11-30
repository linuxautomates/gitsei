import React, { useMemo } from "react";
import { AntText } from "../../../components";
import "./ColorScheme.scss";

interface ColorSchemeProps {
  range: string;
  color: string;
}

const ColorScheme: React.FC<ColorSchemeProps> = (props: ColorSchemeProps) => {
  const { range, color } = props;
  const dynamicStyle = useMemo(() => ({ backgroundColor: color }), []);
  return (
    <span key={range} className="color-scheme">
      <div className="color-dot" style={dynamicStyle} />
      <AntText className="color-value">{range}</AntText>
    </span>
  );
};

export default React.memo(ColorScheme);
