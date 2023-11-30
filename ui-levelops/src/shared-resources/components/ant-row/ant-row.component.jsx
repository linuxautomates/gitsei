import React from "react";
import { Row } from "antd";

const AntRowComponent = props => {
  return <Row {...props} />;
};

export default React.memo(AntRowComponent);
