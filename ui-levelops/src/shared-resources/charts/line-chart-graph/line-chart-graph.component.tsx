import React, { useCallback, useMemo } from "react";
import { LineChartDrillDownProps } from "../chart-types";
import { Line, LineChart, LineType, ResponsiveContainer, XAxis, YAxis, Label, CartesianGrid, DotProps } from "recharts";
import { chartStaticColors } from "../chart-themes";
import "./line-chart-graph.component.scss";
import { round } from "utils/mathUtils";

const LineChartGraphComponent: React.FC<LineChartDrillDownProps> = props => {
  const {
    data,
    chartProps,
    lines,
    lineType,
    xLabelValue,
    xAxisDataKey,
    yLabelValue,
    customizedDotLabel,
    onClick,
    additionalKey
  } = props;

  const CustomizedLabel = (props: any) => {
    const { x, y, stroke, value, index } = props;

    const formatValue = numberFormatter(value, true);
    if (data[index]?.additional_key === additionalKey) {
      return (
        <text x={x} y={y} dy={-8} dx={8} fill={"#404040"} fontSize={14} fontWeight={600} textAnchor="left">
          {formatValue}
        </text>
      );
    }
    return (
      <text x={x} y={y} dy={-8} dx={8} fill={"#7E7E7E"} fontSize={12} textAnchor="right">
        {formatValue}
      </text>
    );
  };

  const CustomizedCircle = (props: any) => {
    const { cx, cy, index } = props;
    if (!data[index] || data[index].value === null || data[index].value === undefined) return null;
    if (data[index].additional_key === additionalKey) {
      return (
        <svg>
          <circle cx={cx} cy={cy} r={12} fill={"#FFA333"} />
          <circle cx={cx} cy={cy} r={6} strokeWidth={2} stroke={"white"} fill={"#789FE8"} />
        </svg>
      );
    }
    return (
      <svg className="defaultCircle">
        <circle className="outerCircle" cx={cx} cy={cy} r={12} fill={""} />
        <circle className="innerCircle" cx={cx} cy={cy} r={6} strokeWidth={2} fill={"#789FE9"} />
      </svg>
    );
  };

  const linesToRender = useMemo(
    () =>
      lines.map((line, index) => {
        return (
          <Line
            dataKey={line}
            type={lineType as LineType}
            stroke="#8884d8"
            dot={<CustomizedCircle />}
            label={customizedDotLabel && <CustomizedLabel />}
            connectNulls
          />
        );
      }),
    [data, additionalKey]
  );

  const numberFormatter = (value: any, hasPercision: boolean = false) => {
    if (typeof value === "number" && value / 1000 >= 1) {
      return hasPercision ? `${round(value / 100, 2) / 10}k` : `${round(value / 1000, 2)}k`;
    } else {
      return value;
    }
  };

  const tickFormatter = useCallback(numberFormatter, []);

  const onDataClick = (data: any) => {
    if (onClick && data) {
      onClick(data.activePayload?.[0]?.payload);
    }
  };

  return (
    <ResponsiveContainer height="100%" width="100%" className="lineChartGraph">
      <LineChart data={data} onClick={onDataClick} {...chartProps} width={1400}>
        <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
        <XAxis
          dataKey={xAxisDataKey}
          stroke={chartStaticColors.axisColor}
          interval={"preserveStartEnd"}
          padding={{ right: 30 }}>
          <Label value={xLabelValue} offset={-20} position="insideBottom" />
        </XAxis>
        <YAxis
          key={`y-axis`}
          orientation={"left"}
          stroke={chartStaticColors.axisColor}
          allowDecimals={false}
          label={{ value: yLabelValue, angle: -90, position: "insideLeft" }}
          tickFormatter={tickFormatter}
          tickLine={false}
        />
        {linesToRender}
      </LineChart>
    </ResponsiveContainer>
  );
};

export default LineChartGraphComponent;
