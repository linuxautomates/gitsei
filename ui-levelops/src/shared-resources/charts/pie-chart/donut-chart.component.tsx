import React, { useCallback, useState } from "react";
import * as PropTypes from "prop-types";
import { PieChart, Pie, Cell } from "recharts";

import { lineChartColors } from "../chart-themes";
import "./donut-chart.style.scss";

import { DonutChartProps, DonutDataItem, DonutLabelProps } from "../chart-types";
import { EmptyWidget } from "../../components";
import { getRenderActiveShape } from "./renderActiveShapeHelper";
import { getDynamicColor } from "../chart-color.helper";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";

const DonutChartComponent = (props: DonutChartProps) => {
  const [activeIndex, setActiveIndex] = useState<any>(0);
  const { data, pieProps, showLegend, height, width, onClick, hasClickEvents, colorSchema } = props;
  const itemCount: number = data.length;

  const colors: Array<string> = itemCount % 5 === 1 ? lineChartColors.slice(0, -1) : lineChartColors;
  const colorCount: number = colors.length;

  const runningTotal: number = data.reduce((acc: number, cur: DonutDataItem): number => acc + cur.value, 0);

  const chartData: Array<DonutDataItem> = data.map(item => ({
    ...item,
    percentage: Math.round((item.value / runningTotal) * 100)
  }));

  const disableTooltip = props.config?.disable_tooltip;

  const DonutLabel = (props: DonutDataItem & DonutLabelProps) => {
    return (
      <ul>
        <li className="donut-list-item">
          <p className="label-text">
            <span style={{ color: props.bulletColor }}>{`● `}</span>
            {props.name}
          </p>
          <p className="label-numbers">
            {props.percentage} {`% | `}
            {props.value.toLocaleString("en-US")}
          </p>
        </li>
      </ul>
    );
  };

  const onPieClick = useCallback((data: any) => {
    const onChartClickPayload = getWidgetConstant(props.reportType as string, ["onChartClickPayload"]);
    if (data && hasClickEvents && onClick) {
      if (onChartClickPayload) {
        const args = { data, across: props.xUnit, visualization: ChartType.DONUT };
        onClick(onChartClickPayload(args));
        return;
      }
      onClick(data.tooltipPayload[0]?.name as string);
    }
  }, []);

  const getColor = useCallback((key: string, index: number) => getDynamicColor(key, index, colorSchema), [colorSchema]);

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  let _pieProps: any = { ...pieProps };
  _pieProps.innerRadius = _pieProps?.innerRadius || 76;
  _pieProps.outerRadius = _pieProps?.outerRadius || 96;

  return (
    <div className="donut-wrapper">
      <PieChart width={width || 300} height={height || 300}>
        <Pie
          onClick={onPieClick}
          data={data}
          {..._pieProps}
          startAngle={90}
          endAngle={-270}
          paddingAngle={0}
          dataKey="value"
          activeShape={!disableTooltip ? getRenderActiveShape(props.unit) : null}
          activeIndex={activeIndex}
          onMouseEnter={(_, index: number) => setActiveIndex(index)}>
          {chartData.map((entry: DonutDataItem, index: number) => (
            <Cell key={`donut-sector-${index}`} fill={getColor(entry?.name || "", index)} />
          ))}
        </Pie>
        {/*{!disableTooltip && <Tooltip cursor={false} content={renderTooltip} />}*/}
      </PieChart>

      {showLegend && (
        <div className="donut-list-wrapper">
          {chartData.map((entry: DonutDataItem, index: number) => (
            <DonutLabel {...entry} bulletColor={colors[index % colorCount]} key={`donut-label-${index}`} />
          ))}
        </div>
      )}
    </div>
  );
};

DonutChartComponent.propTypes = {
  data: PropTypes.object,
  pieProps: PropTypes.object,
  labelTitle: PropTypes.string,
  showLegend: PropTypes.bool
};

DonutChartComponent.defaultProps = {
  labelTitle: "Issues",
  showLegend: false
};

export default DonutChartComponent;
