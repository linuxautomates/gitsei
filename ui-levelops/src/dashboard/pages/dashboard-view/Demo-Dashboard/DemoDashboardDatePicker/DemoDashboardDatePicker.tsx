import React, { useState, useMemo } from "react";
import { Popover } from "antd";
import moment from "moment";
import { AntIcon, AntText } from "shared-resources/components";
import { DashboardTimeRangeOptions } from "../../../../components/dashboard-view-page-secondary-header/constants";
import DemoDashboardHeaderDateRangePickerWrapper from "../DemoDashboardHeaderDateRangePickerWrapper/DemoDashboardHeaderDateRangePickerWrapper";
import { dateRangeFilterValue } from "../../../../components/dashboard-view-page-secondary-header/helper";

interface DemoDashboardDatePickerProps {}

export const DemoDashboardDatePicker: React.FC<DemoDashboardDatePickerProps> = props => {
  const [popOverVisible, setPopOverVisible] = useState<boolean>(false);

  const datePickerCloseHandler = () => {
    setPopOverVisible(false);
  };
  const content = useMemo(() => {
    return (
      <DemoDashboardHeaderDateRangePickerWrapper
        closeHandler={datePickerCloseHandler}
        dashboardTimeRangeOptions={DashboardTimeRangeOptions}
        intervalString={"last_3_month"}
      />
    );
  }, []);

  const dateRangeValue = useMemo(() => {
    return dateRangeFilterValue("last_3_month");
  }, []);

  const dashboardTimeGtValue = useMemo(() => {
    return moment.unix(dateRangeValue["$gt"]).utc().format("MMM DD, YYYY");
  }, []);

  const dashboardTimeLtValue = useMemo(() => {
    return moment.unix(dateRangeValue["$lt"]).utc().format("MMM DD, YYYY");
  }, []);

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
export default React.memo(DemoDashboardDatePicker);
