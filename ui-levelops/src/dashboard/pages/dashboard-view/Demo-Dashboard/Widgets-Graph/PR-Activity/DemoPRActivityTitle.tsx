import React from "react";
import { AntText, CustomSelect } from "shared-resources/components";
import { viewByOptions } from "./../../../../../pages/scorecard/components/PRActivity/helpers";
import DemoWeekSelectionComponent from "./../../../../../pages/dashboard-view/Demo-Dashboard/Widgets-Graph/PR-Activity/DemoWeekSelectionComponent";

interface PRActivityTitleProps {
  title: string;
  isWidget?: boolean;
  viewBy?: string;
  setViewBy?: (viewby: string) => void;
  dashboardTimeGtValue?: string;
  dashboardTimeLtValue?: string;
  setSelectedTimeRange: (timeRange: any) => void;
  displayOnlyTitle?: boolean;
  isTrellisDemoWidget?: boolean;
}

const DemoPRActivityTitle = (props: PRActivityTitleProps) => (
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
        <DemoWeekSelectionComponent
          dashboardTimeGtValue={props.dashboardTimeGtValue as string}
          dashboardTimeLtValue={props.dashboardTimeLtValue as string}
          setSelectedTimeRange={props.setSelectedTimeRange}
        />
      </div>
    )}
    {props.isTrellisDemoWidget && <div className="child" />}
    {!props.isTrellisDemoWidget && (
      <div className="child pr-activity-interval">
        <AntText className="interval-label">Interval: </AntText>&nbsp;
        <AntText className="interval-month">Last Month</AntText>
      </div>
    )}
  </div>
);

export default DemoPRActivityTitle;
