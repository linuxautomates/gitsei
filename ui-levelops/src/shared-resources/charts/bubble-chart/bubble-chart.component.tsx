import React, { useEffect, useState, useCallback, useMemo } from "react";
import { get, uniq } from "lodash";
import {
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  TooltipPayload,
  TooltipProps,
  XAxis,
  YAxis,
  ZAxis
} from "recharts";

import widgetConstants from "dashboard/constants/widgetConstants";
import { chartTransparentStaticColors, lineChartColors } from "../chart-themes";
import { BubbleChartProps } from "../chart-types";
import { getFilteredData, getInitialFilters, sortTooltipListItems } from "../helper";
import { EmptyWidget } from "../../components";
import { ignoreKeys } from "../bar-chart/bar-chart.ignore-keys";
import { toTitleCase } from "utils/stringUtils";
import ChartLegendComponent from "../components/chart-legend/chart-legend.component";
import { round } from "utils/mathUtils";

const BubbleChartComponent = (props: BubbleChartProps) => {
  const {
    data,
    bubbleProps,
    chartProps,
    stacked,
    yunit,
    xunit,
    zunit,
    yunitLabel,
    hasClickEvents,
    onClick,
    id,
    xunitLabel,
    hideLegend
  } = props;

  const [filteredData, setFilteredData] = useState([]);
  const [filters, setFilters] = useState<any>({});

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const defaultFilterKey = get(widgetConstants, [props.reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(data, ignoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (filters) {
      const newData = getFilteredData(data, filters);
      setFilteredData(newData as any);
    }
  }, [filters]); // eslint-disable-line react-hooks/exhaustive-deps

  const onBubbleClick = useCallback((data: any) => {
    // Adding this check at top in order to prevent click event when graph don't have click event or data is not present
    // (i.e: Click is made on part of charts where data is null like x axis labels)
    if (data && hasClickEvents && onClick) {
      if (props.reportType?.includes("levelops")) {
        const _data = data?.activePayload?.[0]?.payload || {};
        onClick({ name: _data.name, id: _data.id });
      } else {
        onClick(data.activeLabel as string);
      }
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const yAxisLabel = useMemo(() => ({ value: yunitLabel, angle: -90, position: "insideLeft" } as any), []);
  const xAxisLabel = useMemo(() => ({ value: xunitLabel, angle: 0, position: "bottom" } as any), []);

  const tickFormatter = useCallback(
    value => (typeof value === "number" && value / 1000 >= 1 ? `${round(value / 1000, 2)}k` : value),
    []
  );

  const tooltipContent = useCallback(
    (tooltipProps: TooltipProps) => {
      const { active, payload } = tooltipProps;
      if (!!payload && payload.length > 0) {
        const uniqueKeys = uniq(payload?.map((item: any) => item.dataKey));
        const toolTipLabel = payload.find((item: TooltipPayload) => item.name === "name")?.payload.name;
        let listitems = uniqueKeys.map((item: any, i: number) => {
          const itemFill = bubbles.find((prop: any) => prop.dataKey === item)?.fill;
          if (item !== "name") {
            const subPayload = payload.find((i: any) => i.dataKey === item);
            let label: string | undefined;
            switch (item) {
              case "xAxis":
                label = xunit;
                break;
              case "yAxis":
                label = yunit;
                break;
              case "zAxis":
                label = zunit;
                break;
              default:
                label = "";
            }
            return {
              label: toTitleCase(label || ""),
              value: subPayload?.value,
              color: itemFill
            };
          }
          return {};
        });

        if (active) {
          return (
            <div className="custom-tooltip">
              <p>{toTitleCase(toolTipLabel) || ""}</p>
              <ul>{sortTooltipListItems(listitems)}</ul>
            </div>
          );
        }
      }
      return null;
    },
    [props]
  );

  const bubbles = useMemo(() => {
    return (bubbleProps || []).map((bubble, i) => {
      bubble.fill = lineChartColors[i % lineChartColors.length];
      return bubble;
    });
  }, [stacked, bubbleProps]);

  const renderBubbles = useMemo(() => {
    return (bubbleProps || []).map((bubble, i) => {
      return <Scatter cursor="pointer" key={`bubble-${i}`} hide={filters?.[bubble.dataKey] === false} {...bubble} />;
    });
  }, [bubbles, filters]);

  const chartStyle = useMemo(() => {
    return { ...chartProps?.margin, right: 5, bottom: 50 };
  }, [chartProps]);

  if ((data || []).length === 0) return <EmptyWidget />;
  return (
    <ResponsiveContainer>
      <ScatterChart data={filteredData} {...chartProps} margin={chartStyle} onClick={onBubbleClick}>
        <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
        <XAxis
          dataKey={"xAxis"}
          stroke={chartTransparentStaticColors.axisColor}
          interval={0}
          name={"name"}
          minTickGap={3}
          label={xAxisLabel}
        />
        <YAxis
          stroke={chartTransparentStaticColors.axisColor}
          allowDecimals={false}
          dataKey={"yAxis"}
          label={yAxisLabel}
          tickFormatter={tickFormatter}
        />
        <ZAxis type={"number"} dataKey={"zAxis"} range={[26, 325]} />
        {!props.config?.disable_tooltip && (data || []).length > 0 && (
          <Tooltip cursor={false} content={tooltipContent} />
        )}
        {!hideLegend && <Legend content={<ChartLegendComponent filters={filters} setFilters={setFilters} />} />}
        {renderBubbles}
      </ScatterChart>
    </ResponsiveContainer>
  );
};

export default BubbleChartComponent;
