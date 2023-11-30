import moment from "moment";
import React from "react";
import { numberSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { capitalize, get } from "lodash";
import { AntBadge, AntIcon, AntProgress, AntTag, AntText, AntTooltip } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { DateFormats } from "utils/dateUtils";
import { newUXColorMapping } from "../chart-themes";
import TeamAllocationColumn from "./components/TeamAllocationColumn.component";
import classNames from "classnames";
import { SetSortConfigFuncType } from "model/report/azure/program-progress-report/program-progress-report.model";
import { ColumnProps, SortOrder, TableStateFilters } from "antd/lib/table";

const priorityTag = (priority: string) => {
  let tagColor = newUXColorMapping["blue-primary"];
  switch (priority) {
    case "HIGHEST":
      tagColor = newUXColorMapping.red;
      break;
    case "HIGH":
      tagColor = newUXColorMapping.orange;
      break;
    case "MEDIUM":
      tagColor = newUXColorMapping.yellow;
      break;
    case "LOW":
      tagColor = newUXColorMapping.light_grey;
      break;
    case "LOWEST":
      tagColor = newUXColorMapping.grey;
      break;
  }
  return (
    <div className="newTag" style={{ background: tagColor }}>
      {capitalize(priority)}
    </div>
  );
};

const getProgressColor = (progress: number): string => {
  if (progress < 35) {
    return newUXColorMapping.red;
  } else if (progress >= 35 && progress <= 50) {
    return newUXColorMapping.orange;
  }
  return newUXColorMapping["blue-primary"];
};

export const JiraProgressTableConfig = [
  {
    ...baseColumnConfig("NAME", "id"),
    width: 250,
    render: (item: any, record: any) => {
      return (
        <AntTooltip title={record?.summary} placement="topLeft">
          <div className="flex direction-column">
            <AntText style={{ color: newUXColorMapping["blue-primary"], fontWeight: 500 }}>
              {record?.summary || item || ""}
            </AntText>
            <div className="flex">
              <AntText style={{ fontSize: 12 }}>
                {record?.total_story_points !== undefined
                  ? `${record?.total_story_points} Points`
                  : `${record?.total_tickets} Tickets`}
              </AntText>
              {record?.total_unestimated_tickets !== undefined && (
                <AntText style={{ fontSize: 12 }}>{`, ${record?.total_unestimated_tickets} unestimated`}</AntText>
              )}
            </div>
          </div>
        </AntTooltip>
      );
    }
  },
  {
    ...baseColumnConfig("PRIORITY", "priority"),
    render: (item: any) => {
      if (item) {
        return priorityTag(item);
      }
      return null;
    },
    width: 150
  },
  {
    ...baseColumnConfig("STORY POINTS COMPLETED", "completed_percent_story_point", { width: 180 }),
    render: (item: any) => {
      return (
        <div className="flex direction-column">
          <AntProgress percent={item || 0} strokeColor={getProgressColor(item || 0)} />
        </div>
      );
    }
  },
  {
    ...baseColumnConfig("TEAM ALLOCATION", "team_allocations", { width: 200 }),
    render: (item: any, record: any) => <TeamAllocationColumn teams={item} />
  }
];
const colormapping: any = {
  Early: "green",
  Delayed: "red",
  "In Progress / Delayed": "#CF1322",
  "In Progress / Early": "#61BA14",
  "Done / Early": "#61BA14",
  "Done / Delayed": "#CF1322"
};

/** This function return title for Program Progress Report columns */
const getProgramProgressReportTitleFunc = (title: string, setSortConfig: SetSortConfigFuncType) => ({
  sortOrder,
  sortColumn
}: {
  filters: TableStateFilters;
  sortOrder?: SortOrder | undefined;
  sortColumn?: ColumnProps<any> | null | undefined;
}) => {
  if (sortOrder && sortColumn?.dataIndex) {
    setSortConfig(sortColumn?.dataIndex, sortOrder);
  } else {
    setSortConfig("", "", true);
  }
  return title;
};

export const AzureProgramProgressTableConfig = [
  baseColumnConfig("ID", "workitem_id"),
  baseColumnConfig("Summary", "summary"),
  baseColumnConfig("ITERATION", "sprint_full_names"),
  baseColumnConfig("WORKITEMS", "workitems"),
  baseColumnConfig("FEATURE EFFORT", "workitem_effort"),
  baseColumnConfig("COMPLETED POINTS", "workitems_ratio"),
  baseColumnConfig("DUE DATE", "due_date"),
  baseColumnConfig("STATUS", "fe_status"),
  baseColumnConfig("WORKITEM COMPLETED", "workitem_completed"),
  baseColumnConfig("WORKITEM PENDING", "workitem_pending")
];

export const AzureProgramProgressTableConfigFunc = (setSortConfig: SetSortConfigFuncType): Array<ColumnProps<any>> => [
  {
    ...baseColumnConfig("ID", "workitem_id", { width: 120 }),
    fixed: "left",
    render: (item: any, record: any) => {
      return (
        <AntText
          style={{ color: newUXColorMapping["completed"], fontWeight: 400, lineHeight: "22px", fontSize: "16px" }}>
          {item || ""}
        </AntText>
      );
    }
  },
  {
    ...baseColumnConfig("Summary", "summary", { width: 200 }),
    render: (item: any, record: any) => {
      return (
        <AntTooltip title={record?.summary} placement="topLeft">
          <AntText
            style={{ color: newUXColorMapping["completed"], fontWeight: 400, lineHeight: "22px", fontSize: "16px" }}>
            {record?.summary || item || ""}
          </AntText>
        </AntTooltip>
      );
    }
  },
  {
    ...baseColumnConfig("ITERATION", "sprint_full_names", { align: "center", width: 200 }),
    title: (
      <>
        <AntTooltip title={"ITERATION"}>{"ITERATION"}</AntTooltip>
        <AntTooltip title={"Shows the current iteration that a workitem is associated with."}>
          <AntIcon type="info-circle" className="ml-5" />
        </AntTooltip>
      </>
    ),
    render: (item: string[]) => {
      return item && item.length ? item[item.length - 1] : "-";
    }
  },
  {
    ...baseColumnConfig("WORKITEMS", "workitems", { align: "center", sorter: true, width: 180 }),
    title: getProgramProgressReportTitleFunc("WORKITEMS", setSortConfig),
    render: (item: any) => <AntText> {item}</AntText>,
    sortDirections: ["descend", "ascend"]
  },
  {
    ...baseColumnConfig("FEATURE EFFORT", "workitem_effort", { align: "center", sorter: true, width: 180 }),
    title: getProgramProgressReportTitleFunc("FEATURE EFFORT", setSortConfig),
    sortDirections: ["descend", "ascend"]
  },
  {
    ...baseColumnConfig("COMPLETED POINTS", "workitems_ratio", { align: "center", sorter: true, width: 180 }),
    title: getProgramProgressReportTitleFunc("COMPLETED POINTS", setSortConfig),
    render: (item: any, record: any) => {
      const completedStoryPoint = get(record, ["completed_points"], 0);
      const totalStoryPoint = get(record, ["total_points"], 0);
      return <AntText> {`${completedStoryPoint}/${totalStoryPoint}`}</AntText>;
    },
    sortDirections: ["descend", "ascend"]
  },
  {
    ...baseColumnConfig("DUE DATE", "due_date", { align: "center", sorter: true, width: 180 }),
    title: getProgramProgressReportTitleFunc("DUE DATE", setSortConfig),
    render: (item: any, record: any) => {
      if (item) {
        const date = moment.unix(item).utc().format(DateFormats.DAY_MONTH);
        return <span>{date}</span>;
      }
      return <span>-</span>;
    },
    sortDirections: ["descend", "ascend"]
  },
  {
    ...baseColumnConfig("STATUS", "fe_status", { align: "center", width: 180 }),
    title: (
      <>
        <AntTooltip title={"STATUS"}>{"STATUS"}</AntTooltip>
        <AntTooltip
          title={
            "Shows the status of the feature. Also shows whether the feature is delayed or early depending on the Due Date."
          }>
          <AntIcon type="info-circle" className="ml-5" />
        </AntTooltip>
      </>
    ),
    render: (item: any) => {
      const status = item?.split(" / ")?.[1];
      const progress = item?.split(" / ")?.[0];
      if (!status && !progress) {
        return <AntText>{item}</AntText>;
      }
      return (
        <div className="dev-productivity-score-table">
          <div
            className={classNames("flex align-center  justify-center ml-20 mr-20", {
              "justify-space-between": !!status
            })}>
            <div
              className={classNames(
                "status-label-container",
                {
                  "justify-center": !status
                },
                { "justify-end": !!status }
              )}>
              {progress ?? "-"}
            </div>
            {status && (
              <div className="status-tag-container">
                <AntTag color={`${colormapping[status]}`} style={{ color: `${colormapping[item]}` }}>
                  {status}
                </AntTag>
              </div>
            )}
          </div>
        </div>
      );
    }
  },
  {
    ...baseColumnConfig("WORKITEM COMPLETED", "workitem_completed", { align: "center", sorter: true, width: 200 }),
    title: getProgramProgressReportTitleFunc("WORKITEM COMPLETED", setSortConfig),
    sortDirections: ["descend", "ascend"]
  },
  {
    ...baseColumnConfig("WORKITEM PENDING", "workitem_pending", { align: "center", sorter: true, width: 180 }),
    title: getProgramProgressReportTitleFunc("WORKITEM PENDING", setSortConfig),
    sortDirections: ["descend", "ascend"]
  }
];
