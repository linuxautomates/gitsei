import React, { useMemo, useState } from "react";
import { Popover, Select } from "antd";
import { AntIcon, AntSelect, AntText } from "../../../shared-resources/components";
import DashboardHeaderDateRangePickerWrapper from "./DateRangePickerHeaderWrapper";
import { getDashboardTimeRangeDateValue, timeStampToValue } from "./helper";
import { ScorecardDashboardTimeRangeOptions, ScorecardDashboardTimeRangeOptionsOld } from "./constants";
import "./dashboard-view-page-secondary-header.style.scss";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { GetInfoIcon } from "dashboard/reports/dev-productivity/helper";

interface ScorecardSecHeaderProps {
  dashboardTimeRange: any;
  onFilterValueChange: (value: any) => void;
  orgListOptions?: Array<Record<string, any>>;
  currentOu?: string | string[] | null | undefined;
  handleOuChange?: (param: string) => void;
  ouView?: boolean;
}

const ScoreCardSecondaryHeaderComponent: React.FC<ScorecardSecHeaderProps> = ({
  dashboardTimeRange,
  onFilterValueChange,
  orgListOptions,
  currentOu,
  handleOuChange,
  ouView
}) => {
  const [popOverVisible, setPopOverVisible] = useState<boolean>(false);
  const showNewInterval = useHasEntitlements(Entitlement.SHOW_TRELIS_NEW_INTERVAL, EntitlementCheckType.AND);
  const newTrellis = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);
  const harnessColorClass = newTrellis ? "harness-blue" : "";
  const datePickerCloseHandler = () => {
    setPopOverVisible(false);
  };

  const content = useMemo(() => {
    return (
      <DashboardHeaderDateRangePickerWrapper
        closeHandler={datePickerCloseHandler}
        onFilerValueChange={onFilterValueChange}
        metaData={{ dashboard_time_range_filter: dashboardTimeRange }}
        dashboardTimeRangeOptions={
          showNewInterval ? ScorecardDashboardTimeRangeOptions : ScorecardDashboardTimeRangeOptionsOld
        }
        disableCustom
      />
    );
  }, [dashboardTimeRange, onFilterValueChange, showNewInterval]);

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

  const dashboardTimeGtValueTemp = useMemo(() => {
    return getDashboardTimeRangeDateValue("last_month", "$gt");
  }, []);

  const dashboardTimeLtValueTemp = useMemo(() => {
    return getDashboardTimeRangeDateValue("last_month", "$lt");
  }, []);

  const typeOfDashboardTimeFilterString = typeof dashboardTimeRange === "string";

  return (
    <div className="dashboard-view-secondary-header" style={{ justifyContent: "space-between" }}>
      {/*<div className="dashboard-view-secondary-header-left">*/}
      {/*  <EngineerDropdownContainer onFilterValueChange={onFilterValueChange} />*/}
      {/*</div>*/}
      <div className="dashboard-view-secondary-header-right flex justify-end">
        {newTrellis && !ouView && (
          <AntSelect
            className="selector"
            value={currentOu?.toString()}
            onChange={handleOuChange}
            options={orgListOptions}
            mode={"default"}
          />
        )}
        <div className="flex justify-center align-center">
          <Popover
            visible={popOverVisible}
            className="secondary-header-dropdown-popover-wrapper"
            content={content}
            trigger={"click"}
            placement="bottomRight">
            <div
              className={`dashboard-view-secondary-header-popover-content ${newTrellis ? "new-trellis" : ""}`}
              onClick={() => setPopOverVisible(state => !state)}>
              <AntIcon type="calendar" className="mr-10" theme="outlined" />
              <AntText className={`mr-5 ${harnessColorClass}`} style={{ fontWeight: "500" }}>
                {typeOfDashboardTimeFilterString &&
                  ScorecardDashboardTimeRangeOptions.find(item => item.key === dashboardTimeRange)?.label}
              </AntText>
              <AntText className={`text-style ${harnessColorClass}`}>{typeOfDashboardTimeFilterString && "|"}</AntText>
              <AntText className={`text-style mr-10 ml-5 ${harnessColorClass}`}>{dashboardTimeGtValue}</AntText>-
              <AntText className={`text-style ml-10 mr-5 ${harnessColorClass}`}>{dashboardTimeLtValue}</AntText>
              <AntIcon type="down" className="ml-5" style={{ color: "#096DD9", fontSize: "12px" }} />
              <GetInfoIcon />
            </div>
          </Popover>
        </div>
      </div>
    </div>
  );
};

export default ScoreCardSecondaryHeaderComponent;
