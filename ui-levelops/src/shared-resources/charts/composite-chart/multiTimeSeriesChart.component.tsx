import { CHART_DATA_TRANSFORMERS, CHART_X_AXIS_TITLE_TRANSFORMER } from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get, startCase, uniq } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
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
import { default as EmptyWidget } from "shared-resources/components/empty-widget/empty-widget.component";
import "../bar-chart/bar-chart.style.scss";
import { chartTransparentStaticColors } from "../chart-themes";
import { BarChartProps } from "../chart-types";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import { compositeYAxisIdByKey, formatTooltipValue, getInitialFilters, sortTooltipListItems } from "../helper";
import { lineChartColors } from "../chart-themes";
import { default as TiltedAxisTick } from "../components/tilted-axis-tick";
import { staticPriorties } from "../jira-prioirty-chart/helper";
import { round } from "utils/mathUtils";

const MultiTimeSeriesChartComponent = (props: BarChartProps) => {
  const { data, chartProps, id, previewOnly, hideLegend, unit } = props;
  const ignoreKeys = ["name", "total_tickets", "key", "toolTip", "total_cases", "additional_key", "timestamp"];
  const [filters, setFilters] = useState<any>({});
  const stacked = ((data?.[0] as any) || {}).stacked;
  const [filteredData, setFilteredData] = useState([]);
  const stacks = ((data?.[0] as any) || {}).stacks;

  const getLineChartStyle = useCallback(
    (index: number) => {
      let fill = lineChartColors[lineChartColors.length - (index % lineChartColors.length) - 1];
      let stroke = fill;
      let strokeWidth = 1.5;
      return { fill, stroke, strokeWidth };
    },
    [props.reportType]
  );

  const units = useMemo(() => {
    const allKeys = uniq(
      Object.keys(filteredData.reduce((acc, next: any) => ({ ...acc, ...next }), {})).map(key => key.split("^^")[0])
    );
    const hasDays = allKeys.some(
      (key: string) =>
        key.includes("median") || key.includes("percentile") || key.includes("average") || key.includes("p90")
    );
    const hasTickets = allKeys.some((key: string) => key.includes("tickets") || key.includes("stacks"));
    const allUnits: string[] = [];
    if (hasTickets) {
      allUnits.push("Tickets");
    }
    if (hasDays) {
      allUnits.push("Days");
    }
    return allUnits;
  }, [filteredData]);

  const hasStoryPoints = useMemo(() => {
    const allKeys = uniq(
      Object.keys(filteredData.reduce((acc, next: any) => ({ ...acc, ...next }), {})).map(key => key.split("^^")[0])
    );
    return allKeys.some((key: string) => key.includes("story_points"));
  }, [filteredData]);

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

  const metrics = useMemo(() => {
    return ["total_tickets", "number_of_tickets_closed"];
  }, []);

  const getYAxisId = useCallback(
    (key: any) => {
      let yAxisId = "hidden";
      if (units?.length && yAxisIds.includes(compositeYAxisIdByKey[key])) {
        yAxisId = compositeYAxisIdByKey[key];
      } else if (unit && yAxisIds.includes(unit)) {
        yAxisId = unit;
      }
      return yAxisId;
    },
    [units, yAxisIds]
  );

  const allBarKeys = useMemo(() => {
    const singleObject = filteredData.reduce((acc, next: any) => ({ ...acc, ...next }), {});
    const keys = Object.keys(singleObject).filter(key => key.includes("^^") && key.includes("bar"));
    return uniq(keys);
  }, [filteredData]);

  const allStackKeys = useMemo(() => {
    const singleObject = filteredData.reduce((acc, next: any) => ({ ...acc, ...next }), {});
    const keys = Object.keys(singleObject).filter(key => key.includes("^^") && key.includes("stack"));
    return uniq(keys);
  }, [filteredData]);

  const allLineKeys = useMemo(() => {
    const singleObject = filteredData.reduce((acc, next: any) => ({ ...acc, ...next }), {});
    const keys = Object.keys(singleObject).filter(key => key.includes("^^") && key.includes("line"));
    return uniq(keys);
  }, [filteredData]);

  const renderBars = useMemo(() => {
    let series: any[] = [];
    let colorIndex = 0;

    metrics.forEach((metricKey, stackIndex) => {
      const filteredKeys = allBarKeys.filter(_key => _key.includes(metricKey));
      filteredKeys.forEach(key => {
        const yAxisId = getYAxisId(key.split("^^")[0]);
        colorIndex = colorIndex + 1;
        series.push(
          <Bar
            yAxisId={yAxisId}
            key={key}
            barSize={30}
            hide={filters[key] === false}
            dataKey={key}
            fill={lineChartColors[colorIndex % lineChartColors.length]}
          />
        );
      });
    });

    allStackKeys.forEach((stackKey: string) => {
      const yAxisId = getYAxisId(stackKey.split("^^")[0]);
      const stackId = stackKey.split("^^")?.[1] || 1;
      colorIndex = colorIndex + 1;
      series.push(
        <Bar
          yAxisId={yAxisId}
          key={stackKey}
          barSize={30}
          hide={filters[stackKey] === false}
          dataKey={stackKey}
          fill={lineChartColors[colorIndex % lineChartColors.length]}
          stackId={stackId}
        />
      );
    });

    return series;
  }, [allBarKeys, allStackKeys, getYAxisId, filters]);

  const renderLines = useMemo(() => {
    let series: any[] = [];
    let colorIndex = 0;
    allLineKeys.forEach((key, index) => {
      const yAxisId = getYAxisId(key.split("^^")[0]);
      colorIndex = colorIndex + 1;
      series.push(
        <Line
          activeDot={{ r: 4 }}
          yAxisId={yAxisId}
          key={key}
          hide={filters[key] === false}
          fill={getLineChartStyle(index).fill}
          stroke={getLineChartStyle(index).stroke}
          strokeWidth={getLineChartStyle(index).strokeWidth}
          dataKey={key}
          connectNulls={true}
        />
      );
    });

    return series;
  }, [allLineKeys, getYAxisId, filters]);

  const renderTooltip = (tProps: TooltipProps) => {
    const { active, payload, label } = tProps;
    if (payload !== undefined && payload !== null && payload.length > 0) {
      const listitems = payload.map((item: TooltipPayload, i: number) => {
        //The tooltip key will be now compositeTransformKey_widgetName
        const splitName = (item?.dataKey?.toString() as string).split("^^");

        let _itemName = item.name;

        if (stacks?.[0] === "priority") {
          const splitValue = _itemName.split("^");
          _itemName = splitValue[0] + " " + get(staticPriorties, [splitValue[1]], _itemName);
        }

        const tooltipMapping = get(widgetConstants, [props.reportType || "", "tooltipMapping"], {});

        let itemName = stacked
          ? _itemName
          : `${tooltipMapping?.[splitName[0]] ?? splitName[0] ?? ""} - ${splitName[1] ?? ""}`;

        let itemValue = item.value;
        itemValue = formatTooltipValue(itemValue);

        return {
          label: startCase(itemName),
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
      let name = yAxis;
      if (yAxis === "Tickets" && hasStoryPoints) {
        name = "Tickets/Story Points";
      }
      const label: any = { value: name, angle: -90, position: index ? "right" : "insideLeft" };
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
  }, [yAxisIds, hasStoryPoints]);

  const renderXAxisTick = useCallback(
    (xAxisProps: any) => {
      const dataIndex = xAxisProps?.payload?.index;
      const dataPayload = filteredData?.[dataIndex] || {};
      const xAxisTitleTransformer = getWidgetConstant(props.reportType, [
        CHART_DATA_TRANSFORMERS,
        CHART_X_AXIS_TITLE_TRANSFORMER
      ]);
      let xAxisLabel = xAxisProps?.payload?.value;
      if (xAxisTitleTransformer) {
        xAxisLabel = xAxisTitleTransformer(xAxisLabel, dataPayload, "name");
      }
      const tickProps = {
        ...(xAxisProps || {}),
        truncate: false,
        xAxisLabel
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [filteredData, props.reportType]
  );

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
          }}>
          <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
          <XAxis
            hide={previewOnly}
            dataKey="name"
            stroke={chartTransparentStaticColors.axisColor}
            interval={filteredData?.length > 25 ? "preserveStartEnd" : 0}
            minTickGap={filteredData?.length > 50 ? 10 : filteredData?.length > 25 ? 5 : 1}
            // tickLine={false}
            tick={renderXAxisTick}
          />
          {renderYAxises}
          {!props.config?.disable_tooltip && (filteredData || []).length > 0 && (
            <Tooltip cursor={false} content={renderTooltip} />
          )}
          {!hideLegend && <Legend content={<ChartLegendComponent filters={filters} setFilters={setFilters} />} />}
          {renderBars}
          {renderLines}
        </ComposedChart>
      </ResponsiveContainer>
    </>
  );
};

export default MultiTimeSeriesChartComponent;
