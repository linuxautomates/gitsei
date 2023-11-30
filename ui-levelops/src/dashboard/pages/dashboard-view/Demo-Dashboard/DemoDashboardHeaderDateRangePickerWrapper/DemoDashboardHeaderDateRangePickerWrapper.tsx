import React, { useMemo } from "react";
import { DatePicker, Menu, Icon } from "antd";
import { AntButton } from "shared-resources/components";
import { dateRangeFilterValue } from "../../../../components/dashboard-view-page-secondary-header/helper";
import moment from "moment";
import "./DemoDashboardHeaderDateRangePickerWrapper.style.scss";
const { RangePicker } = DatePicker;

interface DemoDashboardHeaderDateRangePickerWrapperProps {
  closeHandler: () => void;
  dashboardTimeRangeOptions: { key: string; label: string }[];
  intervalString?: string;
  isDevProductivityReports?: boolean;
}

export const DemoDashboardHeaderDateRangePickerWrapper: React.FC<DemoDashboardHeaderDateRangePickerWrapperProps> = ({
  closeHandler,
  dashboardTimeRangeOptions,
  intervalString,
  isDevProductivityReports
}) => {
  const saveButton = useMemo(() => {
    return (
      <AntButton type="primary" disabled>
        Save
      </AntButton>
    );
  }, []);

  const onCancelClick = () => {
    closeHandler();
  };

  const cancelButton = useMemo(() => {
    return (
      <AntButton className="mr-5" type="secondary" onClick={onCancelClick}>
        Close
      </AntButton>
    );
  }, [closeHandler]);

  const dateRangeValue = useMemo(() => {
    return dateRangeFilterValue(intervalString);
  }, [intervalString]);

  const startDate = useMemo(() => {
    return moment.unix(dateRangeValue["$gt"]).utc();
  }, [dateRangeValue]);

  const endDate = useMemo(() => {
    return moment.unix(dateRangeValue["$lt"]).utc();
  }, [dateRangeValue]);

  const footer = useMemo(() => {
    return (
      <>
        <RangePicker
          allowClear={false}
          value={[startDate, endDate]}
          className="time-range-value-viewer"
          open={false}
          format="LL"
        />
        <div className="flex justify-end">
          {cancelButton}
          {saveButton}
        </div>
      </>
    );
  }, []);

  const customMenuStyle = useMemo(() => ({ paddingRight: 0 }), []);

  const menu = useMemo(() => {
    return (
      <Menu className="widget-actions-menu">
        {dashboardTimeRangeOptions.map(
          item =>
            item.key !== "custom_range" && (
              <Menu.Item key={item.key} disabled={item.key !== intervalString}>
                {item.label}
              </Menu.Item>
            )
        )}
        {!isDevProductivityReports && (
          <Menu.Item
            key={"custom_range"}
            className="flex justify-space-between align-center"
            disabled={true}
            style={customMenuStyle}>
            <div>Custom Range</div>
            <Icon type="right" />
          </Menu.Item>
        )}
      </Menu>
    );
  }, [dashboardTimeRangeOptions]);
  const currentDate = moment();
  return (
    <div className="flex demoDashboardHeaderDateRange">
      <div>{menu}</div>
      <div className="flex direction-column header-date-range-component">
        <div id="header-date-range-container-id" className="header-date-range-container-wrapper" />
        <div className="flex direction-column justify-space-around header-date-range-container-footer">{footer}</div>
      </div>
      <div className={"header-date-range-container"}>
        <RangePicker
          className="ant-calendar-picker"
          value={[startDate, endDate]}
          disabledDate={d => !d || d.isBefore(currentDate) || d.isAfter(currentDate)}
          // @ts-ignore
          getCalendarContainer={() => document.getElementById("header-date-range-container-id")}
          open={true}
        />
      </div>
    </div>
  );
};

export default DemoDashboardHeaderDateRangePickerWrapper;
