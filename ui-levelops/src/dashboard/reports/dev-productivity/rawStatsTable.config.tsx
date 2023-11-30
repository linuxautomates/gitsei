import { Icon, Tooltip } from "antd";
import { ColumnProps } from "antd/lib/table";
import { timeInterval } from "dashboard/constants/devProductivity.constant";
import React from "react";
import { AntText } from "shared-resources/components";
import { getTimeForTrellisProfile } from "utils/dateUtils";
import { toTitleCase, valueToTitle } from "utils/stringUtils";
import { lastUpdatedAt } from "./tableConfigHelper";

export const defaultColumns = [
  "Number of PRs per month",
  "Number of Commits per month",
  "Lines of Code per month",
  "Average Coding days per week",
  "Average PR Cycle Time",
  "Number of PRs approved per month"
];

export const rawStatsDaysColumns = [
  "Average Coding days per week",
  "Average PR Cycle Time",
  "Average time spent working on Issues",
  "Average response time for PR approvals",
  "Average response time for PR comments"
];

export const COLOR_CODE_MAPPING: any = {
  NEEDS_IMPROVEMENT: "improvement",
  GOOD: "good",
  ACCEPTABLE: "acceptable",
  NOT_AVAILABLE: "not-available",
  NO_COLOR: "no-color"
};

const daysTitle = (title: string) => (
  <>
    <AntText style={{ color: "inherit" }}>{title}</AntText>
    <AntText style={{ color: "inherit", opacity: "0.8" }}> (Days)</AntText>
  </>
);

const renderValue = (value: any, record: any, dataIndex: string | undefined, parentClassName: string | undefined) => {
  const renderVal = value || value === 0 ? value : "--";
  const colorValue = record?.no_color ? "no-color" : COLOR_CODE_MAPPING?.[record?.[`${dataIndex}${"_color"}`]];
  const ratingClass = colorValue ? colorValue : record?.no_score_legend_check ? "not-available" : "no-color";
  return <div className={`stats-cell ${parentClassName}  ${ratingClass} `}>{renderVal}</div>;
};

const renderOtherCells = (
  value: any,
  record: any,
  dataIndex: string | undefined,
  parentClassName: string | undefined
) => {
  const renderVal = value || value === 0 ? value : "--";
  const colorValue = record?.no_color ? "no-color" : COLOR_CODE_MAPPING?.[record?.[`${dataIndex}${"_color"}`]];
  const ratingClass = colorValue ? colorValue : record?.no_score_legend_check ? "not-available" : "no-color";

  return <div className={`other-cell ${parentClassName} ${ratingClass} `}>{renderVal}</div>;
};

export interface ColumnPropsWithType<T> extends ColumnProps<T> {
  isNumeric?: boolean;
  titleForCSV?: string;
}

export const rawStatsColumns: Record<string, ColumnPropsWithType<any>> = {
  "Percentage of Rework": {
    title: "Rework (%)",
    dataIndex: "Percentage of Rework"
  },
  "Percentage of Legacy Rework": {
    title: "Legacy Work (%)",
    dataIndex: "Percentage of Legacy Rework"
  },
  "Number of PRs per month": {
    title: "PRs",
    dataIndex: "Number of PRs per month"
  },
  "Number of Commits per month": {
    title: "Commits",
    dataIndex: "Number of Commits per month"
  },
  "Lines of Code per month": {
    title: "Lines Of Code",
    dataIndex: "Lines of Code per month"
  },
  "Number of bugs worked on per month": {
    title: "Bug Fixed",
    dataIndex: "Number of bugs worked on per month"
  },
  "Number of stories worked on per month": {
    title: "Stories",
    dataIndex: "Number of stories worked on per month"
  },
  "Number of Story Points worked on per month": {
    title: "Story Points",
    dataIndex: "Number of Story Points worked on per month"
  },
  "Technical Breadth - Number of unique file extension": {
    title: "Unique File Extension",
    dataIndex: "Technical Breadth - Number of unique file extension"
  },
  "Repo Breadth - Number of unique repo": {
    title: "Unique Repos",
    dataIndex: "Repo Breadth - Number of unique repo"
  },
  "Average Coding days per week": {
    title: "Coding Days",
    dataIndex: "Average Coding days per week"
  },
  "Average PR Cycle Time": {
    title: daysTitle("Avg. PR Cycle Time"),
    dataIndex: "Average PR Cycle Time",
    titleForCSV: "Avg. PR Cycle Time (Days)"
  },
  "Average time spent working on Issues": {
    title: daysTitle("Avg. Issue Resolution Time"),
    dataIndex: "Average time spent working on Issues",
    titleForCSV: "Avg. Issue Resolution Time (Days)"
  },
  "Number of PRs commented on per month": {
    title: "PRs Commented",
    dataIndex: "Number of PRs commented on per month"
  },
  "Average response time for PR approvals": {
    title: daysTitle("Avg. Response Time For PR Approval"),
    dataIndex: "Average response time for PR approvals",
    titleForCSV: "Avg. Response Time For PR Approval (Days)"
  },
  "Average response time for PR comments": {
    title: daysTitle("Avg. Response Time For PR Comment"),
    dataIndex: "Average response time for PR comments",
    titleForCSV: "Avg. Response Time For PR Comment (Days)"
  },
  "Number of PRs approved per month": {
    title: "No. Of PRs Approved",
    dataIndex: "Number of PRs approved per month"
  }
};

