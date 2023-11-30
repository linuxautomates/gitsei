import React from "react";
import { Radar, RadarChart, ResponsiveContainer, PolarGrid, PolarAngleAxis, Tooltip } from "recharts";

import { RadarChartProps, ChartThemeProps } from "../chart-types";
import { chartThemes, radarColors } from "../chart-themes";
import { EmptyWidget } from "../../components";

const RadarChartComponent = (props: RadarChartProps) => {
  const { data, radarProps, config } = props;

  const radars: JSX.Element[] = radarProps.map((radar, i) => {
    let theme: ChartThemeProps = chartThemes[radar.theme];
    return <Radar dataKey={radar.dataKey} stroke={theme.stroke} fill={theme.fill} key={`radar-${i}`} />;
  });

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }
  return (
    <ResponsiveContainer>
      <RadarChart outerRadius={95} data={data}>
        <PolarGrid stroke={radarColors.polygon} />
        <PolarAngleAxis dataKey="subject" fontSize={"11px"} cx={100} cy={250} />

        {radars}

        {!config?.disable_tooltip && <Tooltip />}
      </RadarChart>
    </ResponsiveContainer>
  );
};

export default RadarChartComponent;
