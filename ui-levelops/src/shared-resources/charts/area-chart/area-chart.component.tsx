import { Icon } from "antd";
import {
  azureLeadTimeIssueReports,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE,
  JIRA_SPRINT_REPORTS,
  AZURE_SPRINT_REPORTS,
  leadTimeReports
} from "dashboard/constants/applications/names";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get, isNumber, startCase } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
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
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { leadTimeMetricsMapping } from "../../../custom-hooks/helpers/leadTime.helper";
import { AntText, EmptyWidget } from "../../components";
import { getDynamicColor } from "../chart-color.helper";
import { chartStaticColors, lineChartColors as defaultColors } from "../chart-themes";
import { AreaChartProps } from "../chart-types";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import GenericTooltipRenderer from "../components/GenericTooltipRenderer";
import LeadTimeFilterDropdown from "../components/lead-time-filter-dropdown/LeadTimeFilterDropdown";
import { getInitialFilters, sortTooltipListItems } from "../helper";
import "./area-chart.style.scss";
import { round } from "utils/mathUtils";
import TiltedAxisTick from "../components/tilted-axis-tick";

const AreaChartComponent = (props: AreaChartProps) => {
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
    dataKey,
    showTotalOnTooltip,
    colorSchema,
    xAxisProps
  } = props;
  const [filters, setFilters] = useState<any>({});
  const [filteredData, setFilteredData] = useState([]);

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

  const xAxisTruncateLength = useMemo(() => {
    return get(xAxisProps, ["XAXIS_TRUNCATE_LENGTH"], undefined);
  }, [xAxisProps]);

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
        // excluding certain reports from dynamic colors
        if (
          !Object.values(JIRA_SPRINT_REPORTS).includes(reportType as any) &&
          !Object.values(AZURE_SPRINT_REPORTS).includes(reportType as any)
        ) {
          return getDynamicColor(key, index, colorSchema);
        }
      }
      return lineChartColors[index % numColors];
    },
    [colorSchema, stackedArea, reportType]
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
              {...additionalProps}
            />
          );
        })
      : [];

  const renderTooltip = (tProps: TooltipProps) => {
    const { active, payload, label } = tProps;
    const tooltipRenderTransform = getWidgetConstant(props.reportType, [
      CHART_DATA_TRANSFORMERS,
      CHART_TOOLTIP_RENDER_TRANSFORM
    ]);
    if (tooltipRenderTransform) {
      return (
        <GenericTooltipRenderer tooltipProps={tProps} chartProps={props} extraProps={{ chartType: ChartType.AREA }} />
      );
    }
    if (payload !== undefined && payload !== null && payload.length > 0) {
      let total = 0;
      let listitems = payload.map((item: TooltipPayload, i: number) => {
        const transformer = getTransformer(item && item.dataKey && item.dataKey.toString());
        if (typeof item.value === "number") total += item.value;
        return {
          label: startCase(item.name),
          value: transformer ? transformer(item.value, item.dataKey) : item.value,
          color: item.color
        };
      });

      if (
        ![
          JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
          AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND
        ].includes(reportType as any) &&
        isNumber(total) &&
        (showTotalOnTooltip || stackedArea)
      ) {
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

  const onBarClick = useCallback(
    (data: any) => {
      const onChartClickPayload = getWidgetConstant(reportType, ["onChartClickPayload"]);
      // Adding this check at top in order to prevent click event when graph don't have click event or data is not present
      // (i.e: Click is made on part of charts where data is null like x axis labels)
      if (data && hasClickEvents && onClick) {
        if (onChartClickPayload) {
          const args = { data, across: props.xUnit, visualization: ChartType.AREA };
          onClick(onChartClickPayload(args));
        } else if ([...leadTimeReports, ...azureLeadTimeIssueReports].includes(reportType as any)) {
          const _data = data?.activePayload?.[0]?.payload || {};
          onClick({ label: data.activeLabel as string, value: _data.key });
        } else {
          onClick(data.activeLabel as string);
        }
      }
    },
    [reportType, props.xUnit]
  );

  const tickFormatter = useCallback(
    value => (typeof value === "number" && value / 1000 >= 1 ? `${round(value / 1000, 2)}k` : value),
    []
  );
  // @ts-ignore
  const margin = useMemo(
    () => ({
      top: 40,
      right: 5,
      left: 5,
      bottom: chartProps?.margin?.bottom !== undefined ? chartProps?.margin?.bottom : 50
    }),
    []
  );
  const label = useMemo(() => ({ value: unit, angle: -90, position: "insideLeft" } as any), []);

  const renderLegendContent = useMemo(() => {
    if ([...leadTimeReports, ...azureLeadTimeIssueReports].includes(reportType as any)) {
      return (
        <div className="legend-container mt-25">
          <div className="indicators-container">
            <Icon type="info-circle" theme="outlined" style={{ fontSize: "14px" }} />
            <AntText className="metric-label">{get(leadTimeMetricsMapping, dataKey || "mean", "Average Time")}</AntText>
            <div className="separator" />
            <LeadTimeFilterDropdown filters={filters} setFilters={setFilters} />
          </div>
        </div>
      );
    }

    return <ChartLegendComponent filters={filters} setFilters={setFilters} />;
  }, [reportType, filters, dataKey]);

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
        xAxisLabel,
        xAxisTruncateLength
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [filteredData, props.reportType, config, props.xUnit, props.hasTrendLikeData, xAxisTruncateLength]
  );

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }
  return (
    <ResponsiveContainer>
      <AreaChart onClick={onBarClick} className="area-chart" margin={margin} data={data}>
        <XAxis
          dataKey="name"
          stroke={chartStaticColors.axisColor}
          tickLine={false}
          interval={0}
          minTickGap={filteredData?.length > 50 ? 10 : filteredData?.length > 25 ? 5 : 1}
          tick={renderXAxisTick}
          height={xAxisTruncateLength ? Math.round(xAxisTruncateLength * 3.5) : undefined} // 3.5 is constant as per 70/20 = 3.5 where 70 is expected height as per 20 chars in xaixs
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

export default AreaChartComponent;
