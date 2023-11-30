import { Radio } from "antd";
import { TIME_INTERVAL_TYPES } from "constants/time.constants";
import { WidgetFilterContext } from "dashboard/pages/context";
import { uniq } from "lodash";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { AntTooltipComponent as AntTooltip } from "shared-resources/components/ant-tooltip/ant-tooltip.component";
import { getXAxisTimeLabel } from "utils/dateUtils";
import BarChart from "../bar-chart/bar-chart.component";
import { BarChartProps } from "../chart-types";
import { INSUFFICIENTDATAPOINTMESSAGE, TOMANYDATAPOINTMESSGAE } from "../helper";
import "./doraBarChart.scss";
import { Select } from "antd";

const { Option } = Select;

interface DoraBarChartProps extends Partial<BarChartProps> {
  apiData: {
    day: any[];
    week: any[];
    month: any[];
  };
  title: string;
  onBarClick?: (
    date: string,
    interval: TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH,
    count: number
  ) => void;
  widgetId?: string;
  renderStacksChart?: boolean;
  // onFilterDropdownClick: (value: string) => void;
  dropdownFilterTypeValue: string;
  setInterval: (value: TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH) => void;
  interval: TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH;
}

const DoraBarChart: React.FC<DoraBarChartProps> = ({
  apiData,
  barProps,
  chartProps,
  title,
  unit,
  onBarClick,
  hasClickEvents,
  widgetId,
  renderStacksChart,
  // onFilterDropdownClick,
  dropdownFilterTypeValue,
  setInterval,
  interval
}) => {
  const { setFilters } = useContext(WidgetFilterContext);

  // const [interval, setInterval] = useState<
  //   TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH
  // >(TIME_INTERVAL_TYPES.DAY);
  const [barData, setBarData] = useState<any>({});
  const [availableIntervals, setAvailableIntervals] = useState<string[]>();
  const [transformedData, setTrasformedData] = useState<any>({});
  const [dataOneTimeCheck, setDataOneTimeCheck] = useState<boolean>(true);

  useEffect(() => {
    setFilters(widgetId as string, {
      doraTitle: title,
      doraInterval: interval
    });
  }, [interval, title, renderStacksChart]);

  const intervalButtons = useMemo(() => {
    let disabledDay = availableIntervals?.includes("day");
    let disabledWeek = availableIntervals?.includes("week");
    let disabledMonth = availableIntervals?.includes("month");
    let changeWeekMessage = disabledMonth && !disabledWeek ? true : false;

    return (
      <Radio.Group className="" value={interval} onChange={e => setInterval(e.target.value)}>
        <AntTooltip title={!disabledDay ? TOMANYDATAPOINTMESSGAE : ""}>
          <Radio.Button value="day" disabled={!disabledDay}>
            Day
          </Radio.Button>
        </AntTooltip>
        <AntTooltip
          title={!disabledWeek ? (changeWeekMessage ? TOMANYDATAPOINTMESSGAE : INSUFFICIENTDATAPOINTMESSAGE) : ""}>
          <Radio.Button value="week" disabled={!disabledWeek}>
            Week
          </Radio.Button>
        </AntTooltip>
        <AntTooltip title={!disabledMonth ? INSUFFICIENTDATAPOINTMESSAGE : ""}>
          <Radio.Button value="month" disabled={!disabledMonth}>
            Month
          </Radio.Button>
        </AntTooltip>
      </Radio.Group>
    );
  }, [interval, setInterval, availableIntervals, renderStacksChart]);

  useEffect(() => {
    if (dataOneTimeCheck) {
      if (apiData.day.length < 7) {
        setAvailableIntervals([TIME_INTERVAL_TYPES.DAY]);
        setInterval(TIME_INTERVAL_TYPES.DAY);
      } else if (apiData.day.length < 31) {
        setAvailableIntervals([TIME_INTERVAL_TYPES.DAY, TIME_INTERVAL_TYPES.WEEK, TIME_INTERVAL_TYPES.MONTH]);
        setInterval(TIME_INTERVAL_TYPES.DAY);
      } else if (apiData.week.length < 30) {
        setAvailableIntervals([TIME_INTERVAL_TYPES.WEEK, TIME_INTERVAL_TYPES.MONTH]);
        setInterval(TIME_INTERVAL_TYPES.WEEK);
      } else {
        setAvailableIntervals([TIME_INTERVAL_TYPES.MONTH]);
        setInterval(TIME_INTERVAL_TYPES.MONTH);
      }
      setDataOneTimeCheck(false);
    }
  }, [apiData, dataOneTimeCheck, setDataOneTimeCheck]);

  const transformDataKey = (data: Array<{ key: string; count: number }>) => {
    let barChartData = data.map(item => ({
      ...item,
      name: getXAxisTimeLabel({ key: item.key, interval: interval || "day" })
    }));
    return { chartData: barChartData };
  };

  const transformDataKeyStack = (data: Array<{ key: string; count: number }>) => {
    let barChartKey: any[] = [];
    let barChartData = data.reduce((acc: any, defaultData: any) => {
      let stacksObjects: any = {};
      let stacksTotalCount: number = 0;
      defaultData.stacks?.map((stackedData: { key: string; count: number }) => {
        stacksObjects[stackedData.key] = Number(stackedData.count);
        stacksTotalCount += Number(stackedData.count);
        barChartKey.push(stackedData.key);
      });
      let convertTime = getXAxisTimeLabel({ key: defaultData.key, interval: interval || "day" });

      acc.push({
        name: convertTime,
        ...(stacksTotalCount > 0 ? { totalStacksBarCount: stacksTotalCount } : {}),
        ...stacksObjects
      });
      return acc;
    }, []);

    let chartKeyData = uniq(barChartKey?.map((item: any) => item))?.map(item => ({
      name: item,
      dataKey: item
    }));

    return { chartData: barChartData, chartKey: chartKeyData };
  };

  useEffect(() => {
    // @ts-ignore
    if (interval && apiData[interval]) {
      if (transformedData[interval]) {
        setBarData(transformedData[interval]);
        return;
      }
      // @ts-ignore
      const transData = renderStacksChart
        ? transformDataKeyStack(apiData[interval])
        : transformDataKey(apiData[interval]);

      setTrasformedData({
        ...transformedData,
        [interval]: transData
      });
      setBarData(transData);
    }
  }, [interval, apiData, renderStacksChart]);

  const onClickHandler = useCallback(
    (date: string) => {
      const count = barData.chartData.find((bar: any) => bar.name === date)?.count;
      if (onBarClick) onBarClick(date, interval, count);
    },
    [interval, barData, renderStacksChart]
  );

  // COMMNET THIS CODE BECAUSE IN THIS SPRINT WE ARE ONLY SUPPORT PIPELINE DATA SO NO NEED OF THIS , BUT IN FUTURE THIS WILL COME
  // const filterDropdown = useMemo(() => {
  //   return (
  //     <AntSelect
  //       className="filter-dropdown"
  //       value={dropdownFilterTypeValue}
  //       onChange={onFilterDropdownClick}
  //     >
  //       {DORA_HARNESS_STACK_DROPDOWN.map((int: any) => (<Option value={int.key}>{int.value}</Option>))}
  //     </AntSelect>
  //   );
  // }, [interval, dropdownFilterTypeValue, onFilterDropdownClick])

  return (
    <div className="dora-bar-chart">
      <div className="bar-chart">
        <BarChart
          data={barData.chartData || []}
          barProps={renderStacksChart ? barData.chartKey || [] : barProps || []}
          stacked={renderStacksChart ? true : false}
          hideLegend={renderStacksChart ? false : true}
          readOnlyLegend={renderStacksChart ? true : false}
          chartProps={chartProps}
          unit={unit}
          hasTrendLikeData
          onClick={onClickHandler}
          hasClickEvents={hasClickEvents}
          showValueOnBarStacks={renderStacksChart}
        />
      </div>
      <div className="interval-buttons">
        {/* {renderStacksChart && filterDropdown} */}
        {intervalButtons}
      </div>
    </div>
  );
};

export default DoraBarChart;
