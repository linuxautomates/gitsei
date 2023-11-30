import React, { useMemo } from "react";
import { Popover } from "antd";
import { AntIcon, AntText } from "shared-resources/components";
import DashboardHeaderDateRangePickerWrapper from "../dashboard-view-page-secondary-header/DateRangePickerHeaderWrapper";
import { getDashboardTimeRangeDateValue, timeStampToValue } from "../dashboard-view-page-secondary-header/helper";
import "./DashboardHeader.scss";
import { Metadata } from "dashboard/dashboard-types/Dashboard.types";
import { DateFormats } from "utils/dateUtils";
import LocalStoreService from "services/localStoreService";
import { getDashboardTimeRangeIfPreCalc } from "./helper";
import { TENANTS_USING_PRE_CALC } from "./constants";

interface DashboardDatePickerProps {
  popOverVisible: boolean;
  metaData: Metadata;
  onFilerValueChange: (value: string, key: string) => void;
  setPopOverVisible: (val: boolean) => void;
}

export const DashboardDatePicker: React.FC<DashboardDatePickerProps> = props => {
  const { popOverVisible, metaData, onFilerValueChange, setPopOverVisible } = props;

  const datePickerCloseHandler = () => {
    setPopOverVisible(false);
  };

  const ls = new LocalStoreService();
  const content = useMemo(() => {
    return (
      <DashboardHeaderDateRangePickerWrapper
        closeHandler={datePickerCloseHandler}
        onFilerValueChange={onFilerValueChange}
        metaData={metaData}
        dashboardTimeRangeOptions={getDashboardTimeRangeIfPreCalc()}
        disableCustom={TENANTS_USING_PRE_CALC.includes(ls.getUserCompany() || "")}
      />
    );
  }, [metaData, onFilerValueChange]);

  const dashboardTimeGtValue = useMemo(() => {
    return typeof metaData?.dashboard_time_range_filter === "string"
      ? getDashboardTimeRangeDateValue(metaData?.dashboard_time_range_filter || "last_30_days", "$gt", DateFormats.DAY)
      : timeStampToValue(metaData?.dashboard_time_range_filter?.[`$gt`] || {}, DateFormats.DAY);
  }, [metaData]);

  const dashboardTimeLtValue = useMemo(() => {
    return typeof metaData?.dashboard_time_range_filter === "string"
      ? getDashboardTimeRangeDateValue(metaData?.dashboard_time_range_filter || "last_30_days", "$lt", DateFormats.DAY)
      : timeStampToValue(metaData?.dashboard_time_range_filter?.[`$lt`] || {}, DateFormats.DAY);
  }, [metaData]);

  return (
    <Popover
      visible={popOverVisible}
      className="secondary-header-dropdown-popover-wrapper"
      content={content}
      trigger={"click"}
      placement="bottomRight">
      <div className="dashboard-date-picker" onClick={() => setPopOverVisible(!popOverVisible)}>
        <div>
          <AntText className=" text-style mr-10 ml-5">{dashboardTimeGtValue}</AntText>-
          <AntText className=" text-style ml-10 mr-5">{dashboardTimeLtValue}</AntText>
        </div>
        <span>
          <AntIcon type="down" className="mr-5" style={{ fontSize: "1rem" }} />
        </span>
      </div>
    </Popover>
  );
};

export default React.memo(DashboardDatePicker);
