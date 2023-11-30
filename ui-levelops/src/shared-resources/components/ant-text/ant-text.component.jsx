import React from "react";
import { Typography } from "antd";
const { Text } = Typography;
export const AntTextComponent = props => {
  return <Text {...props} />;
};

export default React.memo(AntTextComponent);
