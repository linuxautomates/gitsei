import { Row, Icon } from "antd";
import React from "react";
import AntText from "./../ant-text/ant-text.component";
import "./empty-api-error-widget.style.scss";

interface EmptyApiErrorWidgetProps {
  minHeight?: string;
  className?: string;
  description?: JSX.Element;
}

const EmptyApiErrorWidgetComponent: React.FC<EmptyApiErrorWidgetProps> = ({ minHeight, description, className }) => (
  <div className={className || "api-error"}>
    <Row align="middle" type="flex" justify="center" style={{ height: minHeight || "20%" }}>
      <Icon type="warning" theme={"filled"} style={{ fontSize: "34px" }} />
    </Row>
    <Row className="api-error-text" align="middle" type="flex" justify="center" style={{ height: minHeight || "20%" }}>
      {description}
    </Row>
  </div>
);

export default EmptyApiErrorWidgetComponent;
