import React, { useCallback } from "react";
import { Icon } from "@harness/uicore";
import { PieChart, Pie, Cell, TooltipProps, Tooltip } from "recharts";
import { DonutDataItem, EffortInvestmentDonutChartProps } from "shared-resources/charts/chart-types";
import { AntIcon, AntText, EmptyWidget } from "shared-resources/components";
import "./donutChartComponent.scss";
import cx from "classnames";
import { Tag } from "antd";
import { get } from "lodash";

const NewDonutChartComponent: React.FC<EffortInvestmentDonutChartProps> = (props: EffortInvestmentDonutChartProps) => {
  const {
    data,
    pieProps,
    height,
    width,
    showTooltip,
    colorsData,
    activeKey,
    setActiveKey,
    unit,
    donutUnit,
    averageEngineers
  } = props;

  const runningTotal: number = data.reduce((acc: number, cur: DonutDataItem): number => acc + cur.value, 0);

  const chartData: Array<DonutDataItem> = data.map((item: any) => ({
    ...item,
    percentage: Math.round((item.value / runningTotal) * 100)
  }));

  const renderTooltip = useCallback(
    (tooltipProps: TooltipProps) => {
      if (tooltipProps.active && activeKey) {
        const activeKeyData: any = get(tooltipProps, ["payload"], []).find((item: any) => item.name === activeKey);
        return (
          <div className="tool-tip-wrapper p-10">
            <div className="tooltip-header">
              <AntText>{activeKeyData?.name}</AntText>
              <Tag style={{ color: "#FFFFFF", backgroundColor: `${colorsData[activeKey]}`, alignSelf: "flex-end" }}>
                {activeKeyData?.value?.toFixed(2)} %
              </Tag>
            </div>
            <hr className="tooltip-divider" />
            <AntIcon className="tooltip-icon mb-10 mt-10" type="team" theme="outlined" />
            <AntText className="tooltip-content-text">{activeKeyData?.value?.toFixed(2)}</AntText>
            <AntText>{unit}</AntText>
          </div>
        );
      }
    },
    [props, activeKey, colorsData]
  );

  const svgContent = (
    <g>
      {
        <>
          <Icon size={14} name="main-user-groups" />
          <text x={150} y={155} dy={2} textAnchor="middle" fontSize="30px" textLength={65}>
            {averageEngineers?.toFixed(2)}
          </text>
          <text x={150} y={185} dy={2} textAnchor="middle" textLength={65}>
            {donutUnit}
          </text>
          <text x={150} y={200} dy={2} textAnchor="middle" textLength={40}>
            {"(avg.)"}
          </text>
        </>
      }
    </g>
  );

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  let _pieProps: any = { ...pieProps };
  _pieProps.innerRadius = _pieProps?.innerRadius || 76;
  _pieProps.outerRadius = _pieProps?.outerRadius || 96;

  const onMouseLeave = () => {
    setActiveKey(undefined);
  };

  const onMouseEnter = (value: any, index: number) => {
    const dataKey = data[index]?.name || "";
    setActiveKey(dataKey);
  };
  return (
    <div className="new-donut-wrapper">
      <PieChart width={width || 300} height={height || 300}>
        <Pie
          data={data}
          {..._pieProps}
          startAngle={90}
          endAngle={-270}
          paddingAngle={0}
          dataKey="value"
          onMouseLeave={onMouseLeave}
          onMouseEnter={onMouseEnter}>
          {chartData.map((entry: DonutDataItem, index: number) => (
            <Cell
              className={cx("noclass", { "donut-pie-cell": activeKey && entry.name !== activeKey })}
              key={`donut-sector-${index}`}
              fill={colorsData[entry.name as any]}
            />
          ))}
        </Pie>
        {svgContent}
        {showTooltip && <Tooltip cursor={false} content={renderTooltip} />}
      </PieChart>
    </div>
  );
};

NewDonutChartComponent.defaultProps = {
  labelTitle: "Issuesws",
  showLegend: false
};

export default NewDonutChartComponent;
