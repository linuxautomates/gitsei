import React, { CSSProperties } from "react";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import "./pillbox-zoom.component.scss";

interface PillboxZoomComponentProps {
  style: CSSProperties;
  zoomValue: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
}

const PillBoxZoomComponent: React.FC<PillboxZoomComponentProps> = (props: PillboxZoomComponentProps) => {
  const { zoomValue, onZoomIn, onZoomOut, style } = props;

  return (
    <div className={"pillbox-zoom"} style={style}>
      <AntButton icon={"plus"} onClick={onZoomIn} />
      <AntText>{zoomValue}%</AntText>
      <AntButton icon={"minus"} onClick={onZoomOut} />
    </div>
  );
};

export default PillBoxZoomComponent;
