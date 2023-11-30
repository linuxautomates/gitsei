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

interface DemoStatsChartExtraProps {
  band: string;
  count: number;
  desc: string;
  realValue?: string;
}

const DemoStatsChartExtra = (props: DemoStatsChartExtraProps) => {
  const { count = 0, band = "LOW", desc, realValue } = props;
  let text = `${count} ${desc}`;
  if (realValue !== undefined) {
    text = `${realValue} of ${count} ${desc}`;
  }
  return (
    <div className="stats-extra-info">
      <div className="info-description">{text}</div>
      <div>
        <Tag color={BAND[band]?.color}>{band}</Tag>
        <AntText>Performance</AntText>
      </div>
    </div>
  );
};

export default DemoStatsChartExtra;
