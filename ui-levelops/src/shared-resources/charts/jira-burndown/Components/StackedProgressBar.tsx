import { Popover, Tooltip } from "antd";
import { forEach } from "lodash";
import React, { useCallback, useMemo } from "react";
import { Bar, ResponsiveContainer, XAxis, BarChart, LayoutType, YAxis, Rectangle } from "recharts";

export type BurndownBarConfigure = {
  xAxisType?: "category" | "number";
  yAxisType?: "category" | "number";
  xHide?: boolean;
  yHide?: boolean;
  orientation?: LayoutType;
  width?: number | string;
  height?: number;
  xAxisDatakey?: string;
  chartProps?: any;
  radiusFactor: number;
  rectangleHeight?: number;
};

const stackId = "BURNDOWN_STACK";

const StackedProgressBar: React.FC<{
  records: any[];
  dataKeys: string[];
  showPopOver?: boolean;
  metaData: BurndownBarConfigure;
  mapping?: any;
  PopoverContent?: React.FC<any>;
  popOverContentProps?: any;
  popOverClassName?: string;
}> = ({ records, dataKeys, metaData, mapping, PopoverContent, popOverContentProps, showPopOver, popOverClassName }) => {
  const {
    xAxisType,
    yAxisType,
    orientation,
    width,
    height,
    yHide,
    xHide,
    xAxisDatakey,
    chartProps,
    radiusFactor,
    rectangleHeight
  } = metaData;

  const getKeyColor = useCallback(
    (key: string) => {
      return mapping[key];
    },
    [mapping]
  );

  const getRadius = useCallback(
    (lastNonZeroIndex: number = -1, firstNonZeroIndex: number = -1) => {
      if (lastNonZeroIndex !== -1) {
        if (orientation === "vertical") return [0, radiusFactor, radiusFactor, 0];
        return [radiusFactor, radiusFactor, 0, 0];
      }

      if (firstNonZeroIndex !== -1) {
        if (orientation === "vertical") return [radiusFactor, 0, 0, radiusFactor];
        return [0, 0, radiusFactor, radiusFactor];
      }
    },
    [orientation, radiusFactor]
  );

  const renderBarShape = useCallback(
    props => {
      let lastNonZeroIndex = -1;
      let firstNonZeroIndex = 100005;
      forEach(dataKeys, (key: string, index: number) => {
        if (!!props[key] && props[key] !== "0") {
          if (firstNonZeroIndex === 100005) {
            firstNonZeroIndex = index;
          }
          lastNonZeroIndex = index;
        }
      });
      if (lastNonZeroIndex !== -1) {
        const isTopmostRect = getKeyColor(dataKeys[lastNonZeroIndex]) === props.fill;
        const isLowestRect = getKeyColor(dataKeys[firstNonZeroIndex]) === props.fill;
        if (isTopmostRect && isLowestRect) {
          props["radius"] = [radiusFactor, radiusFactor, radiusFactor, radiusFactor];
        } else {
          if (isTopmostRect) {
            props["radius"] = getRadius(lastNonZeroIndex);
          }
          if (isLowestRect) {
            props["radius"] = getRadius(-1, firstNonZeroIndex);
          }
        }
      }

      if (rectangleHeight) {
        props["height"] = rectangleHeight;
      }

      if (showPopOver === false) return <Rectangle {...props} />;

      return (
        <Popover
          content={
            !!PopoverContent && (
              <PopoverContent payload={props.payload} mapping={mapping} dataKeys={dataKeys} {...popOverContentProps} />
            )
          }
          overlayClassName={popOverClassName ?? ""}
          placement="right">
          <Rectangle {...props} />
        </Popover>
      );
    },
    [dataKeys, orientation, mapping]
  );

  const getBars = useMemo(() => {
    return dataKeys.map((key, index) => {
      const colorKey = key;
      return (
        <Bar
          key={key}
          dataKey={key}
          fill={mapping[colorKey]}
          stackId={stackId}
          barSize={16}
          shape={renderBarShape}
          radius={[0, 0, 0, 0]}
        />
      );
    });
  }, [dataKeys, mapping, renderBarShape]);

  const RenderXAxisTick = (props: any) => {
    const { x, y, payload, index } = props;
    let value = payload.value;
    const curSprint = (records || []).length ? records[index] : {};
    return (
      <Tooltip title={curSprint?.name}>
        <g transform={`translate(${x},${y})`}>
          <text x={0} y={0} dy={16} dx={-10} fill={mapping["completed"]} style={{ fontSize: "normal" }}>
            {value}
          </text>
        </g>
      </Tooltip>
    );
  };

  return (
    <ResponsiveContainer width={width} height={height}>
      <BarChart data={records} {...chartProps} layout={orientation}>
        <YAxis hide={yHide} type={yAxisType} />
        <XAxis dataKey={xAxisDatakey} tick={<RenderXAxisTick />} type={xAxisType} hide={xHide} />
        {getBars}
      </BarChart>
    </ResponsiveContainer>
  );
};

export default StackedProgressBar;
