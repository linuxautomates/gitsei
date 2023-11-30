import { CHART_DATA_TRANSFORMERS, CHART_TOOLTIP_RENDER_TRANSFORM } from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import React, { useCallback, useEffect, useState } from "react";
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Sector, Tooltip, TooltipProps } from "recharts";
import { ignoreKeys } from "shared-resources/charts/bar-chart/bar-chart.ignore-keys";
import { renderTooltip } from "shared-resources/charts/bar-chart/bar-chart.tooltip";
import { getDynamicColor } from "shared-resources/charts/chart-color.helper";
import ChartLegendComponent from "shared-resources/charts/components/chart-legend/chart-legend.component";
import GenericTooltipRenderer from "shared-resources/charts/components/GenericTooltipRenderer";
import { getInitialFilters } from "shared-resources/charts/helper";
import { EmptyWidget } from "shared-resources/components";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { DemoCircleChartProps } from "./Widget-Grapg-Types/demo-circle-chart-props";

const DemoCircleChartComponent = (props: DemoCircleChartProps) => {
  const { data, id, colorSchema, onClick, reportType, hideLegend } = props;
  const [filters, setFilters] = useState<any>({});

  const [activePieSliceIndex, setActivePieSliceIndex] = React.useState();
  const dataKey = get(props, ["barProps", "0", "dataKey"], "count");
  // @ts-ignore
  const onPieEnter = (data, index) => {
    setActivePieSliceIndex(index);
  };

  // @ts-ignore
  const onPieLeave = (data, index) => {
    setActivePieSliceIndex(undefined);
  };
  // @ts-ignore
  const renderCustomizedLabel = labelParams => {
    const { cx, cy, midAngle, outerRadius, fill, endAngle, startAngle, tooltipPayload } = labelParams;
    const angle = endAngle - startAngle;
    if (angle < 25) {
      return null;
    }

    const RADIAN = Math.PI / 180;
    const sin = Math.sin(-RADIAN * midAngle);
    const cos = Math.cos(-RADIAN * midAngle);
    const sx = cx + (outerRadius + 0) * cos;
    const sy = cy + (outerRadius + 0) * sin;
    const mx = cx + (outerRadius + 20) * cos;
    const my = cy + (outerRadius + 20) * sin;
    const ex = mx + (cos >= 0 ? 1 : -1) * 22;
    const ey = my;
    const textAnchor = cos >= 0 ? "end" : "start";
    // const textAnchor = cos >= 0 ? 'start' : 'end'; // Maybe one day

    const datum = tooltipPayload[0];
    const dataName = datum.name;
    const dataValue = datum.value;

    const showLine = false;
    const bgfill = "#eee";

    return (
      <g style={{ pointerEvents: "none" }}>
        {showLine && <path d={`M${sx},${sy}L${mx},${my}L${ex},${ey}`} stroke={fill} fill="none" />}
        {showLine && <circle cx={ex} cy={ey} r={2} fill={fill} stroke="none" />}
        <defs>
          <filter x="0" y="0" width="1" height="1" id={`solid${bgfill}`} radius="3px">
            <feFlood floodColor={bgfill} />
            <feComposite in="SourceGraphic" />
          </filter>
        </defs>
        <text x={ex + (cos >= 0 ? 1 : -1) * 12} y={ey} textAnchor={textAnchor} fill={fill} fontSize="20px">
          {`${dataValue}`}
        </text>
        <text
          x={ex + (cos >= 0 ? 1 : -1) * 14}
          y={ey}
          dy={17}
          textAnchor={textAnchor}
          fill="#333"
          fontSize="12px"
          fontWeight="500"
          filter={`url(#solid${bgfill})`}>
          &nbsp;{`${dataName}`}&nbsp;
        </text>
      </g>
    );
  };

  // @ts-ignore
  const renderActivePieSlice = activePieSliceProps => {
    const { cx, cy, innerRadius, outerRadius, startAngle, endAngle, fill } = activePieSliceProps;

    return (
      <g>
        <Sector
          cx={cx}
          cy={cy}
          startAngle={startAngle}
          endAngle={endAngle}
          innerRadius={innerRadius}
          outerRadius={data.length > 1 ? outerRadius + 10 : outerRadius}
          fill={fill}
          stroke="white"
        />
      </g>
    );
  };

  const getColor = useCallback((key: string, index: number) => getDynamicColor(key, index, colorSchema), [colorSchema]);
  const tooltipContent = useCallback(
    (tooltipProps: TooltipProps) => {
      const tooltipRenderTransform = getWidgetConstant(props.reportType as any, [
        CHART_DATA_TRANSFORMERS,
        CHART_TOOLTIP_RENDER_TRANSFORM
      ]);

      if (tooltipRenderTransform) {
        return (
          <GenericTooltipRenderer
            tooltipProps={tooltipProps}
            chartProps={props}
            extraProps={{ chartType: ChartType.CIRCLE }}
          />
        );
      }
      return renderTooltip(tooltipProps, props, ChartType.CIRCLE);
    },
    [props]
  );

  useEffect(() => {
    if ((data || []).length) {
      const defaultFilterKey = get(widgetConstants, [reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(data, ignoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
  }, [data, reportType]);

  const onChartClick = (data: any) => {
    if (!data) return;
    onClick && onClick({ widgetId: id, phaseId: data.name, name: data.name, count: data.count });
  };

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <>
      <ResponsiveContainer>
        <PieChart>
          {/* <Legend 
            align="center"
            verticalAlign="bottom"
            layout="horizontal"
          />  */}
          <Pie
            isAnimationActive={false} // Needed to make renderCustomizedLabel work (???)
            activeIndex={activePieSliceIndex}
            activeShape={renderActivePieSlice}
            onClick={(pieSliceData, pieSliceIndex) => {
              onChartClick(pieSliceData);
            }}
            data={data}
            dataKey={dataKey}
            nameKey="name"
            outerRadius="80%"
            onMouseEnter={onPieEnter}
            onMouseLeave={onPieLeave}
            label={renderCustomizedLabel}
            labelLine={false}>
            {data.map((entry, index) => {
              return <Cell key={`cell-${index}`} fill={getColor((entry as any)?.name || "", index)} />;
            })}
          </Pie>
          {<Tooltip content={tooltipContent} />}
          {!hideLegend && <Legend content={<ChartLegendComponent filters={filters} setFilters={setFilters} />} />}
        </PieChart>
      </ResponsiveContainer>
    </>
  );
};

DemoCircleChartComponent.defaultProps = {};

export default DemoCircleChartComponent;
