import React, { useCallback, useMemo } from "react";
import { get, isArray, round } from "lodash";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { BAR_CHART_REF_LINE_STROKE } from "dashboard/constants/filter-key.mapping";
import {
  CHART_DATA_TRANSFORMERS,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE
} from "dashboard/constants/applications/names";
import { yAxisIdByKey } from "shared-resources/charts/helper";
import { TiltedAxisTick } from "shared-resources/charts";
import { chartTransparentStaticColors } from "shared-resources/charts/chart-themes";
import { AntIcon, AntText, EmptyWidget } from "shared-resources/components";
import cx from "classnames";
import { Tag } from "antd";
import { EffortInvestmentBarChartProps } from "shared-resources/charts/chart-types";
import { customBar } from "shared-resources/charts/jira-effort-allocation-chart/components/effort-investment-bar-chart/helper";

interface NewBarChartComponentProps extends EffortInvestmentBarChartProps {
  setActiveCategoryKey: (value: string) => void;
  activeCategoryKey: string;
}

const DemoEffortTrendBarChartComponent: React.FC<NewBarChartComponentProps> = props => {
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
    hideGrid,
    previewOnly = false,
    filters,
    colorsData,
    activeCategoryKey,
    setActiveCategoryKey
  } = props;

  const defaultBarSize: number = 30;
  const defaultGroupBarSize: number = 30;

  const yAxisIds = useMemo(() => {
    if (units && units.length) {
      return [...units, "hidden"];
    }
    return [unit, "hidden"];
  }, [unit, units]);

  const onBarClick = useCallback((data: any) => {
    if (onClick) {
      const values = (data?.activeLabel || "")?.split("-");
      const value = values?.[0]?.trim();
      let currentAllocation = false;
      const activePayload: any[] = get(data, ["activePayload"], []);
      if (activePayload?.length) {
        currentAllocation = get(activePayload[0], ["payload", "is_active_work"], false);
      }
      onClick({ widgetId: id, name: value, phaseId: value, activeData: currentAllocation });
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
      return parseInt(value);
    }
    return value;
  }, []);

  const tooltipContent = useCallback(
    (tooltipProps: TooltipProps) => {
      if (tooltipProps.active && activeCategoryKey) {
        const payload = get(tooltipProps, ["payload"], []);
        const activeCategoryKeyData: any = payload.find((item: any) => item.dataKey === activeCategoryKey);
        return (
          <div className="tool-tip-wrapper p-10">
            <div className="tooltip-header">
              <AntText>{activeCategoryKeyData?.name}</AntText>
              <Tag
                style={{ color: "#FFFFFF", backgroundColor: `${activeCategoryKeyData?.color}`, alignSelf: "flex-end" }}>
                {activeCategoryKeyData?.value?.toFixed(2)} %
              </Tag>
            </div>
            <hr className="tooltip-divider" />
            <AntIcon className="tooltip-icon mb-10 mt-10" type="team" theme="outlined" />
            <AntText className="tooltip-content-text">{activeCategoryKeyData?.value?.toFixed(2)}</AntText>
            <AntText>{unit ?? ""}</AntText>
          </div>
        );
      }
    },
    [barProps, props, activeCategoryKey]
  );

  function getColorForBar(dataKey: string) {
    return colorsData[dataKey];
  }

  const bars = useMemo(() => {
    if (stacked) {
      return (barProps || []).map((bar: any, i: number) => {
        bar.fill = getColorForBar(bar.dataKey);
        let yAxisId = "hidden";
        if (units?.length && yAxisIds.includes(yAxisIdByKey[bar.dataKey])) {
          yAxisId = yAxisIdByKey[bar.dataKey];
        } else if (unit && yAxisIds.includes(unit)) {
          yAxisId = unit;
        }
        return (
          <Bar
            onMouseLeave={() => {
              setActiveCategoryKey("");
            }}
            onMouseEnter={() => {
              setActiveCategoryKey(bar.dataKey);
            }}
            className={cx("noclass", { "bar-chart-cell": activeCategoryKey && bar.dataKey !== activeCategoryKey })}
            hide={filters?.[bar?.dataKey] == false}
            cursor={"pointer"}
            stackId={1}
            shape={(_props: any) => customBar({ ..._props, data })}
            yAxisId={yAxisId}
            barSize={defaultBarSize}
            key={`bar-${i}`}
            {...bar}>
            <Cell
              className={cx("noclass", { "bar-chart-cell": activeCategoryKey && bar.dataKey !== activeCategoryKey })}
              key={`bar-${i}`}
              fill={bar.fill}
            />
          </Bar>
        );
      });
    }
    return (barProps || []).map((bar: any, i: number) => {
      bar.fill = getColorForBar(bar.dataKey);
      return (
        <Bar
          onMouseLeave={() => {
            setActiveCategoryKey("");
          }}
          onMouseEnter={() => {
            setActiveCategoryKey(bar.dataKey);
          }}
          className={cx("noclass", { "bar-chart-cell": activeCategoryKey && bar.dataKey !== activeCategoryKey })}
          hide={filters?.[bar?.dataKey] == false}
          cursor={"pointer"}
          barSize={defaultGroupBarSize}
          yAxisId={yAxisIdByKey[bar.dataKey] || unit}
          key={`bar-${i}`}
          {...bar}
        />
      );
    });
  }, [stacked, barProps, defaultGroupBarSize, unit, units, filters, activeCategoryKey, data]);

  const barChartStyle = useMemo(() => {
    return { ...chartProps?.margin, right: 100, bottom: 50 };
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
          domain={[0, 100]}
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
      const dataPayload = data?.[dataIndex] || {};
      const xAxisTitleTransformer = getWidgetConstant(props.reportType, [
        CHART_DATA_TRANSFORMERS,
        CHART_X_AXIS_TITLE_TRANSFORMER
      ]);
      const xAxisTruncate = getWidgetConstant(
        props.reportType,
        [CHART_DATA_TRANSFORMERS, CHART_X_AXIS_TRUNCATE_TITLE],
        true
      );
      let xAxisLabel = xAxisProps?.payload?.value;
      if (xAxisTitleTransformer) {
        xAxisLabel = xAxisTitleTransformer(xAxisLabel, dataPayload, "name");
      }
      const tickProps = {
        ...(xAxisProps || {}),
        showXAxisTooltip: config?.showXAxisTooltip,
        truncate: xAxisTruncate,
        xAxisLabel
      };
      return <TiltedAxisTick {...tickProps} />;
    },
    [data, props.reportType, config]
  );

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <div className="demo-trend-bar-chart-wrapper">
      <ResponsiveContainer>
        <BarChart data={data} {...chartProps} margin={barChartStyle} onClick={onBarClick}>
          {!hideGrid && <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />}
          <XAxis
            hide={previewOnly}
            dataKey="name"
            stroke={chartTransparentStaticColors.axisColor}
            interval={0}
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

          {bars}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default DemoEffortTrendBarChartComponent;
