import React, { useState, useMemo } from "react";
import { CustomSelect } from "shared-resources/components";
import { viewByOptions } from "./helpers";
import { getDashboardTimeRangeDateValue } from "./../../../../components/dashboard-view-page-secondary-header/helper";
import { getDashboardTimeRangeDate } from "./../../../../components/dashboard-view-page-secondary-header/helper";
import WeekSelectionComponent from "./WeekSelectionComponent";

interface PRActivityTitleProps {
  title: string;
  isWidget?: boolean;
  viewBy?: string;
  setViewBy?: (viewby: string) => void;
  dashboardTimeGtValue: string;
  dashboardTimeLtValue: string;
  setSelectedTimeRange: (timeRange: any) => void;
  displayOnlyTitle?: boolean;
}

const PRActivityTitle = (props: PRActivityTitleProps) => {
  const dashboardTimeGtValueTemp = useMemo(() => {
    return getDashboardTimeRangeDate("last_month", "$gt")?.unix();
  }, []);
  const dashboardTimeLtValueTemp = useMemo(() => {
    return getDashboardTimeRangeDate("last_month", "$lt")?.unix();
  }, []);

  return (
    <div className="pr-activity-header">
      <div className="child pr-activity-title-flex">
        {props.title}
        {props.isWidget && !props.displayOnlyTitle && (
          <CustomSelect
            options={viewByOptions}
            labelKey="label"
            valueKey="key"
            mode={"default"}
            value={props.viewBy}
            onChange={props.setViewBy || (() => {})}
            createOption={false}
            labelCase={"none"}
            className="pr-activity-by"
            showSearch={false}
          />
        )}
      </div>
      {!props.displayOnlyTitle && (
        <div className="child pr-activity-week-selection">
          <WeekSelectionComponent
            dashboardTimeGtValue={props.dashboardTimeGtValue}
            dashboardTimeLtValue={props.dashboardTimeLtValue}
            setSelectedTimeRange={props.setSelectedTimeRange}
          />
        </div>
      )}
      <div className="child" />
    </div>
  );
};

export default PRActivityTitle;
