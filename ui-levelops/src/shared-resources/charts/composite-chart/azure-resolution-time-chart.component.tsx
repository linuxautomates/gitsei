import { get, isArray, startCase, uniq } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Area,
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  TooltipPayload,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import "../bar-chart/bar-chart.style.scss";
import { chartTransparentStaticColors } from "../chart-themes";
import { BarChartProps } from "../chart-types";
import ChartLegendComponent from "shared-resources/charts/components/chart-legend/chart-legend.component";
import {
  compositeYAxisIdByKey,
  formatTooltipValue,
  getFilteredData,
  getInitialFilters,
  sortTooltipListItems
} from "../helper";
import { lineChartColors } from "../chart-themes";
import { default as TiltedAxisTick } from "../components/tilted-axis-tick";
import { staticPriorties } from "../jira-prioirty-chart/helper";
import { getWidgetConstant } from "../../../dashboard/constants/widgetConstants";
import { toTitleCase } from "../../../utils/stringUtils";
import { round } from "utils/mathUtils";
import { default as EmptyWidget } from "shared-resources/components/empty-widget/empty-widget.component";

const AzureResolutionChartComponent = (props: BarChartProps) => {
  const { data, chartProps, unit, id, units, previewOnly, hideLegend } = props;
  let lines: JSX.Element[] = [];
  const ignoreKeys = ["name", "total_tickets", "key", "toolTip", "total_cases", "additional_key"];
  const [filters, setFilters] = useState<any>({});
  const stacked = ((data[0] as any) || {}).stacked;
  const [filteredData, setFilteredData] = useState([]);
  const stacks = ((data[0] as any) || {}).stacks;

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

  const yAxisIds = useMemo(() => {
    if (units && units.length) {
      return [...units, "hidden"];
    }
    return [unit, "hidden"];
  }, [unit, units]);

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const initialFilters = getInitialFilters(data, ignoreKeys);
      setFilters(initialFilters as any);
    }
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (filters && data && isArray(data)) {
      const newData = getFilteredData(data, filters);
      setFilteredData(newData);
    }
  }, [filters]); // eslint-disable-line react-hooks/exhaustive-deps

  const metrics = useMemo(() => {
    return uniq(
      Object.keys(data?.[0] || {})
        .filter((item: string) => item.includes("bar") && item.includes("^__"))
        .map((item: string) => item.split("^__")?.[0])
    );
  }, [data]);

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

  const allKeys = useMemo(() => {
    const singleObject = data.reduce((acc, next) => ({ ...acc, ...next }), {});
    const keys = Object.keys(singleObject).filter(key => key.includes("^") && key.includes("bar"));
    return uniq(keys);
  }, [data]);

  const renderBars = useMemo(() => {
    let series: any[] = [];
    let colorIndex = 0;

    metrics.forEach((metricKey, stackIndex) => {
      const filteredKeys = allKeys.filter(_key => _key.includes(metricKey));
      filteredKeys.forEach(key => {
        const yAxisId = getYAxisId(key.split("^__")?.[0]);
        colorIndex = colorIndex + 1;
        series.push(
          <Bar
            yAxisId={yAxisId}
            key={key}
            barSize={30}
            dataKey={key}
            stackId={stackIndex}
            fill={lineChartColors[colorIndex % lineChartColors.length]}
          />
        );
      });
    });

    return series;
  }, [data]);

  const renderLines = useMemo(() => {
    let series: any[] = [];
    let colorIndex = 0;
    ["number_of_tickets_closed"].forEach((key, index) => {
      const yAxisId = getYAxisId(key);
      colorIndex = colorIndex + 1;
      series.push(
        <Line
          activeDot={{ r: 4 }}
          yAxisId={yAxisId}
          key={key}
          fill={getLineChartStyle(index).fill}
          stroke={getLineChartStyle(index).stroke}
          strokeWidth={getLineChartStyle(index).strokeWidth}
          dataKey={key}
          connectNulls={true}
        />
      );
    });

    return series;
  }, [data]);

  const onGraphClick = useCallback(
    (data: any) => {
      if (data && props.hasClickEvents && props.onClick) {
        const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
        if (onChartClickPayload) {
          const args = { data, across: props.xUnit };
          props.onClick(onChartClickPayload(args));
          return;
        }
        props.onClick(data.activeLabel as string);
      }
    },
    [props.reportType]
  );

  const renderTooltip = (tProps: TooltipProps) => {
    const { active, payload, label } = tProps;
    if (payload !== undefined && payload !== null && payload.length > 0) {
      const listitems = payload.map((item: TooltipPayload, i: number) => {
        //The tooltip key will be now compositeTransformKey_widgetName
        const splitName = (item?.dataKey?.toString() as string).split("^");

        let _itemName = item.name;

        if (stacks[0] === "priority") {
          const splitValue = _itemName.split("^");
          _itemName = splitValue[0] + " " + get(staticPriorties, [splitValue[1]], _itemName);
        }

        let itemName = stacked ? _itemName : splitName[0];

        if (itemName.includes("bar")) {
          itemName = itemName.substr(0, itemName.length - 3);
        } else if (itemName.endsWith("area") || itemName.endsWith("line")) {
          itemName = itemName.substr(0, itemName.length - 4);
        }

        let itemValue = item.value;
        itemValue = formatTooltipValue(itemValue);

        itemName = itemName?.replaceAll("^", " ");

        return {
          label: toTitleCase(itemName),
          value: itemValue,
          color: item.fill
        };
      });
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

  const renderYAxises = useMemo(() => {
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
  }, [yAxisIds]);

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }
  return (
    <>
      <ResponsiveContainer>
        <ComposedChart
          data={filteredData}
          {...chartProps}
          barGap={0}
          margin={{
            top: 40,
            right: 10,
            left: 5,
            bottom: chartProps?.margin?.bottom !== undefined ? chartProps?.margin?.bottom : 50
          }}
          onClick={onGraphClick}>
          <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
          <XAxis
            hide={previewOnly}
            dataKey="name"
            stroke={chartTransparentStaticColors.axisColor}
            interval={data?.length > 25 ? "preserveStartEnd" : 0}
            minTickGap={data?.length > 50 ? 10 : data?.length > 25 ? 5 : 1}
            // tickLine={false}
            tick={<TiltedAxisTick />}
          />
          {renderYAxises}
          {!props.config?.disable_tooltip && (data || []).length > 0 && (
            <Tooltip cursor={false} content={renderTooltip} />
          )}
          {!hideLegend && <Legend content={<ChartLegendComponent filters={filters} setFilters={setFilters} />} />}
          {renderLines}
          {renderBars}
        </ComposedChart>
      </ResponsiveContainer>
    </>
  );
};

export default AzureResolutionChartComponent;
