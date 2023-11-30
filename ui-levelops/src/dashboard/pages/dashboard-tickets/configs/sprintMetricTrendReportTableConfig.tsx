import React from "react";
import { sprintStatCalculateBasedOnMetric } from "custom-hooks/helpers/sprintStatReporthelper";
import { baseColumnConfig } from "utils/base-table-config";
import { coloredTagsColumn, statusColumn } from "./common-table-columns";
import { azureUtilityDrilldownFilterColumn, azureWorkitemColumn, jiraKeyColumn } from "./jiraTableConfig";
import { azureSprintMetricsHiddenFilters, jiraSprintMetricsHiddenFilters } from "./sprintSingleStatTableConfig";
import { uniqBy } from "lodash";
import { SprintNodeType } from "../../../../custom-hooks/helpers/constants";
import { Tag } from "antd";

export const sprintMetricTrendColumns = {
  [SprintNodeType.TIME_RANGE]: [
    baseColumnConfig("Sprint Name", "additional_key", { align: "center", width: "4%" }),
    baseColumnConfig("Commit Points", "committed_story_points", { align: "center", width: "4%" }),
    baseColumnConfig("Creep Points", "creep_story_points", {
      align: "center",
      width: "4%"
    }),
    baseColumnConfig("Commit Done Points", "commit_delivered_story_points", {
      align: "center",
      width: "4%"
    }),
    baseColumnConfig("Creep Done Points", "delivered_creep_story_points", {
      align: "center",
      width: "4%"
    }),
    baseColumnConfig("Delivered Points", "delivered_story_points", { align: "center", width: "4%" }),
    {
      ...baseColumnConfig("Commit Missed Points", "commit_to_miss_points", {
        align: "center",
        width: "4%"
      }),
      render: (value: any, record: any) => {
        return sprintStatCalculateBasedOnMetric("commit_to_miss_points", 0, record).num;
      }
    },
    {
      ...baseColumnConfig("Creep Missed Points", "creep_to_miss_points", {
        align: "center",
        width: "4%"
      }),
      render: (value: any, record: any) => {
        return sprintStatCalculateBasedOnMetric("creep_to_miss_points", 0, record).num;
      }
    },
    {
      ...baseColumnConfig("Delivered to Commit %", "done_to_commit", { align: "center", width: "4%" }),
      render: (value: any, record: any, index: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("avg_commit_to_done", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    {
      ...baseColumnConfig("Commit Done %", "commit_to_done", { align: "center", width: "4%" }),
      render: (value: any, record: any, index: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("commit_to_done", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    {
      ...baseColumnConfig("Creep Done to Commit %", "creep_done_to_commit", {
        align: "center",
        width: "4%"
      }),
      render: (value: any, record: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("avg_creep_done_to_commit", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    {
      ...baseColumnConfig("Creep to Commit %", "sprint_creep", { align: "center", width: "4%" }),
      render: (value: any, record: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("avg_creep", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    ...jiraSprintMetricsHiddenFilters
  ],
  [SprintNodeType.SPRINT]: uniqBy(
    [
      { ...jiraKeyColumn, title: "Jira Ticket ID", width: "5%" },
      baseColumnConfig("Ticket Summary", "summary"),
      coloredTagsColumn("Ticket Category", "ticket_category"),
      { ...statusColumn(), filterLabel: "Status" },
      {
        ...baseColumnConfig("Resolved In Sprint", "resolved_in_sprint"),
        render: (item: string) => {
          const color = item === "Yes" ? "blue" : "red";
          return (
            <Tag color={color} style={{ margin: "5px" }}>
              {item}
            </Tag>
          );
        }
      },
      baseColumnConfig("Story Points", "story_points"),
      ...jiraSprintMetricsHiddenFilters
    ],
    "key"
  )
};

export const azureSprintMetricTrendColumns = {
  [SprintNodeType.TIME_RANGE]: [
    baseColumnConfig("Sprint Name", "additional_key", { align: "center", width: "4%" }),
    baseColumnConfig("Commit Points", "committed_story_points", { align: "center", width: "4%" }),
    baseColumnConfig("Creep Points", "creep_story_points", {
      align: "center",
      width: "4%"
    }),
    baseColumnConfig("Commit Done Points", "commit_delivered_story_points", {
      align: "center",
      width: "4%"
    }),
    baseColumnConfig("Creep Done Points", "delivered_creep_story_points", {
      align: "center",
      width: "4%"
    }),
    baseColumnConfig("Delivered Points", "delivered_story_points", { align: "center", width: "4%" }),
    {
      ...baseColumnConfig("Commit Missed Points", "commit_to_miss_points", {
        align: "center",
        width: "4%"
      }),
      render: (value: any, record: any) => {
        return sprintStatCalculateBasedOnMetric("commit_to_miss_points", 0, record).num;
      }
    },
    {
      ...baseColumnConfig("Creep Missed Points", "creep_to_miss_points", {
        align: "center",
        width: "4%"
      }),
      render: (value: any, record: any) => {
        return sprintStatCalculateBasedOnMetric("creep_to_miss_points", 0, record).num;
      }
    },
    {
      ...baseColumnConfig("Delivered to Commit %", "done_to_commit", { align: "center", width: "4%" }),
      render: (value: any, record: any, index: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("avg_commit_to_done", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    {
      ...baseColumnConfig("Commit Done %", "commit_to_done", { align: "center", width: "4%" }),
      render: (value: any, record: any, index: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("commit_to_done", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    {
      ...baseColumnConfig("Creep Done to Commit %", "creep_done_to_commit", {
        align: "center",
        width: "4%"
      }),
      render: (value: any, record: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("avg_creep_done_to_commit", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    {
      ...baseColumnConfig("Creep to Commit %", "sprint_creep", { align: "center", width: "4%" }),
      render: (value: any, record: any) => {
        const { num, count } = sprintStatCalculateBasedOnMetric("avg_creep", 0, record);
        return num ? Math.round(num * 100) : 0;
      }
    },
    ...azureSprintMetricsHiddenFilters,
    ...azureUtilityDrilldownFilterColumn
  ],
  [SprintNodeType.SPRINT]: uniqBy(
    [
      { ...azureWorkitemColumn, width: "5%" },
      baseColumnConfig("Summary", "summary"),
      coloredTagsColumn("Ticket Category", "ticket_category"),
      { ...statusColumn(), filterLabel: "Status" },
      {
        ...baseColumnConfig("Resolved In Sprint", "resolved_in_sprint"),
        render: (item: string) => {
          const color = item === "Yes" ? "blue" : "red";
          return (
            <Tag color={color} style={{ margin: "5px" }}>
              {item}
            </Tag>
          );
        }
      },
      baseColumnConfig("Story Points", "story_points"),
      ...azureSprintMetricsHiddenFilters,
      ...azureUtilityDrilldownFilterColumn
    ],
    "key"
  )
};
