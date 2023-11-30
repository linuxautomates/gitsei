import React from "react";
import { CartesianGrid, ErrorBar, ResponsiveContainer, Scatter, ScatterChart, Tooltip, XAxis, YAxis } from "recharts";
import { AntText, EmptyWidget } from "../../components";
import { ScatterChartProps } from "../chart-types";
import { chartStaticColors } from "../chart-themes";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import TiltedAxisTick from "../components/tilted-axis-tick";

const ScatterChartComponent = (props: ScatterChartProps) => {
  const { data, yDataKey, rangeY, unit, hasClickEvents, onClick, previewOnly, id, isDemo } = props;

  const stackedBarColors = ["#1a4fa9", "#0a9999", "#ff5630", "#ff9510", "#8280ff"];

  const onScatterClick = (data: any) => {
    if (hasClickEvents && isDemo) {
      onClick && onClick({ widgetId: id, name: data.additional_key, phaseId: data.additional_key });
      return;
    }
    const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
    if (onChartClickPayload) {
      const args = { data, across: props.xUnit };
      onClick && onClick(onChartClickPayload(args));
      return;
    }
    hasClickEvents && onClick && onClick(data.key as string);
  };
  // @ts-ignore
  const chartData = data.map(d => ({
    ...d,
    // @ts-ignore
    key: d.key || d.name,
    range_Y: [
      // @ts-ignore
      d[yDataKey] - d[rangeY[0]],
      // @ts-ignore
      d[rangeY[1]] - d[yDataKey]
    ]
  }));

  const toolTipContent = (props: any) => {
    if (props.payload.length > 0) {
      const style = {
        border: "1px solid #dcdfe4",
        boxShadow: "none",
        backgroundColor: "#fff",
        opacity: "0.9",
        minWidth: "50px",
        padding: "10px"
      };
      return (
        <div style={style}>
          <AntText strong style={{ fontSize: "12px" }}>
            {props.payload[0].payload.name}
          </AntText>
          <br />
          <AntText type={"secondary"} style={{ fontSize: "12px", color: stackedBarColors[0] }}>
            {rangeY[0]}: {props.payload[0].payload[rangeY[0]]}
          </AntText>
          <br />
          <AntText type={"secondary"} style={{ fontSize: "12px", color: "#ff7300" }}>
            {yDataKey}: {props.payload[0].payload[yDataKey]}
          </AntText>
          <br />
          <AntText type={"secondary"} style={{ fontSize: "12px", color: stackedBarColors[0] }}>
            {rangeY[1]}: {props.payload[0].payload[rangeY[1]]}
          </AntText>
        </div>
      );
    }
  };
  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <ResponsiveContainer>
      <ScatterChart margin={{ top: 20, right: 20, bottom: 50, left: 20 }}>
        <CartesianGrid horizontal={false} vertical={false} />
        {
          <XAxis
            hide={previewOnly}
            dataKey="name"
            interval={0}
            tick={<TiltedAxisTick />}
            stroke={chartStaticColors.axisColor}
          />
        }
        {
          <YAxis
            hide={previewOnly}
            dataKey={yDataKey}
            label={{ value: unit, angle: -90, position: "insideLeft" }}
            allowDecimals={false}
            stroke={chartStaticColors.axisColor}
          />
        }

        {!props.config?.disable_tooltip && (
          <Tooltip
            cursor={{ fill: "transparent" }}
            coordinate={{ x: 100, y: 140 }}
            contentStyle={{
              fontSize: "12px",
              fontWeight: "bold",
              opacity: "0.9"
            }}
            itemStyle={{
              fontSize: "12px",
              fontWeight: "normal",
              textTransform: "capitalize"
            }}
            content={toolTipContent}
          />
        )}
        <Scatter
          isAnimationActive={false}
          data={chartData}
          fill="#ff7300"
          onClick={(data: any) => onScatterClick(data)}
          cursor={"pointer"}>
          {
            // @ts-ignore
            <ErrorBar dataKey="range_Y" width={4} strokeWidth={2} stroke={stackedBarColors[0]} direction="y" />
          }
        </Scatter>
        {/*<Legend/>*/}
      </ScatterChart>
    </ResponsiveContainer>
  );
};

export default ScatterChartComponent;
