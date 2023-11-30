import React, { useMemo, useState } from "react";
import { Popover } from "antd";
import { AntIcon, AntText } from "shared-resources/components";
import DemoDashboardHeaderDateRangePickerWrapper from "dashboard/pages/dashboard-view/Demo-Dashboard/DemoDashboardHeaderDateRangePickerWrapper/DemoDashboardHeaderDateRangePickerWrapper";
import {
  getDashboardTimeRangeDateValue,
  timeStampToValue
} from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { ScorecardDashboardTimeRangeOptions } from "dashboard/components/dashboard-view-page-secondary-header/constants";
import "../../../components/dashboard-view-page-secondary-header/dashboard-view-page-secondary-header.style.scss";

interface DemoScorecardSecHeaderProps {
  dashboardTimeRange: any;
}

const DemoScoreCardSecondaryHeaderComponent: React.FC<DemoScorecardSecHeaderProps> = ({ dashboardTimeRange }) => {
  const [popOverVisible, setPopOverVisible] = useState<boolean>(false);

  const datePickerCloseHandler = () => {
    setPopOverVisible(false);
  };

  const content = useMemo(() => {
    return (
      <DemoDashboardHeaderDateRangePickerWrapper
        closeHandler={datePickerCloseHandler}
        intervalString={dashboardTimeRange}
        dashboardTimeRangeOptions={ScorecardDashboardTimeRangeOptions}
        isDevProductivityReports
      />
    );
  }, [dashboardTimeRange]);

  const dashboardTimeGtValue = useMemo(() => {
    return typeof dashboardTimeRange === "string"
      ? getDashboardTimeRangeDateValue(dashboardTimeRange || "last_quarter", "$gt")
      : timeStampToValue(dashboardTimeRange?.[`$gt`] || {});
  }, [dashboardTimeRange]);

  const dashboardTimeLtValue = useMemo(() => {
    return typeof dashboardTimeRange === "string"
      ? getDashboardTimeRangeDateValue(dashboardTimeRange || "last_quarter", "$lt")
      : timeStampToValue(dashboardTimeRange?.[`$lt`] || {});
  }, [dashboardTimeRange]);

  const typeOfDashboardTimeFilterString = typeof dashboardTimeRange === "string";

  return (
    <div className="dashboard-view-secondary-header" style={{ justifyContent: "space-between" }}>
      <div className="dashboard-view-secondary-header-right flex justify-end">
        <div className="flex justify-center align-center">
          <Popover
            visible={popOverVisible}
            className="secondary-header-dropdown-popover-wrapper"
            content={content}
            trigger={"click"}
            placement="bottomRight">
            <div
              className="dashboard-view-secondary-header-popover-content"
              onClick={() => setPopOverVisible(state => !state)}>
              <AntIcon type="calendar" className="mr-10" theme="outlined" />
              <AntText className="mr-5" style={{ fontWeight: "500" }}>
                {typeOfDashboardTimeFilterString &&
                  ScorecardDashboardTimeRangeOptions.find(item => item.key === dashboardTimeRange)?.label}
              </AntText>
              <AntText className="text-style">{typeOfDashboardTimeFilterString && "|"}</AntText>
              <AntText className=" text-style mr-10 ml-5">{dashboardTimeGtValue}</AntText>-
              <AntText className=" text-style ml-10 mr-5">{dashboardTimeLtValue}</AntText>
              <AntIcon type="down" className="ml-5" style={{ color: "#096DD9", fontSize: "12px" }} />
            </div>
          </Popover>
        </div>
      </div>
    </div>
  );
};

export default DemoScoreCardSecondaryHeaderComponent;