export const getRawStatsColumnsList = (
  selectedColumns: string[],
  isCellClickable: boolean = false,
  interval?: string,
  className?: string
) => {
  const rawStatColumns: Array<ColumnPropsWithType<any>> = [];
  selectedColumns.forEach((columnName: string) => {
    let newColumnName: any = undefined;
    let column = rawStatsColumns[columnName];
    const isReplace = columnName?.includes("per month");
    const replaceKey = interval?.includes(timeInterval.LAST_WEEK)
      ? "in one week"
      : interval?.includes(timeInterval.LAST_TWO_WEEKS)
      ? "in two weeks"
      : "";
    if ((interval?.includes(timeInterval.LAST_WEEK) || interval?.includes(timeInterval.LAST_TWO_WEEKS)) && isReplace) {
      newColumnName = columnName.replace("per month", replaceKey);
      column.dataIndex = newColumnName;
    }
    if (column) {
      rawStatColumns.push({
        ...column,
        key: column.dataIndex?.replaceAll(" ", "_") || "",
        isNumeric: true,
        render: (value, record: any) => ({
          children: isCellClickable
            ? renderValue(value, record, column.dataIndex, className)
            : renderOtherCells(value, record, column.dataIndex, className)
        }),
        onCell: (record: any) => ({
          record,
          columnName: newColumnName ? newColumnName : columnName,
          isCellClickable
        })
      });
    }
  });
  return rawStatColumns;
};

const getRawStatsColumnGroup = (
  selectedColumns: string[],
  isCellClickable?: boolean,
  interval?: string,
  className?: string
): ColumnProps<any> => {
  const children = getRawStatsColumnsList(selectedColumns, isCellClickable, interval, className);
  return {
    title: "Developer Stats",
    key: "developer_stats",
    className: "attribute-column",
    children
  };
};

const nameColumn = (
  nameIndex: string,
  showUpdatedAt: boolean = false,
  parentClassName?: string | undefined
): ColumnPropsWithType<any> => ({
  title: !showUpdatedAt ? "Contributor" : "Name",
  key: "name",
  dataIndex: nameIndex,
  fixed: "left",
  width: 250,
  render: (item, record) => (
    <>
      <div className={`other-cell ${parentClassName}`}>
        {showUpdatedAt ? lastUpdatedAt(item, record?.result_time) : item}
      </div>
    </>
  ),
  isNumeric: false,
  onCell: (record: any, rowIndex: number) => ({
    record,
    value: record[nameIndex],
    type: "name"
  })
});

const contributorRoleColumn = (title: string, name: string): ColumnPropsWithType<any> => ({
  title: title,
  key: name,
  dataIndex: name,
  fixed: "left",
  width: 250,
  render: item => (
    <>
      <div className="other-cell">{item}</div>
    </>
  ),
  isNumeric: false
});

const getUserAttributes = (userAttributes: string[] = [], parentClassName: string | undefined): ColumnProps<any> => ({
  title: "User Attributes",
  key: "user_attributes",
  className: "attribute-column",
  // @ts-ignore
  children: userAttributes.map((attribute: string) => ({
    title: toTitleCase(valueToTitle(attribute)),
    dataIndex: attribute,
    key: "user_attributes",
    width: 300,
    render: (value, record: any) => renderOtherCells(value, record, attribute, parentClassName),
    onCell: (record: any, rowIndex: number) => ({
      record,
      value: record[attribute],
      type: "user_stats"
    })
  }))
});

export const getDeveloperRawStatColumns = (
  userAttributes?: string[],
  selectedColumns?: string[],
  interval?: string,
  className?: string
): Array<ColumnProps<any>> => {
  const columns: Array<ColumnProps<any>> = [];
  columns.push(nameColumn("full_name", undefined, "developer-raw-stat"));
  const userAttributeColumns = getUserAttributes(userAttributes, className);
  if (userAttributeColumns.children?.length) {
    columns.push(userAttributeColumns);
  }
  columns.push(getRawStatsColumnGroup(selectedColumns || defaultColumns, true, interval, className));
  return columns;
};

export const getOrgRawStatColumns = (
  selectedColumns?: string[],
  interval?: string,
  parentClass?: string | undefined
): Array<ColumnProps<any>> => {
  const columns: Array<ColumnProps<any>> = [
    {
      ...nameColumn("name", true, parentClass)
    },
    ...getRawStatsColumnsList(selectedColumns || defaultColumns, undefined, interval, parentClass)
  ];
  return columns;
};

export const rawStatColumns = Object.values(rawStatsColumns);

export const defaultRawStatColumns = getRawStatsColumnsList(defaultColumns);
