import React from "react";
import { Statistic, Typography } from "antd";

const { Text } = Typography;

export const AntStatisticComponent = props => {
  return (
    <Statistic
      title={
        <Text strong type={"secondary"} style={{ fontSize: "12px" }}>
          {props.title}
        </Text>
      }
      value={props.value}
      valueStyle={{ fontSize: "30px" }}
    />
  );
};
