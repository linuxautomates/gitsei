import { DatePicker, Menu, Icon } from "antd";
import React, { useMemo, useState } from "react";
import { AntButton } from "shared-resources/components";
import "./dateRangePickerHeaderWrapper.scss";
import { dateRangeFilterValue } from "./helper";
import moment from "moment";
import { DateFormats } from "utils/dateUtils";

const { RangePicker } = DatePicker;

interface DashboardHeaderDateRangePickerWrapperProps {
  onFilerValueChange: (value: any, key: string) => void;
  metaData: any;
  closeHandler: () => void;
  dashboardTimeRangeOptions: { key: string; label: string }[];
  disableCustom?: boolean;
}
const FORMAT = "YYYY-MM-DD";
export const DashboardHeaderDateRangePickerWrapper: React.FC<DashboardHeaderDateRangePickerWrapperProps> = ({
  onFilerValueChange,
  metaData,
  closeHandler,
  dashboardTimeRangeOptions,
  disableCustom
}) => {
  const [selectedValue, setSelectedValue] = useState<any>(metaData.dashboard_time_range_filter || "");
  const defaultSelectedKeys: any = typeof selectedValue === "string" ? selectedValue : "custom_range";
  const [selectedKey, setSelectedKey] = useState<any>(defaultSelectedKeys);
  const onSaveHandler = () => {
    if (selectedValue !== metaData.dashboard_time_range_filter) {
      onFilerValueChange(selectedValue, "dashboard_time_range_filter");
    }
    closeHandler();
  };
  const saveButton = useMemo(() => {
    return (
      <AntButton type="primary" onClick={onSaveHandler}>
        Save
      </AntButton>
    );
  }, [selectedValue, onFilerValueChange, metaData]);

  const onCancelClick = () => {
    const key: any =
      typeof metaData.dashboard_time_range_filter === "string" ? metaData.dashboard_time_range_filter : "custom_range";
    setSelectedKey(key);
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
    return typeof selectedValue === "string" ? dateRangeFilterValue(selectedValue || "last_30_days") : selectedValue;
  }, [selectedValue, onFilerValueChange]);

  const startDate = useMemo(() => {
    return moment.unix(dateRangeValue["$gt"]).utc();
  }, [selectedValue, dateRangeValue, onFilerValueChange]);

  const endDate = useMemo(() => {
    return moment.unix(dateRangeValue["$lt"]).utc();
  }, [selectedValue, dateRangeValue, onFilerValueChange]);

  const footer = useMemo(() => {
    return (
      <>
        <RangePicker
          allowClear={false}
          value={[startDate, endDate]}
          className="time-range-value-viewer"
          open={false}
          format={DateFormats.DAY}
        />
        <div className="flex justify-end">
          {cancelButton}
          {saveButton}
        </div>
      </>
    );
  }, [selectedValue, onFilerValueChange]);

  const customMenuStyle = useMemo(() => ({ paddingRight: 0 }), []);

  const handleMenuClick = (value: { key: string }) => {
    const rangeType = value.key;
    setSelectedKey(rangeType);
    if (rangeType !== "custom_range") {
      setSelectedValue(rangeType);
    } else {
      onRangePickerRangeHandler([startDate, endDate]);
    }
  };

  const menu = useMemo(() => {
    return (
      <Menu
        defaultSelectedKeys={selectedKey}
        selectedKeys={selectedKey}
        onClick={handleMenuClick}
        className="widget-actions-menu">
        {dashboardTimeRangeOptions.map(
          item => item.key !== "custom_range" && <Menu.Item key={item.key}>{item.label}</Menu.Item>
        )}
        {!disableCustom && (
          <Menu.Item key={"custom_range"} className="flex justify-space-between align-center" style={customMenuStyle}>
            <div>Custom Range</div>
            <Icon type="right" />
          </Menu.Item>
        )}
      </Menu>
    );
  }, [metaData, setSelectedValue, selectedKey, dashboardTimeRangeOptions]);

  const onRangePickerRangeHandler = (dates: any) => {
    if (!disableCustom) {
      const data = {
        $gt: dates[0] ? moment.utc(dates[0].format(FORMAT), FORMAT).startOf("day").unix() : undefined,
        $lt: dates[1] ? moment.utc(dates[1].format(FORMAT), FORMAT).endOf("day").unix() : undefined
      };
      setSelectedValue(data);
    }
  };

  const onFocusChange = () => {
    if (!disableCustom) {
      setSelectedKey("custom_range");
    }
  };

  return (
    <div className="flex">
      <div>{menu}</div>
      <div className="flex direction-column header-date-range-component">
        <div id="header-date-range-container-id" className="header-date-range-container-wrapper" />
        <div className="flex direction-column justify-space-around header-date-range-container-footer">{footer}</div>
      </div>
      <div className={"header-date-range-container"}>
        <RangePicker
          className="ant-calendar-picker"
          onFocus={onFocusChange}
          onChange={onRangePickerRangeHandler}
          value={[startDate, endDate]}
          // @ts-ignore
          getCalendarContainer={() => document.getElementById("header-date-range-container-id")}
          open={true}
        />
      </div>
    </div>
  );
};

export default DashboardHeaderDateRangePickerWrapper;
