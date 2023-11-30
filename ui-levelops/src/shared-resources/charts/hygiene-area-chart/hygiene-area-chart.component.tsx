import {
  CHART_DATA_TRANSFORMERS,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE
} from "dashboard/constants/applications/names";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { isNumber, startCase, get } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  TooltipPayload,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import { EmptyWidget } from "../../components";
import { getDynamicColor } from "../chart-color.helper";
import { chartStaticColors, lineChartColors as defaultColors } from "../chart-themes";
import { AreaChartProps } from "../chart-types";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import { getInitialFilters, sortTooltipListItems } from "../helper";
import "./area-chart.style.scss";
import { getXAxisTimeLabel } from "utils/dateUtils";
import { round } from "utils/mathUtils";
import TiltedAxisTick from "../components/tilted-axis-tick";

const HygieneAreaChartComponent = (props: AreaChartProps) => {
  const {
    // @ts-ignore
    id,
    data,
    areaProps,
    unit,
    hasClickEvents,
    onClick,
    customColors,
    stackedArea = false,
    chartProps,
    config,
    showGrid,
    fillOpacity,
    showDots,
    reportType,
    legendType = "line",
    areaType = "monotone",
    hideLegend,
    previewOnly = false,
    hideKeys,
    showTotalOnTooltip,
    colorSchema,
    hygieneMapping
  } = props;
  const [filters, setFilters] = useState<any>({});
  const [filteredData, setFilteredData] = useState([]);

  const activeStack = useRef<string>();

  const ignoreKeys = ["name", "total_tickets", "key", "toolTip"];
  let lineChartColors = defaultColors;
  if (customColors && Array.isArray(customColors)) {
    lineChartColors = [...customColors, ...defaultColors];
  }

  const numColors = lineChartColors.length;
  const dot = !!showDots;

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      let initialFilters: any = getInitialFilters(data, ignoreKeys);
      if (hideKeys && hideKeys.length) {
        hideKeys.forEach(key => {
          initialFilters[key] = false;
        });
      }
      setFilters(initialFilters as any);
    }
  }, [data, hideKeys]); // eslint-disable-line react-hooks/exhaustive-deps

  const getTransformer = (dataKey: string | undefined | number) => {
    if (!dataKey) {
      return 0;
    }
    const property = areaProps && areaProps.find(f => f.dataKey === dataKey);
    return property ? property.transformer : null;
  };

  const additionalProps = stackedArea ? { stackId: 1 } : {};

  const activeDots = useMemo(() => ({ r: 4 }), []);

  const getColor = useCallback(
    (key: string, index: number) => {
      if (stackedArea) {
        return getDynamicColor(key, index, colorSchema);
      }
      return lineChartColors[index % numColors];
    },
    [colorSchema, stackedArea, reportType]
  );

  const onAreaClick = useCallback((currentStack: string) => {
    activeStack.current = currentStack;
  }, []);

  const onAreaChartClick = useCallback(
    (data: any) => {
      const onChartClickPayload = getWidgetConstant(reportType, ["onChartClickPayload"]);
      const activeLabel = data?.activeLabel || "";
      if (activeLabel && activeStack.current && hasClickEvents && onClick) {
        if (onChartClickPayload) {
          const args = { data, across: props.xUnit, hygiene: activeStack.current };
          onClick(onChartClickPayload(args));
        } else {
          onClick({ value: activeLabel, hygiene: activeStack.current });
        }
      }
    },
    [reportType, activeStack.current]
  );

  const allKeys: any = useMemo(() => getInitialFilters(filteredData, ignoreKeys), [filteredData]);

  const areas: JSX.Element[] =
    filteredData && filteredData.length > 0
      ? Object.keys(allKeys).map((line: string, index) => {
          return (
            <Area
              type={areaType as any}
              dataKey={line}
              stroke={getColor(line, index)}
              fill={getColor(line, index)}
              strokeWidth={1.5}
              key={line}
              dot={dot}
              hide={filters?.[line] === false}
              activeDot={activeDots}
              connectNulls={true}
              legendType={legendType as any}
              fillOpacity={fillOpacity ?? 0.5}
              onClick={() => onAreaClick(line)}
              {...additionalProps}
            />
          );
        })
      : [];

  const renderTooltip = (tProps: TooltipProps) => {
    const { active, payload, label } = tProps;
    if (payload !== undefined && payload !== null && payload.length > 0) {
      let total = 0;
      let listitems = payload.map((item: TooltipPayload, i: number) => {
        const transformer = getTransformer(item && item.dataKey && item.dataKey.toString());
        if (typeof item.value === "number") total += item.value;

        let label = item.name;

        if (hygieneMapping) {
          label = hygieneMapping?.[item.name] || item.name;
        }

        return {
          label: startCase(label),
          value: transformer ? transformer(item.value, item.dataKey) : item.value,
          color: item.color
        };
      });

      if (isNumber(total) && (showTotalOnTooltip || stackedArea)) {
        total = parseFloat(total.toFixed(2));
        listitems.push({ label: "Total", value: total, color: "#000000" });
      }

      if (active) {
        return (
          <div className="custom-tooltip">
            <p>{label}</p>
            <ul>{sortTooltipListItems(listitems)}</ul>
          </div>
        );
      }
    }
    return null;
  };

  const tickFormatter = useCallback(
    value => (typeof value === "number" && value / 1000 >= 1 ? `${round(value / 1000, 2)}k` : value),
    []
  );
  // @ts-ignore
  const margin = useMemo(
    () => ({
      top: 40,
      right: 10,
      left: 5,
      bottom: chartProps?.margin?.bottom !== undefined ? chartProps?.margin?.bottom : 50
    }),
    []
  );
  const label = useMemo(() => ({ value: unit, angle: -90, position: "insideLeft" } as any), []);

  const renderLegendContent = useMemo(() => {
    return <ChartLegendComponent filters={filters} setFilters={setFilters} labelMapping={hygieneMapping} />;
  }, [filters, hygieneMapping]);

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
      const interval = get(props, ["interval"]);
      if (interval === "week") {
        const weekDateFormat = get(props, ["widgetMetaData", "weekdate_format"]);
        xAxisLabel = getXAxisTimeLabel({ interval, key: get(dataPayload, ["key"]), options: { weekDateFormat } });
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

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }
  return (
    <ResponsiveContainer>
      <AreaChart className="area-chart" margin={margin} data={data} onClick={(args: any) => onAreaChartClick(args)}>
        <XAxis
          dataKey="name"
          stroke={chartStaticColors.axisColor}
          tickLine={false}
          interval={"preserveStartEnd"}
          minTickGap={filteredData?.length > 50 ? 10 : filteredData?.length > 25 ? 5 : 1}
          tick={renderXAxisTick}
        />
        <YAxis
          hide={previewOnly}
          stroke={chartStaticColors.axisColor}
          label={label}
          tickLine={false}
          tickFormatter={tickFormatter}
        />
        {showGrid && <CartesianGrid strokeDasharray="3 3" />}
        <ReferenceLine y={0} stroke="#f1f1f1" />
        {!config?.disable_tooltip && (data || []).length > 0 && <Tooltip cursor={false} content={renderTooltip} />}
        {areas}
        {!hideLegend && <Legend content={renderLegendContent} />}
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default HygieneAreaChartComponent;
