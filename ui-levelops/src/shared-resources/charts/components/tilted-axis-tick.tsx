import React from "react";
import { Tooltip } from "antd";

// * CAN'T make types as per now , the component is used for every chart component
const TiltedAxisTick = (props: any) => {
  const { x, y, payload, showXAxisTooltip, xAxisTruncateLength } = props;
  let value = payload.value;

  if (props?.xAxisLabel?.length) {
    value = props?.xAxisLabel;
  }

  if (value.length > 10 && props.truncate !== false && !xAxisTruncateLength) {
    value = value.substr(0, 8).concat("...");
  }

  if (xAxisTruncateLength && value.length > xAxisTruncateLength) {
    value = value.substr(0, xAxisTruncateLength).concat("...");
  }

  const renderXAxis = (
    <g transform={`translate(${x},${y})`}>
      <text
        x={0}
        y={0}
        dy={16}
        textAnchor="end"
        fill="#666"
        transform="rotate(-49)"
        style={{ fontSize: "11px", cursor: props.cursor || "default" }}>
        {value}
      </text>
    </g>
  );

  return !!showXAxisTooltip ? <Tooltip title={payload.value}>{renderXAxis}</Tooltip> : renderXAxis;
};

export default TiltedAxisTick;
