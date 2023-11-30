import {
  ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM,
  AZURE_SPRINT_REPORTS,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_SPRINT_REPORTS
} from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { BAR_CHART_REF_LINE_STROKE } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ComposedChart,
  LabelList,
  Legend,
  Line,
  ReferenceArea,
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
import {
  getBaselineYAxisTicks,
  getDomainMax,
  getInitialFilters,
  getReferenceAreas,
  getYAxisDomainMax,
  yAxisIdByKey
} from "../helper";
import { default as TiltedAxisTick } from "../components/tilted-axis-tick";
import { ignoreKeys } from "../bar-chart/bar-chart.ignore-keys";
import "../bar-chart/bar-chart.style.scss";
import { getXAxisTimeLabel } from "utils/dateUtils";
import { TIME_FILTERS_KEYS } from "constants/filters";
import { labelMapping } from "../constant";
import { BarChartTooltipWrapper } from "../bar-chart/BarChartTooltipWrapper";
import { WidgetDrilldownHandlerContext } from "dashboard/pages/context";

const CompositeBarChart = (props: BarChartProps) => {
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
    xAxisProps,
    widgetMetaData,
    barTopValueFormater,
    readOnlyLegend,
    showValueOnBarStacks,
    baseLineMap,
    trendLineKey
  } = props;
  let { baseLinesDataPoints = undefined } = props;
  baseLinesDataPoints = baseLinesDataPoints?.length === 0 ? undefined : baseLinesDataPoints;

  const { isDrilldownOpen } = useContext(WidgetDrilldownHandlerContext);

  const [position, setposition] = useState<{ x: any; y: any }>({
    x: 0,
    y: "auto"
  });
  const [isHoverOnTooltip, setIsHoverOnTooltip] = useState(false);

  const defaultBarSize: number = 30;
  const defaultGroupBarSize: number = 30;
  const [filteredData, setFilteredData] = useState([]);
  const [filters, setFilters] = useState<any>({});
  const [barClickedData, setBarClickedData] = useState(null);
  const allowLabelTransform = getWidgetConstant(props.reportType, [
    CHART_DATA_TRANSFORMERS,
    ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM
  ]);
  const isAllowRealTimeDrilldownDataUpdate = getWidgetConstant(
    props.reportType,
    ["isAllowRealTimeDrilldownDataUpdate"],
    false
  );

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const defaultFilterKey = get(widgetConstants, [props.reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(data, ignoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  const xAxisTruncateLength = useMemo(() => {
    return get(xAxisProps, ["XAXIS_TRUNCATE_LENGTH"], undefined);
  }, [xAxisProps]);
  const updatedData = useMemo(() => {
    if (props.useOrderedStacks && props.orderedBarProps) {
      return data.map((item: any) => {
        const keys: any = Object.values(item).reduce((acc: any, next: any) => {
          if (next && filters[next.key]) {
            return {
              ...(acc || {}),
              [next.key]: next.value
            };
          }
          return acc;
        }, {});

        return {
          name: item.name,
          totalStacksBarCount: Object.values(keys).reduce((acc: any, val) => {
            if (acc !== undefined && val !== undefined) {
              acc = acc + val;
            }
            return acc;
          }, 0),
          [trendLineKey]: item?.[trendLineKey],
          ...(keys || {})
        };
      });
    }
    return filteredData;
  }, [data, filteredData, props.useOrderedStacks, props.orderedBarProps, trendLineKey, filters]);

  const yAxisIds = useMemo(() => {
    if (units && units.length) {
      return [...units, "hidden"];
    }
    return [unit, "hidden"];
  }, [unit, units]);

  const xAxisInterval = useMemo(() => {
    return get(xAxisProps, ["interval"], 0);
  }, [xAxisProps]);

  const onBarClick = useCallback(
    (data: any) => {
      const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
      // Adding this check at top in order to prevent click event when graph don't have click event or data is not present
      // (i.e: Click is made on part of charts where data is null like x axis labels)
      if (hasClickEvents && onClick) {
        let selectedFilteredList: string[] = [];

        selectedFilteredList = Object.keys(filters || {}).filter(filter => !!filters?.[filter]);

        if (onChartClickPayload) {
          const args = { data, across: props.xUnit, visualization: ChartType.BAR };
          onClick(onChartClickPayload(args), selectedFilteredList);
        } else if (props.reportType?.includes("levelops")) {
          const _data = data?.activePayload?.[0]?.payload || {};
          onClick({ name: _data.name, id: _data.id }, selectedFilteredList);
        } else if (
          [
            jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
            ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT
          ].includes(props.reportType as any)
        ) {
          let values = (data?.activeLabel || "").split("-");
          if (!!(values || []).length && hasClickEvents) {
            onClick?.(values[0], selectedFilteredList);
          }
        } else if (
          [JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND, JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND].includes(
            props.reportType as any
          )
        ) {
          onClick(
            { sprint_name: data.activeLabel, sprint_id: data.activePayload?.[0]?.payload?.sprint_id },
            selectedFilteredList
          );
        } else {
          onClick(data.activeLabel as string, selectedFilteredList);
        }
      }
    },
    [onClick, filters]
  );

  useEffect(() => {
    if (!isAllowRealTimeDrilldownDataUpdate) return;

    if (barClickedData && isDrilldownOpen) {
      onBarClick(barClickedData);
    } else if (!isDrilldownOpen) {
      setBarClickedData(null);
    }
  }, [filters, isDrilldownOpen]);

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
      return (
        <BarChartTooltipWrapper
          props={props}
          tooltipProps={tooltipProps}
          isHoverOnTooltip={isHoverOnTooltip}
          setIsHoverOnTooltip={setIsHoverOnTooltip}
          tooltipRenderTransform={tooltipRenderTransform}
        />
      );
    },
    [props, isHoverOnTooltip]
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
  const displayValue =
    getWidgetConstant(props.reportType, "application", undefined) === "levelops"
      ? widgetMetaData?.show_value_on_bar
      : !widgetMetaData?.hasOwnProperty("show_value_on_bar") || widgetMetaData.show_value_on_bar;

  const lines = useMemo(() => {
    return (
      !!trendLineKey && (
        <Line dataKey={trendLineKey} stroke={"#CF1322"} strokeDasharray="5 5" strokeWidth={1.5} dot={false} />
      )
    );
  }, [trendLineKey]);

  const stackedBars = (bars: any, hide: boolean = false) => {
    if (bars.length > 0) {
      return [...bars].map((bar: any, i: number) => {
        let yAxisId = "hidden";
        if (units?.length && yAxisIds.includes(yAxisIdByKey[bar.dataKey])) {
          yAxisId = yAxisIdByKey[bar.dataKey];
        } else if (unit && yAxisIds.includes(unit)) {
          yAxisId = unit;
        }

        return (
          <Bar
            cursor={"pointer"}
            stackId={1}
            yAxisId={yAxisId}
            hide={filters[bar?.dataKey] == false}
            barSize={defaultBarSize}
            isAnimationActive={false}
            key={`bar-${i}`}
            {...bar}>
            {showValueOnBarStacks && i === bars?.length - 1 && !hide && (
              <LabelList dataKey={"totalStacksBarCount"} position="top" formatter={barTopValueFormater} />
            )}
          </Bar>
        );
      });
    }
    return [];
  };
  const bars = useMemo(() => {
    if (stacked) {
      if (props.useOrderedStacks && props.orderedBarProps) {
        const visibleBars = props?.orderedBarProps.filter((bar: any) => filters[bar?.dataKey]);
        const hiddenBars = props?.orderedBarProps.filter((bar: any) => !filters[bar?.dataKey]);
        return [...stackedBars(visibleBars), ...stackedBars(hiddenBars, true)];
      }

      return (barProps || []).map((bar, i: number) => {
        const generateBarColors = getWidgetConstant(props.reportType, ["generateBarColors"], null);

        if (generateBarColors) {
          bar.fill = generateBarColors(bar.dataKey);
        } else {
          bar.fill =
            Object.values(JIRA_SPRINT_REPORTS).includes(props.reportType as any) ||
            Object.values(AZURE_SPRINT_REPORTS).includes(props.reportType as any)
              ? getColorForBar(i)
              : getBarColor(bar.dataKey, i);
        }
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
            isAnimationActive={false}
            {...bar}>
            {showValueOnBarStacks && i === barProps?.length - 1 && (
              <LabelList dataKey={"totalStacksBarCount"} position="top" formatter={barTopValueFormater} />
            )}
          </Bar>
        );
      });
    }
    return (barProps || []).map((bar, i: number) => {
      bar.fill = getColorForBar(i);

      return (
        <Bar
          hide={filters?.[bar.dataKey] == false}
          cursor={"pointer"}
          barSize={defaultGroupBarSize}
          yAxisId={yAxisIdByKey[bar.dataKey] || unit}
          key={`bar-${i}`}
          isAnimationActive={false}
          {...bar}>
          {displayValue && <LabelList dataKey={bar.dataKey} position="top" formatter={barTopValueFormater} />}
        </Bar>
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
    props.reportType,
    showValueOnBarStacks,
    props.orderedBarProps,
    props.useOrderedStacks
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
      const across = get(props, ["xUnit"], "");

      if (TIME_FILTERS_KEYS.includes(across)) {
        const interval = get(props, ["interval"]);
        const weekDateFormat = get(props, ["widgetMetaData", "weekdate_format"]);
        xAxisLabel = getXAxisTimeLabel({ interval, key: get(dataPayload, ["key"]), options: { weekDateFormat } });
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

  const domainMax = useMemo(() => {
    return getDomainMax(data, props.stacked);
  }, [data, props?.stacked]);

  const referenceAreas = useMemo(() => {
    return getReferenceAreas(baseLinesDataPoints, baseLineMap, data, props.stacked);
  }, [baseLinesDataPoints, baseLineMap, domainMax]);

  const renderBaselineYAxisTicks = useCallback(
    (yAxisProps: any) => {
      return getBaselineYAxisTicks(yAxisProps, baseLinesDataPoints, baseLineMap);
    },
    [baseLinesDataPoints, filteredData]
  );

  const renderLegends = useMemo(() => {
    return (
      <ChartLegendComponent
        legendsProps={{ unit }}
        report={props?.reportType}
        filters={filters}
        setFilters={setFilters}
        allowLabelTransform={allowLabelTransform}
        readOnlyLegend={readOnlyLegend}
        trendLineKey={trendLineKey}
        labelMapping={labelMapping}
      />
    );
  }, [filters, allowLabelTransform, readOnlyLegend]);

  const rightYaxis = useMemo(() => {
    if (baseLinesDataPoints) {
      return (
        <YAxis
          orientation="right"
          ticks={
            baseLinesDataPoints[0] >= getYAxisDomainMax(domainMax) ? [baseLinesDataPoints[0]] : baseLinesDataPoints
          }
          tick={renderBaselineYAxisTicks}
          domain={[0, getYAxisDomainMax(domainMax)]}
          interval={0}
          stroke={chartTransparentStaticColors.axisColor}
        />
      );
    }
    return [];
  }, [baseLinesDataPoints, domainMax, getYAxisDomainMax, renderBaselineYAxisTicks, filteredData]);

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <ResponsiveContainer>
      <ComposedChart
        data={updatedData}
        {...chartProps}
        margin={barChartStyle}
        onClick={(data: any) => {
          if (data) {
            setBarClickedData(isAllowRealTimeDrilldownDataUpdate ? data : null);
            onBarClick(data);
          }
        }}
        onMouseMove={state => {
          if (stacked && state?.isTooltipActive && !isHoverOnTooltip) {
            setposition({
              x: state.chartX + 20,
              y: "auto"
            });
          }
        }}>
        <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
        <XAxis
          hide={previewOnly}
          dataKey="name"
          stroke={chartTransparentStaticColors.axisColor}
          interval={xAxisInterval}
          minTickGap={3}
          // tickLine={false}
          tick={renderXAxisTick}
          height={xAxisTruncateLength ? Math.round(xAxisTruncateLength * 3.5) : undefined} // 3.5 is constant as per 70/20 = 3.5 where 70 is expected height as per 20 chars in xaixs
        />
        {renderYAxises}
        <ReferenceLine
          y={0}
          ifOverflow="extendDomain"
          yAxisId={yAxisIds.includes(unit) ? unit : "hidden"}
          stroke={getWidgetConstant(props.reportType, [BAR_CHART_REF_LINE_STROKE], "#8A94A5")}
        />

        {rightYaxis}
        {!config?.disable_tooltip && (data || []).length > 0 && (
          <Tooltip {...(stacked && { position: position })} cursor={false} content={tooltipContent} />
        )}
        {showDefaultLegend && (
          <Legend verticalAlign={legendPosition || "bottom"} {...(legendProps || {})} formatter={legendFormatter} />
        )}
        {!showDefaultLegend && !hideLegend && <Legend content={renderLegends} />}
        {referenceAreas}
        {bars}
        {lines}
      </ComposedChart>
    </ResponsiveContainer>
  );
};

export default CompositeBarChart;
