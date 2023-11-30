import {
  getDashboardTimeGtValue,
  getDashboardTimeLtValue
} from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { WidgetFilterContext } from "dashboard/pages/context";
import { viewByOptions } from "dashboard/pages/scorecard/components/PRActivity/helpers";
import PRActivityTitle from "dashboard/pages/scorecard/components/PRActivity/PRActivityTitle";
import React, { useContext, useEffect, useMemo, useState } from "react";
import { getDate, getTimeStamp, getWeekStartEnd } from "utils/dateUtils";

interface PRActivityReportTitleProps {
  title: string;
  dashboardTimeRange: any;
  widgetId: string;
  displayOnlyTitle?: boolean;
}

const PRActivityReportTitle = (props: PRActivityReportTitleProps) => {
  const { title, dashboardTimeRange, widgetId, displayOnlyTitle } = props;
  const [viewBy, setViewBy] = useState<string>(viewByOptions[0].key);
  const { setFilters } = useContext(WidgetFilterContext);

  const dashboardTimeGtValue = useMemo(() => getDashboardTimeGtValue(dashboardTimeRange), [dashboardTimeRange]);

  const dashboardTimeLtValue = useMemo(() => getDashboardTimeLtValue(dashboardTimeRange), [dashboardTimeRange]);

  const firstTimeRange = useMemo(() => {
    const startDate = getDate(dashboardTimeLtValue);
    const week = getWeekStartEnd(startDate);
    return {
      $gt: `${getTimeStamp(week.start)}`,
      $lt: `${getTimeStamp(week.end)}`
    };
  }, [dashboardTimeLtValue]);

  const [time_range, setSelectedTimeRange] = useState<any>(firstTimeRange);
  useEffect(() => {
    if (time_range && viewBy) {
      setFilters(widgetId, {
        time_range,
        across: viewBy
      });
    }
  }, [time_range, viewBy]);

  return (
    <PRActivityTitle
      title={title}
      dashboardTimeGtValue={dashboardTimeGtValue}
      dashboardTimeLtValue={dashboardTimeLtValue}
      setSelectedTimeRange={setSelectedTimeRange}
      isWidget
      viewBy={viewBy}
      setViewBy={setViewBy}
      displayOnlyTitle={displayOnlyTitle}
    />
  );
};

export default PRActivityReportTitle;

export const getPRActivityReportTitle = (args: any) => {
  return (
    <PRActivityReportTitle
      title={args.title}
      dashboardTimeRange={args.interval}
      widgetId={args.widgetId}
      displayOnlyTitle={args.displayOnlyTitle}
    />
  );
};
