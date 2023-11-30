import React, { useState, useCallback, useMemo } from "react";
import {
  getDashboardTimeGtValue,
  getDashboardTimeLtValue
} from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { ScorecardDashboardTimeRangeOptions } from "dashboard/components/dashboard-view-page-secondary-header/constants";
import { strIsEqual } from "utils/stringUtils";
import { DrillDownProps } from "../../helper";
import { WebRoutes } from "../../../../../routes/WebRoutes";
import FeatureDrillDownComponent from "../../../../../dashboard/pages/scorecard/components/FeatureDrillDownComponent";
import LineGraphForDevRawStatsDrilldown from "./LineGraphForDevRawStatsDrilldown";
import { intervalToAggIntervalMapping } from "./constants";
import moment from "moment";
import { DateFormats, dateRangeFromStartMoment } from "utils/dateUtils";
import { LineGraphDataSourceType } from "shared-resources/charts/chart-types";
import { timeInterval } from "dashboard/constants/devProductivity.constant";
import { ProjectPathProps } from "classes/routeInterface";
import { useParams } from "react-router-dom";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

interface DevRawStatsDrilldownProps {
  drilldownHeaderProps: any;
  drillDownProps: DrillDownProps;
}

const DevRawStatsDrilldownWrapper: React.FC<DevRawStatsDrilldownProps> = (props: DevRawStatsDrilldownProps) => {
  const { drillDownProps, drilldownHeaderProps } = props;
  const { widgetId } = drillDownProps;
  const { onDrilldownClose } = drilldownHeaderProps;
  const { columnName, record, interval, dashboardOuIds } = drillDownProps?.x_axis;
  const user_id = record?.org_user_id;
  const user_id_type = "ou_user_ids";
  const name = record?.full_name;
  const [pageSize, setPageSize] = useState<number>(10);
  const [page, setPage] = useState<number>(1);
  const [intervalKey, setIntervalKey] = useState<string>(
    moment.unix(getDashboardTimeGtValue(interval)).format(DateFormats.MONTH_YEAR_NUMBER)
  );
  const projectParams = useParams<ProjectPathProps>();
  const {
    utils: { getLocationPathName }
  } = useParentProvider();

  const dateRange = useMemo(
    () => dateRangeFromStartMoment(moment(intervalKey, DateFormats.MONTH_YEAR_NUMBER), interval),
    [interval, intervalKey]
  );
  const dashboardTimeGtValue = useMemo(
    () => (record?.start_time ? record?.start_time : dateRange?.["$gt"]),
    [record, dateRange]
  );
  const dashboardTimeLtValue = useMemo(
    () => (record?.end_time ? record?.end_time : dateRange?.["$lt"]),
    [record, dateRange]
  );

  const handlePageSizeChange = useCallback(
    (page_size: number) => {
      setPage(1);
      setPageSize(page_size);
    },
    [pageSize, setPageSize, setPage]
  );

  const selectedTimeRange = ScorecardDashboardTimeRangeOptions.find(item =>
    strIsEqual(item.key, interval.toLowerCase())
  )?.label;

  const onOpenReports = useCallback(() => {
    const url = WebRoutes.dashboard.scorecardDrillDown(
      projectParams,
      user_id as string,
      user_id_type as string,
      columnName || "",
      dashboardTimeGtValue,
      dashboardTimeLtValue,
      undefined,
      typeof interval === "string" ? interval.toUpperCase() : ""
    );
    window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
  }, [user_id, user_id_type, columnName, dashboardTimeGtValue, dashboardTimeLtValue, interval]);

  const onChartClick = (data: LineGraphDataSourceType) => {
    setIntervalKey(data?.additional_key);
  };

  const lineChart = useMemo(() => {
    if (
      [
        timeInterval.LAST_WEEK,
        timeInterval.LAST_TWO_WEEKS,
        timeInterval.LAST_TWO_MONTHS,
        timeInterval.LAST_THREE_MONTHS
      ].includes(interval)
    ) {
      return null;
    }
    const filters = {
      filter: {
        agg_interval: intervalToAggIntervalMapping[interval],
        ou_ref_ids: dashboardOuIds,
        no_comparison: true,
        report_requests: [
          {
            id: user_id,
            id_type: "ou_user_ids"
          }
        ]
      }
    };
    return (
      <LineGraphForDevRawStatsDrilldown
        filters={filters}
        widgetId={widgetId}
        columnName={columnName}
        interval={interval}
        onChartClick={onChartClick}
        additionalKey={intervalKey}
      />
    );
  }, [widgetId, columnName, dashboardOuIds, interval, user_id, intervalKey]);

  return (
    <FeatureDrillDownComponent
      selectedFeature={columnName}
      user_id={user_id}
      user_id_type={user_id_type}
      setSelectedFeature={columnName}
      dashboardTimeGtValue={dashboardTimeGtValue}
      dashboardTimeLtValue={dashboardTimeLtValue}
      dashboardTimeRange={selectedTimeRange}
      pageSize={pageSize}
      onOpenReports={onOpenReports}
      onPageSizeChange={handlePageSizeChange}
      page={page}
      setPage={setPage}
      isDevRawStatsDrilldown={true}
      onDrilldownClose={onDrilldownClose}
      nameForTitle={name}
      extraPropsForGraph={lineChart}
      isDashboardDrilldown={true}
      interval={interval}
    />
  );
};

export default DevRawStatsDrilldownWrapper;
