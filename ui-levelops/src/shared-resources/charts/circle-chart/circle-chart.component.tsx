import React, { useCallback, useEffect, useMemo } from "react";
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Sector, Tooltip, TooltipProps } from "recharts";
import { get } from "lodash";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { SCM_PRS_REPORTS, getFilteredData, getInitialFilters } from "../helper";
import { CircleChartProps } from "../chart-types";
import { renderTooltip } from "../bar-chart/bar-chart.tooltip";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ignoreKeys } from "../bar-chart/bar-chart.ignore-keys";
import { EmptyWidget } from "../../components";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import { CHART_DATA_TRANSFORMERS, CHART_TOOLTIP_RENDER_TRANSFORM } from "dashboard/constants/applications/names";
import GenericTooltipRenderer from "../components/GenericTooltipRenderer";
import { getDynamicColor } from "../chart-color.helper";

const CircleChartComponent = (props: CircleChartProps) => {
  const { data, id, config, previewOnly, hideLegend, colorSchema } = props;
  const [filteredData, setFilteredData] = React.useState([]);
  const [activePieSliceIndex, setActivePieSliceIndex] = React.useState();

  const [filters, setFilters] = React.useState<any>([]);
  const dataKey = props.barProps[0]?.dataKey;

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const defaultFilterKey = get(widgetConstants, [props.reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(data, ignoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  useEffect(() => {
    if (filters) {
      const newData = getFilteredData(data, filters);
      setFilteredData(newData as any);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  // @ts-ignore
  const renderCustomizedLabel = labelParams => {
    const { cx, cy, midAngle, outerRadius, fill, endAngle, startAngle, tooltipPayload } = labelParams;

    // Only do fancy labels for big pie slices.
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
        <text
          x={ex + (cos >= 0 ? 1 : -1) * 12}
          y={ey}
          textAnchor={textAnchor}
          fill={fill}
          fontSize="20px"
          // style={{
          //   textShadow: "1px 1px 3px white",
          // }}
        >
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
          filter={`url(#solid${bgfill})`}
          // style={{
          //   paintOrder: "stroke",
          //   stroke: "white",
          //   strokeWidth: "4px",
          //   strokeLinecap: "butt",
          //   strokeLinejoin: "miter",
          //   // textShadow: "1px 1px 3px white",
          // }}
        >
          &nbsp;{`${dataName}`}&nbsp;
        </text>
      </g>
    );
  };

  // @ts-ignore
  const onPieEnter = (data, index) => {
    setActivePieSliceIndex(index);
  };

  // @ts-ignore
  const onPieLeave = (data, index) => {
    setActivePieSliceIndex(undefined);
  };

  const tooltipContent = useCallback(
    (tooltipProps: TooltipProps) => {
      const tooltipRenderTransform = getWidgetConstant(props.reportType, [
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
          outerRadius={filteredData.length > 1 ? outerRadius + 10 : outerRadius}
          fill={fill}
          stroke="white"
        />
      </g>
    );
  };

  const getColor = useCallback((key: string, index: number) => getDynamicColor(key, index, colorSchema), [colorSchema]);

  const noDataToCreate = useMemo(() => {
    return (data || []).filter((item: any) => item?.[dataKey] > 0).length === 0;
  }, [data, dataKey]);

  if ((data || []).length === 0 || noDataToCreate) {
    return <EmptyWidget />;
  }

  const onChartClick = (data: any, props: any, chart_type: ChartType) => {
    const { reportType, hasClickEvents, onClick } = props;
    const onChartClickPayload = getWidgetConstant(reportType, ["onChartClickPayload"]);
    if (onChartClickPayload) {
      const args = { data, across: props.xUnit, chart_type: chart_type };
      onClick(onChartClickPayload(args));
      return;
    }
    if (SCM_PRS_REPORTS.includes(reportType)) {
      const name = data?.name ?? data?.additional_key ?? data.key;
      return { name: name, id: data.key };
    } else if (reportType?.includes("levelops")) {
      const _data = data?.activePayload?.[0]?.payload || {};
      data && hasClickEvents && onClick && onClick({ name: _data.name, id: _data.id });
    } else if (chart_type === ChartType.CIRCLE) {
      if (data && hasClickEvents && onClick) {
        const key = data.key;
        onClick(key);
      }
    } else {
      data && hasClickEvents && onClick && onClick(data.activeLabel as string);
    }
  };
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
              onChartClick(pieSliceData, props, ChartType.CIRCLE);
            }}
            data={filteredData}
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
          {!props.config?.disable_tooltip && <Tooltip content={tooltipContent} />}
          {!hideLegend && <Legend content={<ChartLegendComponent filters={filters} setFilters={setFilters} />} />}
        </PieChart>
      </ResponsiveContainer>
    </>
  );
};

CircleChartComponent.defaultProps = {};

export default CircleChartComponent;
