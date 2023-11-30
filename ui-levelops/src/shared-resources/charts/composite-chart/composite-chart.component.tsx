import widgetConstants from "dashboard/constants/widgetConstants";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { cloneDeep, forEach, get, map, max } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Area,
  Bar,
  CartesianGrid,
  ComposedChart,
  LabelList,
  Legend,
  Line,
  ReferenceArea,
  ResponsiveContainer,
  Tooltip,
  TooltipPayload,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import {
  CHART_DATA_TRANSFORMERS,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE,
  ISSUE_MANAGEMENT_REPORTS,
  PAGERDUTY_REPORT
} from "../../../dashboard/constants/applications/names";
import { jiraBacklogKeyMapping } from "../../../dashboard/graph-filters/components/Constants";
import { EmptyWidget } from "../../components";
import "../bar-chart/bar-chart.style.scss";
import { getDynamicColor } from "../chart-color.helper";
import {
  areaChartKeys,
  barChartKeys,
  chartTransparentStaticColors,
  lineChartColors,
  lineChartKeys
} from "../chart-themes";
import { BarChartProps } from "../chart-types";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import {
  compositeYAxisIdByKey,
  formatTooltipValue,
  getBaselineYAxisTicks,
  getDomainMax,
  getInitialFilters,
  getReferenceAreas,
  getYAxisDomainMax,
  sortTooltipListItems
} from "../helper";
import { getXAxisTimeLabel } from "utils/dateUtils";
import { TIME_FILTERS_KEYS } from "constants/filters";
import { truncateAndEllipsis } from "utils/stringUtils";
import { TREND_LINE_DIFFERENTIATER } from "../constant";
import { round } from "utils/mathUtils";
import TiltedAxisTick from "../components/tilted-axis-tick";

