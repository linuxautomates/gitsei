import React from "react";
import { Tag } from "antd";
import { AntText } from "shared-resources/components";

const BAND: any = {
  ELITE: {
    color: "#61BA14"
  },
  HIGH: {
    color: "#789FE9"
  },
  LOW: {
    color: "#D4380D"
  }
};

interface StatsChartExtraProps {
  band: string;
  count: number;
  desc: string;
  realValue?: string;
}

const StatsChartExtra = (props: StatsChartExtraProps) => {
  const { count = 0, band = "LOW", desc, realValue } = props;
  let value = `${count} `;
  if (realValue !== undefined) {
    value = `${realValue} of ${count} `;
  }
  return (
    <div className="stats-extra-info">
      <div className="info-description">
        <AntText className="bold">{value}</AntText>
        <AntText>{desc}</AntText>
      </div>
      <div>
        <Tag color={BAND[band]?.color}>{band}</Tag>
        <AntText>Performance</AntText>
      </div>
    </div>
  );
};

export default StatsChartExtra;
