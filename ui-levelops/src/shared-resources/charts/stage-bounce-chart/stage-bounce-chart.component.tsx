import {
  ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE
} from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { BAR_CHART_REF_LINE_STROKE } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get, isArray } from "lodash";
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
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { toTitleCase } from "utils/stringUtils";
import { round } from "../../../utils/mathUtils";
import { EmptyWidget } from "../../components";
import { getDynamicColor } from "../chart-color.helper";
import { chartTransparentStaticColors, lineChartColors } from "../chart-themes";
import { BarChartProps } from "../chart-types";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import GenericTooltipRenderer from "../components/GenericTooltipRenderer";
import { getFilteredData, getInitialFilters, yAxisIdByKey } from "../helper";
import { ignoreKeys } from "../bar-chart/bar-chart.ignore-keys";
import "../bar-chart/bar-chart.style.scss";
import { renderTooltip } from "../bar-chart/bar-chart.tooltip";
import TiltedAxisTick from "../components/tilted-axis-tick";

const StageBounceChartComponent = (props: BarChartProps) => {
  const {
    data,
    barProps,
    chartProps,
    stacked,
    unit,
    units,
    hasClickEvents,
    onClick,
    id,
    config,
    showNegativeValuesAsPositive,
    customColors,
    hideGrid,
    previewOnly = false,
    hideLegend,
    showDefaultLegend,
    legendPosition,
    legendProps,
    colorSchema,
    xAxisProps
  } = props;

  const defaultBarSize: number = 30;
  const defaultGroupBarSize: number = 30;
  const [filteredData, setFilteredData] = useState([]);
  const [filters, setFilters] = useState<any>({});
  const allowLabelTransform = getWidgetConstant(props.reportType, [
    CHART_DATA_TRANSFORMERS,
    ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM
  ]);

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const defaultFilterKey = get(widgetConstants, [props.reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(data, ignoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (filters && data && isArray(data)) {
      const newData = getFilteredData(data, filters);
      setFilteredData(newData);
    }
  }, [filters]); // eslint-disable-line react-hooks/exhaustive-deps

  const updatedData = useMemo(() => {
    if (props.useOrderedStacks && props.orderedBarProps) {
      return data.map((item: any) => {
        const keys: any = Object.values(item).reduce((acc: any, next: any) => {
          if (next) {
            return {
              ...(acc || {}),
              [next.key]: next.value
            };
          }
          return acc;
        }, {});

        return {
          name: item.name,
          ...(keys || {})
        };
      });
    }

    return filteredData;
  }, [data, filteredData, props.useOrderedStacks, props.orderedBarProps]);

  const yAxisIds = useMemo(() => {
    if (units && units.length) {
      return [...units, "hidden"];
    }
    return [unit, "hidden"];
  }, [unit, units]);

  const xAxisInterval = useMemo(() => {
    return get(xAxisProps, ["interval"], 0);
  }, [xAxisProps]);

  const onBarClick = useCallback((data: any, stage?: string) => {
    const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
    if (data && hasClickEvents && onClick) {
      if (onChartClickPayload) {
        const args = { data, across: props.xUnit, stage };
        onClick(onChartClickPayload(args));
      } else {
        onClick(data.activeLabel as string);
      }
    }
  }, []);

  const tickFormatter = useCallback(value => {
    if (typeof value === "number") {
      if (showNegativeValuesAsPositive) {
        value = Math.abs(value);
      }
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
            extraProps={{ chartType: ChartType.BAR }}
          />
        );
      }
      return renderTooltip(tooltipProps, props, ChartType.BAR);
    },
    [props]
  );

  const legendFormatter = useCallback(
    (value, entry, index) => <span className="legend-label">{toTitleCase(value)}</span>,
    []
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

  const bars = useMemo(() => {
    if (stacked) {
      return (barProps || []).map((bar, i: number) => {
        bar.fill = getBarColor(bar.dataKey, i);
        let yAxisId = "hidden";
        if (units?.length && yAxisIds.includes(yAxisIdByKey[bar.dataKey])) {
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
            onClick={(args: any) => onBarClick(args, bar.stackId)}
            {...bar}
          />
        );
      });
    }
    return (barProps || []).map((bar, i: number) => {
      bar.fill = getColorForBar(i);
      return (
        <Bar
          hide={filters?.[bar?.dataKey] == false}
          cursor={"pointer"}
          barSize={defaultGroupBarSize}
          yAxisId={yAxisIdByKey[bar.dataKey] || unit}
          key={`bar-${i}`}
          onClick={(args: any) => onBarClick(args, bar.stackId)}
          {...bar}
        />
      );
    });
  }, [
    stacked,
    barProps,
    defaultGroupBarSize,
    unit,
    units,
    filters,
    getBarColor,
    filteredData,
    getColorForBar,
    props.reportType
  ]);

  const barChartStyle = useMemo(() => {
    if (units && units.length > 1) {
      return { ...(chartProps?.margin || {}), right: 15, bottom: 50 };
    }
    return { ...chartProps?.margin, right: 5, bottom: 50 };
  }, [units]);

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
          hide={yAxis === "hidden" || previewOnly}
        />
      );
    });
  }, [unit, units]);

  // * USE this as it is if we want to transform the xaxis for other charts
  const renderXAxisTick = useCallback(
    (xAxisProps: any) => {
      const dataIndex = xAxisProps?.payload?.index;
      const dataPayload = filteredData?.[dataIndex] || {};
      const xAxisTitleTransformer = getWidgetConstant(props.reportType, [
        CHART_DATA_TRANSFORMERS,
        CHART_X_AXIS_TITLE_TRANSFORMER
      ]);
      let truncate = true;
      const xAxisTruncate = getWidgetConstant(
        props.reportType,
        [CHART_DATA_TRANSFORMERS, CHART_X_AXIS_TRUNCATE_TITLE],
        true
      );
      if (typeof xAxisTruncate === "boolean") {
        truncate = xAxisTruncate;
      }
      if (typeof xAxisTruncate === "function") {
        truncate = xAxisTruncate?.({ data: dataPayload }) ?? true;
      }
      if (props.hasTrendLikeData) {
        truncate = false;
      }
      let xAxisLabel = xAxisProps?.payload?.value;
      if (xAxisTitleTransformer) {
        xAxisLabel = xAxisTitleTransformer(xAxisLabel, dataPayload, "name");
      }
      const tickProps = {
        ...(xAxisProps || {}),
        showXAxisTooltip: config?.showXAxisTooltip,
        truncate,
        xAxisLabel
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [filteredData, props.reportType, config, props.xUnit, props.hasTrendLikeData]
  );

  const renderLegends = useMemo(() => {
    return <ChartLegendComponent filters={filters} setFilters={setFilters} allowLabelTransform={allowLabelTransform} />;
  }, [filters, allowLabelTransform]);

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <ResponsiveContainer>
      <BarChart data={updatedData} {...chartProps} margin={barChartStyle}>
        {!hideGrid && <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />}
        <XAxis
          hide={previewOnly}
          dataKey="name"
          stroke={chartTransparentStaticColors.axisColor}
          interval={xAxisInterval}
          minTickGap={3}
          tick={renderXAxisTick}
        />
        {renderYAxises}
        <ReferenceLine
          y={0}
          ifOverflow="extendDomain"
          yAxisId={yAxisIds.includes(unit) ? unit : "hidden"}
          stroke={getWidgetConstant(props.reportType, [BAR_CHART_REF_LINE_STROKE], "#8A94A5")}
        />
        {!config?.disable_tooltip && (data || []).length > 0 && <Tooltip cursor={false} content={tooltipContent} />}
        {showDefaultLegend && (
          <Legend verticalAlign={legendPosition || "bottom"} {...(legendProps || {})} formatter={legendFormatter} />
        )}
        {!showDefaultLegend && !hideLegend && <Legend content={renderLegends} />}
        {bars}
      </BarChart>
    </ResponsiveContainer>
  );
};

export default StageBounceChartComponent;