const CompositeChartComponent = (props: BarChartProps) => {
  const {
    data,
    chartProps,
    stacked,
    unit,
    id,
    units,
    previewOnly,
    hideLegend,
    colorSchema,
    isDemo,
    xAxisProps,
    displayValueOnBar,
    display_colors,
    trendLineData,
    baseLineMap
  } = props;

  let { baseLinesDataPoints } = props;
  baseLinesDataPoints = baseLinesDataPoints?.length === 0 ? undefined : baseLinesDataPoints;

  let bars: JSX.Element[];
  let lines: JSX.Element[];
  let areas: JSX.Element[];
  const defaultBarSize: number = 30;
  const defaultGroupBarSize: number = 30;
  const ignoreKeys = ["name", "total_tickets", "key", "toolTip", "total_cases", "additional_key"];
  const [filters, setFilters] = useState<any>({});
  const numColors = lineChartColors.length;

  const jiraReportsException = useMemo(() => {
    return [
      JiraReports.RESOLUTION_TIME_REPORT,
      JiraReports.BACKLOG_TREND_REPORT,
      "scm_issues_time_resolution_report",
      "azure_backlog_trend_report",
      ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
      PAGERDUTY_REPORT.RESPONSE_REPORTS
    ].includes(props.reportType as any);
  }, [props.reportType]);

  const xAxisInterval = useMemo(() => {
    return get(xAxisProps, ["interval"], undefined);
  }, [xAxisProps]);

  const xAxisTruncateLength = useMemo(() => {
    return get(xAxisProps, ["XAXIS_TRUNCATE_LENGTH"], undefined);
  }, [xAxisProps]);

  const yAxisIds = useMemo(() => {
    if (units && units.length) {
      return [...units, "hidden"];
    }
    return [unit, "hidden"];
  }, [unit, units]);

  const getTransformer = (dataKey: string | undefined | number) => {
    if (!dataKey) {
      return null;
    }
    // @ts-ignore
    const areaProps = props.areaProps;
    const property =
      areaProps && areaProps.length && areaProps.find((f: any) => (dataKey as string).includes(f.dataKey));
    return property ? property.transformer : null;
  };

  useEffect(() => {
    if ((data || []).length) {
      const initialFilters = getInitialFilters(data, ignoreKeys);
      setFilters(initialFilters as any);
    }
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  const allKeys: any = useMemo(() => {
    const everyKeys = getInitialFilters(
      data,
      ignoreKeys,
      undefined,
      ["azure_backlog_trend_report"].includes(props.reportType as any)
    );
    if (["jira_backlog_trend_report", "azure_backlog_trend_report"].includes(props.reportType as any)) {
      return { ...(everyKeys as any), total_tickets: true };
    } else return everyKeys;
  }, [data]);

  const mappedLineChartKeys = useMemo(() => {
    if (["resolution_time_report", ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(props.reportType as any)) {
      return {
        lines: ["number_of_tickets_closed"],
        bars: ["median_resolution_time", "90th_percentile_resolution_time", "average_resolution_time"],
        area: []
      };
    }
    if (["jira_backlog_trend_report", "azure_backlog_trend_report"].includes(props.reportType as any)) {
      let bars = ["total_tickets", "total_story_points"];
      if (stacked) {
        bars = (props.barProps || []).map((bar: any) => bar.dataKey);
      }

      return {
        lines: ["median", "p90", "mean"],
        bars: bars,
        area: []
      };
    }
    if (props.reportType === "scm_issues_time_resolution_report") {
      return {
        lines: ["number_of_tickets_closed"],
        bars: ["median_resolution_time"],
        area: []
      };
    }

    if (props.reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      let bars = ["count"];
      if (stacked) {
        bars = (props.barProps || []).map((bar: any) => bar.dataKey);
      }
      return {
        lines: ["mean", "median"],
        bars: bars,
        area: []
      };
    }

    if ((props as any).tableId !== undefined && (props as any).tableId !== null && (props as any).tableId !== "") {
      return {
        lines: [],
        bars: [],
        area: []
      };
    }
    return {
      lines: lineChartKeys,
      bars: barChartKeys,
      area: areaChartKeys
    };
  }, [props.reportType, props.barProps, stacked]);

  const customLineChartStyles: any = {
    resolution_time_report: {
      fill: "orange",
      stroke: "orange",
      strokeWidth: 3
    },
    azure_resolution_time_report: {
      fill: "orange",
      stroke: "orange",
      strokeWidth: 3
    },
    jira_backlog_trend_report: {
      fill: "orange",
      stroke: "orange",
      strokeWidth: 3
    },
    azure_backlog_trend_report: {
      fill: "orange",
      stroke: "orange",
      strokeWidth: 3
    },
    scm_issues_time_resolution_report: {
      fill: "orange",
      stroke: "orange",
      strokeWidth: 3
    },
    [PAGERDUTY_REPORT.RESPONSE_REPORTS]: {
      fill: "orange",
      stroke: "orange",
      strokeWidth: 3
    }
  };

  const getLineChartStyle = useCallback(
    (index: number) => {
      let fill = lineChartColors[lineChartColors.length - (index % lineChartColors.length) - 1];
      let stroke = fill;
      let strokeWidth = 1.5;

      if (props.reportType && customLineChartStyles[props.reportType]) {
        stroke = customLineChartStyles[props.reportType]["stroke"];
      }
      if (props.reportType && customLineChartStyles[props.reportType]) {
        fill = customLineChartStyles[props.reportType]["fill"];
      }
      if (props.reportType && customLineChartStyles[props.reportType]) {
        strokeWidth = customLineChartStyles[props.reportType]["strokeWidth"];
      }
      return { fill, stroke, strokeWidth };
    },
    [props.reportType]
  );

  const getYAxisId = useCallback(
    (key: any) => {
      let yAxisId = "hidden";
      if (units?.length && yAxisIds.includes(compositeYAxisIdByKey[key])) {
        yAxisId = compositeYAxisIdByKey[key];
      } else if (yAxisIds.includes(unit)) {
        (yAxisId as any) = unit;
      }
      return yAxisId;
    },
    [units, yAxisIds]
  );

  const getColor = useCallback(
    (key: string, index: number) => {
      return getDynamicColor(key, index, colorSchema);
    },
    [colorSchema]
  );

  const mappedKeys = useMemo(() => {
    return {
      lines: Object.keys(allKeys).filter(
        item =>
          mappedLineChartKeys.lines.find((key: string) => item.includes(key)) !== undefined || item.includes("_line")
      ),
      bars: Object.keys(allKeys).filter(
        item =>
          mappedLineChartKeys.bars.find((key: string) => {
            if (["azure_backlog_trend_report", "jira_backlog_trend_report"].includes(props.reportType as any)) {
              return item === key;
            }
            return item.split("-")[0] === key;
          }) !== undefined || item.includes("_bar")
      ),
      areas: Object.keys(allKeys).filter(
        item =>
          mappedLineChartKeys.area.find((key: string) => item.includes(key)) !== undefined || item.includes("_area")
      )
    };
  }, [allKeys, mappedLineChartKeys, props.reportType]);

  const barTopValueFormater = (v: string | number) => {
    if (typeof v === "number") return formatTooltipValue(v);
    if (typeof v === "string") return truncateAndEllipsis(v, 5);
    return v;
  };

  lines =
    data && data.length > 0
      ? mappedKeys.lines.map((line, index) => {
          const yAxisId = getYAxisId(line);
          const moreProps = jiraReportsException ? { yAxisId } : {};
          return (
            <Line
              //type="monotone"
              {...moreProps}
              dataKey={line}
              fill={getLineChartStyle(index).fill}
              stroke={getLineChartStyle(index).stroke}
              strokeWidth={getLineChartStyle(index).strokeWidth}
              hide={filters?.[line] === false}
              dot={false}
              key={`line-${line}`}
              activeDot={{ r: 4 }}
              connectNulls={true}
            />
          );
        })
      : [];

  bars =
    data && data.length > 0
      ? mappedKeys.bars.map((line, index) => {
          const yAxisId = getYAxisId(line);
          const moreProps = jiraReportsException ? { yAxisId } : {};
          if (stacked) {
            return (
              <Bar
                {...moreProps}
                cursor={"pointer"}
                stackId={1}
                barSize={defaultBarSize}
                key={`bar-${index}`}
                hide={filters?.[line] === false}
                fill={getColor(line, index)}
                dataKey={line}
              />
            );
          } else {
            if (!!get(trendLineData, [line])) {
              lines.push(
                <Line
                  dataKey={trendLineData?.[line] ?? ""}
                  stroke={display_colors?.[line]}
                  hide={filters?.[line] === false}
                  strokeDasharray="5 5"
                  strokeWidth={1.5}
                  dot={false}
                  connectNulls={true}
                />
              );
            }
            return (
              <Bar
                {...moreProps}
                cursor={"pointer"}
                hide={filters?.[line] === false}
                barSize={defaultGroupBarSize}
                key={`bar-${index}`}
                fill={display_colors?.[line] ?? lineChartColors[index % lineChartColors.length]}
                dataKey={line}
                isAnimationActive={false}>
                {displayValueOnBar && <LabelList dataKey={line} position="top" formatter={barTopValueFormater} />}
              </Bar>
            );
          }
        })
      : [];

  areas =
    data && data.length > 0
      ? mappedKeys.areas.map((line, index) => {
          const yAxisId = getYAxisId(line);
          const moreProps = jiraReportsException ? { yAxisId } : {};
          return (
            <Area
              {...moreProps}
              type="monotone"
              dataKey={line}
              hide={filters?.[line] === false}
              stroke={lineChartColors[numColors - (index % numColors)]}
              fill={lineChartColors[numColors - (index % numColors)]}
              strokeWidth={1.5}
              key={line}
              dot={false}
              activeDot={{ r: 4 }}
              connectNulls={true}
              fillOpacity={0.5}
            />
          );
        })
      : [filters];

  const onGraphClick = useCallback(
    (data: any) => {
      if (!data) {
        return;
      }
      // handling cases for jira resolution time report and jira backlog trend report
      // they have now composite graph type for some cases
      const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);

      if (props.hasClickEvents && props.onClick && data) {
        if (isDemo) {
          props.onClick({ widgetId: id, name: data?.activeLabel, phaseId: data?.activeLabel });
          return;
        }

        if (onChartClickPayload) {
          const args = { data, across: props.xUnit };
          props.onClick(onChartClickPayload(args));
          return;
        }
      }
      if (
        [
          JiraReports.BACKLOG_TREND_REPORT,
          "scm_issues_time_resolution_report",
          "azure_backlog_trend_report",
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
          PAGERDUTY_REPORT.RESPONSE_REPORTS
        ].includes(props.reportType || ("" as any))
      ) {
        const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
        if (data && props.hasClickEvents && props.onClick) {
          if (onChartClickPayload) {
            const args = { data, across: props.xUnit };
            props.onClick(onChartClickPayload(args));
          } else {
            props.onClick(data?.activeLabel as string);
          }
        }
      }
    },
    [props.reportType, id, isDemo]
  );

  const renderTooltip = (tProps: TooltipProps) => {
    const { active, payload, label } = tProps;
    if (payload !== undefined && payload !== null && payload.length > 0) {
      const listitems = payload
        .filter((item: TooltipPayload) => !(item?.name ?? "").includes(TREND_LINE_DIFFERENTIATER))
        .map((item: TooltipPayload, i: number) => {
          const transformer = getTransformer(item && item.dataKey && item.dataKey.toString());
          //The tooltip key will be now compositeTransformKey_widgetName
          let itemName = item.name;
          if (itemName.includes("bar")) {
            itemName = itemName.substr(0, itemName.length - 3);
          } else if (itemName.endsWith("area") || itemName.endsWith("line")) {
            itemName = itemName.substr(0, itemName.length - 4);
          }

          const tooltipMapping = get(widgetConstants, [props.reportType || "", "tooltipMapping"], {});
          const dataTruncatingValue = get(props, ["dataTruncatingValue"], 1);
          let itemValue = transformer ? transformer(item.value, item.dataKey) : item.value;
          itemValue = formatTooltipValue(itemValue, dataTruncatingValue);

          if (["jira_backlog_trend_report", "azure_backlog_trend_report"].includes(props.reportType as any)) {
            itemName = get(jiraBacklogKeyMapping, [itemName], itemName);
          }
          if (Object.keys(tooltipMapping).length) {
            itemName = get(tooltipMapping, [itemName], itemName);
          }

          const color = item.fill;

          return {
            label: itemName,
            value: itemValue,
            color
          };
        });
      if (active) {
        return (
          <div className="custom-tooltip">
            <p className={`${props?.bypassTitleTransform ? "" : "capitalize"}`}>{label}</p>
            <ul className={`${props?.bypassTitleTransform ? "" : "capitalize"}`}>{sortTooltipListItems(listitems)}</ul>
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

  const renderYAxises = useMemo(() => {
    if (!jiraReportsException) {
      const useCustomDomain =
        typeof props.dataMin === "number" &&
        typeof props.dataMax === "number" &&
        0 <= props.dataMin &&
        props.dataMax <= 1;
      return (
        <YAxis
          hide={previewOnly}
          key={`y-axis`}
          stroke={chartTransparentStaticColors.axisColor}
          allowDecimals={false}
          label={{ value: unit, angle: -90, position: "insideLeft" }}
          tickFormatter={tickFormatter}
          domain={useCustomDomain ? [0, 1] : undefined}
        />
      );
    }
    return (yAxisIds || []).map((yAxis, index) => {
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
  }, [yAxisIds, props.dataMin, props.dataMax]);

  const renderXAxisTick = useCallback(
    (xAxisProps: any) => {
      const dataIndex = xAxisProps?.payload?.index;
      const dataPayload = data?.[dataIndex] || {};
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

      // updating date format only key is present
      if (TIME_FILTERS_KEYS.includes(across) && get(dataPayload, ["key"], undefined)) {
        const interval = get(props, ["interval"]);
        const weekDateFormat = get(props, ["widgetMetaData", "weekdate_format"]);
        xAxisLabel = getXAxisTimeLabel({ interval, key: get(dataPayload, ["key"]), options: { weekDateFormat } });
      }

      const tickProps = {
        ...(xAxisProps || {}),
        truncate,
        xAxisLabel,
        xAxisTruncateLength
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [data, props.reportType, props.xUnit, props.hasTrendLikeData, xAxisTruncateLength]
  );

  const renderLegends = useMemo(() => {
    return <ChartLegendComponent filters={filters} setFilters={setFilters} />;
  }, [filters]);

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
    [baseLinesDataPoints]
  );

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <>
      <ResponsiveContainer>
        <ComposedChart
          data={data}
          {...chartProps}
          barGap={0}
          margin={{
            top: 40,
            right: 10,
            left: 5,
            bottom: chartProps?.margin?.bottom !== undefined ? chartProps.margin.bottom : 50
          }}
          onClick={onGraphClick}>
          <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
          <XAxis
            hide={previewOnly}
            dataKey="name"
            stroke={chartTransparentStaticColors.axisColor}
            interval={xAxisInterval !== undefined ? xAxisInterval : data?.length > 25 ? "preserveStartEnd" : 0}
            minTickGap={data?.length > 50 ? 10 : data?.length > 25 ? 5 : 1}
            // tickLine={false}
            tick={renderXAxisTick}
            height={xAxisTruncateLength ? Math.round(xAxisTruncateLength * 3.5) : undefined} // 3.5 is constant as per 70/20 = 3.5 where 70 is expected height as per 20 chars in xaixs
          />
          {renderYAxises}
          {baseLinesDataPoints?.length && (
            <YAxis
              yAxisId="right"
              orientation="right"
              ticks={baseLinesDataPoints}
              tick={renderBaselineYAxisTicks}
              domain={[0, getYAxisDomainMax(domainMax)]}
              interval={0}
              stroke={chartTransparentStaticColors.axisColor}
            />
          )}
          {!props.config?.disable_tooltip && (data || []).length > 0 && (
            <Tooltip cursor={false} content={renderTooltip} />
          )}
          {!hideLegend && <Legend content={renderLegends} />}
          {referenceAreas}
          {bars}
          {lines}
          {areas}
        </ComposedChart>
      </ResponsiveContainer>
    </>
  );
};

export default CompositeChartComponent;
