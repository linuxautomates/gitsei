import {
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE,
  HIDE_TOTAL_TOOLTIP
} from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get, isNumber } from "lodash";
import { config } from "process";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  TooltipPayload,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import { EmptyWidget } from "shared-resources/components";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { toTitleCase } from "../../../utils/stringUtils";
import { ignoreKeys, nonDataKeys } from "../bar-chart/bar-chart.ignore-keys";
import "../bar-chart/bar-chart.style.scss";
import { chartStaticColors, chartTransparentStaticColors, lineChartColors } from "../chart-themes";
import { LineChartProps } from "../chart-types";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import GenericTooltipRenderer from "../components/GenericTooltipRenderer";
import {
  formatTooltipValue,
  getInitialFilters,
  getShowDotData,
  SHOW_DOT,
  sortTooltipListItems,
  yAxisIdByKey
} from "../helper";
import { round } from "utils/mathUtils";
import TiltedAxisTick from "../components/tilted-axis-tick";

const LineChartComponent: React.FC<LineChartProps> = props => {
  const {
    data,
    chartProps,
    lineProps,
    unit,
    units,
    hasClickEvents,
    onClick,
    id,
    tooltipTitle,
    hideLegend,
    showDefaultLegend,
    colorSchema,
    alwaysShowDot,
    showYaxis
  } = props;
  const [filteredData, setFilteredData] = useState<any[]>([]);
  const [chartData, setChartData] = useState<any[]>([]);
  const [filters, setFilters] = useState<any>({});

  const yAxisIds = useMemo(() => {
    if (units && units.length) {
      return [...units, "hidden"];
    }
    return [unit, "hidden"];
  }, [unit, units]);

  useEffect(() => {
    if (data && data.length) {
      const _data = data.map((item: any, index) => {
        const dataKeys = Object.keys(item).filter((key: any) => !nonDataKeys.includes(key));
        const dotsData = getShowDotData(data, dataKeys, index);
        return {
          ...item,
          ...dotsData
        };
      });
      setFilteredData(_data);
      const initialFilters = getInitialFilters(_data, ignoreKeys);
      setChartData(_data);
      setFilters(initialFilters as any);
    }
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  const getTransformer = (dataKey: string | undefined | number) => {
    if (!dataKey) {
      return 0;
    }
    const property: any = lineProps && lineProps.find((f: any) => f.dataKey === dataKey);
    return property ? property.transformer : null;
  };

  const onLineClick = (data: any) => {
    const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
    // Adding this check at top in order to prevent click event when graph don't have click event or data is not present
    // (i.e: Click is made on part of charts where data is null like x axis labels)
    if (data && hasClickEvents && onClick) {
      if (onChartClickPayload) {
        const args = { data, across: props.xUnit, visualization: ChartType.LINE };
        onClick(onChartClickPayload(args));
      } else if (props.reportType?.includes("levelops")) {
        const _data = data?.activePayload?.[0]?.payload || {};
        onClick({ name: _data.name, id: _data.id });
      } else {
        onClick(data.activeLabel as string);
      }
    }
  };
  const getValue = (data: any, transformer: any) => {
    let _value = transformer ? transformer(data.value) : data.value;
    _value = props.percentIncludeFn ? props.percentIncludeFn(data, props.all_data || []) : _value;
    return formatTooltipValue(_value);
  };

  const renderTooltip = (Tprops: TooltipProps) => {
    const { active, payload, label } = Tprops;
    const tooltipRenderTransform = getWidgetConstant(props.reportType, [
      CHART_DATA_TRANSFORMERS,
      CHART_TOOLTIP_RENDER_TRANSFORM
    ]);
    if (tooltipRenderTransform) {
      return (
        <GenericTooltipRenderer tooltipProps={Tprops} chartProps={props} extraProps={{ chartType: ChartType.LINE }} />
      );
    }
    if (payload !== undefined && payload !== null && payload.length > 0) {
      let total = 0;
      const tooltipMapping = get(widgetConstants, [props.reportType || "", "tooltipMapping"], {});
      const listitems = payload.map((item: TooltipPayload, i: number) => {
        const transformer = getTransformer(item && item.dataKey && item.dataKey.toString());
        if (typeof item.value === "number") total += item.value;
        let itemValue = getValue(item, transformer);
        itemValue = formatTooltipValue(itemValue);
        return {
          label: (tooltipMapping[item.name] || item.name).replace(/_/g, " "),
          value: itemValue,
          color: item.color
        };
      });

      let showTotalValue = true;
      if (props?.reportType) {
        const widgetConfig = get(widgetConstants, [props.reportType]);
        if (widgetConfig && HIDE_TOTAL_TOOLTIP in widgetConfig && widgetConfig[HIDE_TOTAL_TOOLTIP]) {
          showTotalValue = false;
        }
      }

      if (showTotalValue && payload.length > 1 && isNumber(total)) {
        total = parseFloat(total.toFixed(2));
        if (props.totalCountTransformFn) {
          total = props.totalCountTransformFn(total, {
            all_data: props.all_data,
            payload: payload[0]?.payload
          });
        }
        let label = "Total";
        const getTotalLabel = get(widgetConstants, [props.reportType || "", "getTotalLabel"], undefined);
        if (getTotalLabel) {
          label = getTotalLabel(props);
        }
        listitems.push({ label, value: total, color: "#000000" });
      }
      if (active) {
        const toolTipLabel = tooltipTitle ? toTitleCase(`${tooltipTitle} of ${label}`) : label;
        return (
          <div className="custom-tooltip">
            <p>{toolTipLabel}</p>
            <ul>{sortTooltipListItems(listitems)}</ul>
          </div>
        );
      }
    }
    return null;
  };

  const CustomizedDot = (props: any) => {
    const { dataKey, payload, cx, cy, stroke, r } = props;
    const dotKey = `${SHOW_DOT}_${dataKey}`;
    if (payload[dotKey] || alwaysShowDot) {
      return (
        <svg x={cx - r} y={cy - r} width={8} height={8} fill="white">
          <g transform={`translate(${r} ${r})`}>
            <circle r={r} fill={stroke} />
          </g>
        </svg>
      );
    }
    return null;
  };

  const dataKeys = useMemo(
    () =>
      lineProps
        ? lineProps.map(lineProp => lineProp.dataKey)
        : filteredData && filteredData.length > 0
        ? Object.keys(filteredData[0]).filter(
            item => !["key", "name", "id", "additional_key", "stage"].includes(item) && !item.includes("showDot")
          )
        : [],
    [lineProps, filteredData]
  );

  const lines = useMemo(
    () =>
      dataKeys.map((line, index) => {
        let yAxisId = "hidden";
        if (units?.length && yAxisIds.includes(yAxisIdByKey[line])) {
          yAxisId = yAxisIdByKey[line];
        } else if (yAxisIds.includes(unit)) {
          yAxisId = unit;
        }
        return (
          <Line
            yAxisId={yAxisId}
            dataKey={line}
            hide={filters?.[line] === false}
            stroke={lineChartColors[index % lineChartColors.length]}
            strokeWidth={1.5}
            key={line}
            activeDot={{ r: 4 }}
            dot={<CustomizedDot />}
            isAnimationActive={false}
            isUpdateAnimationActive={false}
            connectNulls={true}
          />
        );
      }),
    [data, filteredData, unit, filters]
  );

  const lineChartStyle = useMemo(() => {
    if (units && units.length > 1) {
      return { right: 15, bottom: 50, ...(chartProps?.margin || {}) };
    }
    return { right: 5, bottom: 50, ...(chartProps?.margin || {}) };
  }, [units]);
  const tickFormatter = useCallback(
    value => (typeof value === "number" && value / 1000 >= 1 ? `${round(value / 1000, 2)}k` : value),
    []
  );

  const renderYAxises = useMemo(() => {
    return yAxisIds.map((yAxis, index) => {
      const label: any = { value: yAxis, angle: -90, position: index ? "right" : "insideLeft" };
      return (
        <YAxis
          key={`y-axis-${index}`}
          yAxisId={yAxis}
          orientation={index ? "right" : "left"}
          stroke={showYaxis ? chartStaticColors.axisColor : chartTransparentStaticColors.axisColor}
          allowDecimals={false}
          label={label}
          tickFormatter={tickFormatter}
          hide={yAxis === "hidden"}
        />
      );
    });
  }, [yAxisIds]);

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
        truncate,
        xAxisLabel
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [filteredData, props.reportType, config, props.xUnit, props.hasTrendLikeData]
  );

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <>
      <ResponsiveContainer>
        <LineChart
          //ref={ref => convertImgChart(ref)}
          data={filteredData}
          {...chartProps}
          margin={lineChartStyle}
          onClick={(data: any) => onLineClick(data)}>
          {/*<CartesianGrid horizontal={false} vertical={false} />*/}
          <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
          <XAxis
            dataKey="name"
            stroke={chartStaticColors.axisColor}
            interval={"preserveStartEnd"}
            minTickGap={filteredData?.length > 50 ? 10 : filteredData?.length > 25 ? 5 : 1}
            // tickLine={false}
            tick={renderXAxisTick}
          />
          {renderYAxises}
          {!props.config?.disable_tooltip && data && data.length > 0 && (
            <Tooltip cursor={false} content={renderTooltip} />
          )}
          {showDefaultLegend && <Legend />}
          {!showDefaultLegend && !hideLegend && (
            <Legend content={<ChartLegendComponent filters={filters} setFilters={setFilters} />} />
          )}
          {lines}
        </LineChart>
      </ResponsiveContainer>
    </>
  );
};

export default LineChartComponent;
