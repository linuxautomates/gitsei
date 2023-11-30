import { useDemoDashboardDataId } from "custom-hooks/useDemoDashboardDataKey";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import React, { useMemo } from "react";
import { useSelector } from "react-redux";
import { useLocation } from "react-router-dom";
import { getDateRangeEpochToString } from "utils/dateUtils";
import queryString from "query-string";
import DoraChart from "shared-resources/charts/dora-chart/DoraChart";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { PhaseChartIssues } from "shared-resources/charts";
import Loader from "components/Loader/Loader";

const DemoCombinedBarChart = (props: any) => {
  const { id, reportType, query, chartType, hasClickEvents, onClick, dateRangeValue } = props;
  const demoDataKey = useDemoDashboardDataId(id);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;

  const widgetData = useSelector(state =>
    get(
      state,
      [
        "restapiReducer",
        "selected-dashboard",
        "metadata",
        "demo_data",
        props.id as string,
        "data",
        demoDataKey as string,
        "data"
      ],
      {}
    )
  );
  const statDescInterval = useMemo(() => getDateRangeEpochToString(dateRangeValue), [dateRangeValue]);

  const transformedData = useMemo(() => {
    const transFunc = get(widgetConstants, [reportType, "transformFunction"], undefined);
    if (transFunc && widgetData.length) {
      return transFunc({ reportType, apiData: widgetData, metadata: {} })?.data || [];
    }
    return widgetData;
  }, [widgetData]);

  const filters = useMemo(
    () => ({
      filter: {
        ...(query || {}),
        time_range: dateRangeValue
      },
      ou_ids: [queryParamOU]
    }),
    [query, queryParamOU, dateRangeValue]
  );

  const widgetChartProps = useMemo(() => {
    const chartPropsFunc = get(widgetConstants, [reportType, "getChartProps"], undefined);
    if (chartPropsFunc) {
      return chartPropsFunc({ widgetMetaData: {} });
    }
    return {};
  }, [reportType]);

  const onClickHandler = (data: any) => {
    if (onClick) {
      onClick({
        ...data,
        widgetId: id,
        name: getDateRangeEpochToString(data.time_range)
      });
    }
  };

  const updatedChartProps = {
    ...props,
    apiData: widgetData,
    trendData: {},
    dateRange: dateRangeValue,
    onDoraClick: onClickHandler,
    hasClickEvents,
    statDescInterval,
    showDoraGrading: true,
    reportType: reportType,
    id: id,
    widgetMetaData: {},
    dateRangeValue: dateRangeValue,
    // workflowProfile: workflowProfile,
    filters,
    data: transformedData,
    // onClick: onClickHandler,
    // onFilterDropdownClick: onFilterDropdownClick,
    // dropdownFilterTypeValue: dropdownFilterTypeValue,
    getProfileIntegrationApplication: "jira",
    ...widgetChartProps
  };

  if (!widgetData || Object.keys(widgetData).length <= 0) {
    return <Loader />;
  }

  switch (chartType) {
    case ChartType.DORA_COMBINED_BAR_CHART:
      return <DoraChart {...(updatedChartProps as any)} />;
    default:
      return <PhaseChartIssues {...(updatedChartProps as any)} isDemo />;
  }
};

export default DemoCombinedBarChart;
