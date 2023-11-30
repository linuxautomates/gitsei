import { TIME_INTERVAL_TYPES } from "constants/time.constants";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import React, { useMemo, useState } from "react";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  Deployments,
  DoraBarChartResponseType,
  FailureRate
} from "shared-resources/containers/dora-api-container/types";
import { BarChartProps } from "../chart-types";
import { simplifyValueByInterval } from "../helper";
import DoraBarChart from "./DoraBarChart";
import "./doraChart.scss";
import DoraStatChart, { DoraStatChartProps } from "./DoraStatChart";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

export interface DoraChartProps extends Partial<BarChartProps> {
  apiData: DoraBarChartResponseType;
  chartTitle: string;
  statDescInterval?: string;
  showDoraGrading: boolean;
  onDoraClick: (data: any) => void;
  dateRangeValue: {
    $lt: number;
    $gt: number;
  };
  reportType?: string;
  filters?: any;
  chartType?: ChartType;
  // onFilterDropdownClick: (value: string) => void;
  dropdownFilterTypeValue: string;
  getProfileIntegrationApplication: string;
}

const DoraChart: React.FC<DoraChartProps> = ({
  apiData,
  barProps,
  chartProps,
  chartTitle,
  unit,
  statDescInterval,
  onDoraClick,
  hasClickEvents,
  showDoraGrading,
  dateRangeValue,
  id,
  reportType,
  filters,
  chartType,
  // onFilterDropdownClick,
  dropdownFilterTypeValue,
  getProfileIntegrationApplication
}) => {
  const dashboardTimeRangeData = filters?.filter?.time_range;
  const [clicked, setClicked] = useState<boolean>(false);
  const [interval, setInterval] = useState<
    TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH
  >(TIME_INTERVAL_TYPES.DAY);
  const onBarClickHandler = (
    date: string,
    interval: TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH,
    count: number
  ) => {
    setClicked(false);
    const onChartClickPayload = getWidgetConstant(reportType, ["onChartClickPayload"]);
    if (onChartClickPayload) {
      onDoraClick(onChartClickPayload({ date, interval, dashboardTimeRangeData, count }));
    }
  };

  // ENTITLEMENT FOR DF WIDGTE FOR GET DATA OF WEEK & MONTH FROM BE (IF ENTIITLEMENTS IS TRUE) OR CALCULATE FROM FE
  const dfConfigurableWeekMonth = useHasEntitlements(Entitlement.DF_CONFIGURABLE_WEEK_MONTH, EntitlementCheckType.AND);

  const valueUnit = useMemo(
    () =>
      apiData.stats.hasOwnProperty("count_per_day")
        ? simplifyValueByInterval(apiData.stats as Deployments, interval, dfConfigurableWeekMonth)
        : {
            simplifiedValue: (apiData.stats as FailureRate).is_absolute
              ? (apiData.stats as FailureRate).total_deployment
              : (apiData.stats as FailureRate).failure_rate || 0,
            unit: (apiData.stats as FailureRate).is_absolute ? "Failures" : "Failure Rate"
          },
    [apiData, interval, dfConfigurableWeekMonth]
  );

  const statClickHandler = () => {
    setClicked(true);
    onDoraClick({
      time_range: {
        $lt: dateRangeValue.$lt,
        $gt: dateRangeValue.$gt
      },
      statClicked: true
    });
  };

  const doraStatProps: DoraStatChartProps = useMemo(() => {
    return {
      value: valueUnit.simplifiedValue || 0,
      unit: valueUnit.unit,
      unitSymbol: apiData.stats.hasOwnProperty("is_absolute") && !(apiData.stats as FailureRate).is_absolute ? "%" : "",
      band: (apiData.stats as Deployments).band,
      count: apiData.stats.total_deployment,
      descInterval: statDescInterval,
      descStringValue: "deployments",
      isRelative: apiData.stats.hasOwnProperty("is_absolute") && !!!(apiData.stats as FailureRate).is_absolute,
      showDoraGrading,
      onClick: statClickHandler,
      reportType: reportType
    };
  }, [apiData.stats, valueUnit, statDescInterval]);

  const renderStacksChart = useMemo(() => {
    if (getProfileIntegrationApplication && getProfileIntegrationApplication.includes(IntegrationTypes.HARNESSNG)) {
      return true;
    }
    return false;
  }, [getProfileIntegrationApplication]);

  return (
    <div className="dora-chart">
      <div className="left-component">
        <DoraStatChart {...doraStatProps} clicked={clicked} setClicked={setClicked} />
      </div>
      <div className="right-component">
        <DoraBarChart
          apiData={apiData.time_series}
          barProps={barProps}
          chartProps={chartProps}
          title={chartTitle}
          unit={unit}
          onBarClick={onBarClickHandler}
          hasClickEvents={hasClickEvents}
          widgetId={id}
          renderStacksChart={renderStacksChart}
          // onFilterDropdownClick={onFilterDropdownClick}
          dropdownFilterTypeValue={dropdownFilterTypeValue}
          setInterval={setInterval}
          interval={interval}
        />
      </div>
    </div>
  );
};

export default DoraChart;
