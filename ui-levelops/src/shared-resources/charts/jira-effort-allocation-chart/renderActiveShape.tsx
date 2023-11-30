import { Sector } from "recharts";
import React from "react";

export const renderActiveShape = (data: any[]) => {
  return (props: any) => {
    const RADIAN = Math.PI / 180;
    const { cx, cy, midAngle, innerRadius, outerRadius, startAngle, endAngle, fill, payload, percent, value } = props;
    const sin = Math.sin(-RADIAN * midAngle);
    const cos = Math.cos(-RADIAN * midAngle);
    const sx = cx + (outerRadius + 7) * cos;
    const sy = cy + (outerRadius + 7) * sin;
    const mx = cx + (outerRadius + 22) * cos;
    const my = cy + (outerRadius + 22) * sin;
    const ex = mx + (cos >= 0 ? 1 : -1) * 16;
    const ey = my;
    const textAnchor = cos >= 0 ? "start" : "end";

    const lengthAdjust = payload?.name?.length > 17 ? "spacingAndGlyphs" : "";

    const allAvg = data?.reduce((acc: any, next: any) => {
      if (next.value) {
        return acc + next.value;
      }
      return acc;
    }, 0);

    return (
      <g>
        {!!lengthAdjust && (
          <>
            <text x={cx} y={cy} dy={2} textAnchor="middle" textLength={150} lengthAdjust={lengthAdjust}>
              {(allAvg / data.length).toFixed(2)}
            </text>
            <text x={cx} y={cy} dy={28} textAnchor="middle" textLength={150} lengthAdjust={lengthAdjust}>
              Engineers
            </text>
            <text x={cx} y={cy} dy={48} textAnchor="middle" textLength={150} lengthAdjust={lengthAdjust}>
              (avg.)
            </text>
          </>
        )}
        {!lengthAdjust && (
          <>
            <text x={cx} y={cy} dy={2} textAnchor="middle">
              {(allAvg / data.length).toFixed(2)}
            </text>
            <text x={cx} y={cy} dy={28} textAnchor="middle">
              Engineers
            </text>
            <text x={cx} y={cy} dy={48} textAnchor="middle">
              (avg.)
            </text>
          </>
        )}
        <Sector
          cx={cx}
          cy={cy}
          innerRadius={innerRadius}
          outerRadius={outerRadius}
          startAngle={startAngle}
          endAngle={endAngle}
          fill={fill}
        />
      </g>
    );
  };
};
