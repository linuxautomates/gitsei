import { baseColumnConfig, columnOptions, timeUnits } from "utils/base-table-config";
import { tableCell } from "utils/tableUtils";
import { valueToUnixTime } from "utils/dateUtils";
import moment from "moment";
import "moment-duration-format";
import { convertEpochToHumanizedForm } from "utils/timeUtils";
import { get } from "lodash";
import React from "react";
import { AntPopover, AntText } from "shared-resources/components";

// Handles strings that may or may not be unix times.
export const cautiousUnixTimeColumn = (
  title: string = "Created At",
  key: string = "created_at",
  options?: columnOptions
) => {
  return {
    ...baseColumnConfig(title, key, { ...(options || {}), sorter: true }),
    render: (value: any) => {
      let unixTime = valueToUnixTime(value);

      if (unixTime) {
        return tableCell("updated_on", unixTime);
      } else if (typeof value === "string") {
        return value;
      } else {
        return "";
      }
    }
  };
};

export const timeColumn = (title: string = "Created At", key: string = "created_at", options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { sorter: true, ...(options || {}) }),
  render: (value: any) => tableCell("updated_on", value)
});

export const userColumn = (
  title: string = "Assignee",
  key: string = "assignee",
  cell: "users" | "user" = "user",
  options?: columnOptions
) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any) => tableCell(cell, value)
});

export const statusColumn = (title: string = "Status", key: string = "status", options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any) => tableCell("status", value)
});

export const priorityColumn = (title: string = "Priority", key: string = "priority", options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any) => tableCell("priority", value)
});

export const dateRangeFilterColumn = (
  filterLabel: string,
  filterField: string,
  options?: columnOptions,
  rangeDataType?: "number" | "string"
) => ({
  ...baseColumnConfig("Date Range", "date_range", { ...(options || {}), hidden: true }),
  filterType: "dateRange",
  filterLabel,
  filterField,
  rangeDataType: rangeDataType || "string"
});

export const inputNumberFilterColumn = (
  filterLabel: string,
  filterField: string,
  min: number = 0,
  max: number,
  options?: columnOptions
) => {
  const selectOptions = [];
  for (let i = min; i <= max; i++) {
    selectOptions.push({ label: `${i}`, value: i });
  }

  return {
    ...baseColumnConfig("Input Number", "input_number_filter", { ...(options || {}), hidden: true }),
    filterType: "select",
    filterLabel,
    filterField,
    options: selectOptions,
    unlimitedLength: true,
    span: 4
  };
};

export const timeRangeFilterColumn = (
  title: string,
  filterLabel: string,
  filterField: string,
  options?: columnOptions
) => ({
  ...baseColumnConfig(title, "time_filter", { ...(options || {}), hidden: true }),
  filterType: "timeRange",
  filterLabel,
  filterField
});

//f1 = DD/MM/YYYY
//f2 = YYYY-MM-DD HH:mm:ss
export const utcTimeColumn = (
  title: string,
  key: string,
  format: "f1" | "f2" | "f3" = "f1",
  options?: columnOptions
) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any) => tableCell(`time_utc_${format}`, value)
});

export const coloredTagsColumn = (title: string, key: string, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any, record: any, index: number) =>
    tableCell("tags_withColor", value, undefined, { record_index: index })
});

export const commaSepColumnConfig = (title: string, key: string, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any, record: any, index: number) => tableCell("workspaces", value, undefined, { record_index: index })
});

export const booleanToStringColumn = (title: string, key: string, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any, record: any, index: number) =>
    tableCell("convertBooleanToString", value, undefined, { record_index: index })
});

export const timeDurationColumn = (title: string, key: string, unit: timeUnits, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  render: (value: any) => moment.duration(value, unit).format()
});

export const convertToReadableTimestamp = (title: string, key: string, format: any) => {
  return {
    ...baseColumnConfig(title, key),
    render: (value: any, record: any, index: any) => {
      return convertEpochToHumanizedForm(format, record[key]);
    }
  };
};

export const linesAdded = {
  ...baseColumnConfig("Lines Added", "lines_added"),
  render: (value: any, record: any, index: any) => {
    const additions = get(record, ["additions"], 0);
    const changes = get(record, ["changes"], 0);
    return additions + changes;
  }
};

export const totalLinesAdded = {
  ...baseColumnConfig("Total Lines", "total_lines_added"),
  render: (value: any, record: any, index: any) => {
    const tot_lines_added = get(record, ["tot_lines_added"], 0);
    const tot_lines_changed = get(record, ["tot_lines_changed"], 0);
    const tot_lines_removed = get(record, ["tot_lines_removed"], 0);
    return tot_lines_added + tot_lines_changed + tot_lines_removed;
  }
};

export const convertSecToDay = (title: string, key: string, format: string) => ({
  ...baseColumnConfig(title, key),
  render: (value: any) => `${(value / 86400).toFixed(1)} ${format}`
});

export const addTextToValue = (title: string, key: string, text: string) => ({
  ...baseColumnConfig(title, key),
  render: (value: any) => `${value} ${text}`
});

export const linesChangesColumn = {
  ...baseColumnConfig("Number Of Lines Changed", "num_changes", { align: "center" }),
  render: (value: any, record: any, index: any) => (
    <AntPopover
      trigger={["hover", "click"]}
      content={
        <div>
          <div className="flex items-center">
            <AntText style={{ fontWeight: "500", paddingRight: "4px" }}>Lines Added: </AntText>
            <AntText>{record.num_additions}</AntText>
          </div>
          <div className="flex items-center">
            <AntText style={{ fontWeight: "500", paddingRight: "4px" }}>Lines Removed: </AntText>
            <AntText>{record.num_deletions}</AntText>
          </div>
          <div className="flex items-center">
            <AntText style={{ fontWeight: "500", paddingRight: "4px" }}>Lines Changed: </AntText>
            <AntText>{record.num_changes}</AntText>
          </div>
        </div>
      }>
      <div>{record.num_changes}</div>
    </AntPopover>
  )
};
