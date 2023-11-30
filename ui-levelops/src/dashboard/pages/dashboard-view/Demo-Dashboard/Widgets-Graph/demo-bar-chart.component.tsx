import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import { round } from "utils/mathUtils";
import { getDynamicColor } from "shared-resources/charts/chart-color.helper";
import { lineChartColors, TiltedAxisTick } from "shared-resources/charts";
import { getInitialFilters, yAxisIdByKey } from "shared-resources/charts/helper";
import { chartTransparentStaticColors } from "shared-resources/charts/chart-themes";
import { EmptyWidget } from "shared-resources/components";
import { DemoBarChartProps } from "./Widget-Grapg-Types/demo-bar-chart.types";
import {
  ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM,
  AZURE_SPRINT_REPORTS,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  JIRA_SPRINT_REPORTS
} from "dashboard/constants/applications/names";
import GenericTooltipRenderer from "shared-resources/charts/components/GenericTooltipRenderer";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { renderTooltip } from "shared-resources/charts/bar-chart/bar-chart.tooltip";
import widgetConstants from "dashboard/constants/widgetConstants";
import ChartLegendComponent from "shared-resources/charts/components/chart-legend/chart-legend.component";
import { ignoreKeys } from "shared-resources/charts/bar-chart/bar-chart.ignore-keys";

const DemoBarChartComponent: React.FC<DemoBarChartProps> = (props: DemoBarChartProps) => {
  const {
    data,
    barProps,
    customColors,
    chartProps,
    stacked,
    unit,
    id,
    colorSchema,
    onClick,
    reportType,
    hideLegend,
    hasClickEvents
  } = props;

  const defaultBarSize: number = 30;
  const defaultGroupBarSize: number = 30;
  const [filters, setFilters] = useState<any>({});
  const allowLabelTransform = getWidgetConstant(props.reportType, [
    CHART_DATA_TRANSFORMERS,
    ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM
  ]);

  const yAxisIds = useMemo(() => {
    return [unit, "hidden"];
  }, [unit]);

  const xAxisInterval = useMemo(() => {
    return 0;
  }, []);

  const tickFormatter = useCallback(value => {
    if (typeof value === "number") {
      if (value / 1000 >= 1) {
        return `${round(value / 1000, 2)}k`;
      }
    }
    return value;
  }, []);

  const getBarColor = useCallback(
    (bar: string, index: number) => {
      return getDynamicColor(bar, index, colorSchema);
    },
    [colorSchema]
  );

  const getColorForBar = useCallback(
    (index: number) => {
      let colorsArray = lineChartColors;
      if (customColors && Array.isArray(customColors)) {
        colorsArray = [...customColors, ...lineChartColors];
      }
      return colorsArray[index % colorsArray.length];
    },
    [customColors]
  );

  useEffect(() => {
    if ((data || []).length) {
      const defaultFilterKey = get(widgetConstants, [reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(data, ignoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
  }, [data, reportType]);

  const bars = useMemo(() => {
    if (stacked) {
      return (barProps || []).map((bar: any, i: number) => {
        bar.fill =
          Object.values(JIRA_SPRINT_REPORTS).includes(props.reportType as any) ||
          Object.values(AZURE_SPRINT_REPORTS).includes(props.reportType as any)
            ? getColorForBar(i)
            : getBarColor(bar.dataKey, i);
        let yAxisId = "hidden";
        if (yAxisIds.includes(yAxisIdByKey[bar.dataKey])) {
          yAxisId = yAxisIdByKey[bar.dataKey];
        } else if (unit && yAxisIds.includes(unit)) {
          yAxisId = unit;
        }

        return (
          <Bar
            hide={filters?.[bar?.dataKey] == false}
            cursor={"pointer"}
            stackId={1}
            yAxisId={yAxisId}
            barSize={defaultBarSize}
            key={`bar-${i}`}
            {...bar}
          />
        );
      });
    }
    return (barProps || []).map((bar: any, i: number) => {
      bar.fill = getColorForBar(i);
      return (
        <Bar
          hide={filters?.[bar.dataKey] == false}
          cursor={"pointer"}
          barSize={defaultGroupBarSize}
          yAxisId={yAxisIdByKey[bar.dataKey] || unit}
          key={`bar-${i}`}
          isAnimationActive={false}
          {...bar}></Bar>
      );
    });
  }, [stacked, barProps, defaultGroupBarSize, unit, filters, getBarColor, data, getColorForBar]);

  const barChartStyle = useMemo(() => {
    return { ...chartProps?.margin, right: 5, bottom: 50 };
  }, []);

  const renderYAxises = useMemo(() => {
    return yAxisIds.map((yAxis, index) => {
      const label: any = { value: yAxis, angle: -90, position: index ? "right" : "insideLeft" };
      return (
        <YAxis
          key={`y-axis-${index}`}
          yAxisId={yAxis}
          orientation={index ? "right" : "left"}
          stroke={chartTransparentStaticColors.axisColor}
          allowDecimals={false}
          label={label}
          tickFormatter={tickFormatter}
          hide={yAxis === "hidden"}
        />
      );
    });
  }, [unit]);

  const renderXAxisTick = useCallback(
    (xAxisProps: any) => {
      let truncate = true;
      let xAxisLabel = xAxisProps?.payload?.value;

      const tickProps = {
        ...(xAxisProps || {}),
        showXAxisTooltip: false,
        truncate,
        xAxisLabel
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [data]
  );

  const onBarClick = useCallback((data: any) => {
    if (!data) return;
    hasClickEvents &&
      onClick &&
      onClick({ widgetId: id, name: data?.activeLabel ?? "", phaseId: data?.activeLabel ?? "" });
  }, []);

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
            extraProps={{ chartType: ChartType.BAR }}
          />
        );
      }
      return renderTooltip(tooltipProps, props, ChartType.BAR);
    },
    [props]
  );

  const renderLegends = useMemo(() => {
    return <ChartLegendComponent filters={filters} setFilters={setFilters} allowLabelTransform={allowLabelTransform} />;
  }, [filters, allowLabelTransform]);

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <ResponsiveContainer>
      <BarChart data={data} {...chartProps} margin={barChartStyle} onClick={onBarClick}>
        {<CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />}
        <XAxis
          hide={false}
          dataKey="name"
          stroke={chartTransparentStaticColors.axisColor}
          interval={xAxisInterval}
          minTickGap={3}
          // tickLine={false}
          tick={renderXAxisTick}
        />
        {renderYAxises}
        <ReferenceLine
          y={0}
          ifOverflow="extendDomain"
          yAxisId={yAxisIds.includes(unit) ? unit : "hidden"}
          stroke={"#8A94A5"}
        />
        <Tooltip cursor={false} content={tooltipContent} />
        <Legend content={renderLegends} />
        {bars}
        {!hideLegend && <Legend content={renderLegends} />}
      </BarChart>
    </ResponsiveContainer>
  );
};

export default DemoBarChartComponent;
