import { API_CALL_ON_TABLE_SORT_CHANGE } from "constants/pageSettings";
import {
  sprintMetricStatColumnSorterComparater,
  sprintStatCalculateBasedOnMetric
} from "custom-hooks/helpers/sprintStatReporthelper";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { baseColumnConfig } from "utils/base-table-config";
import { timeColumn } from "./common-table-columns";
import { azureCommonTableColumns, jiraCommonTableColumns } from "./jiraTableConfig";
import {
  sprintReportDataKeyTypes,
  spritnMetricKeyTypes
} from "dashboard/graph-filters/components/sprintFilters.constant";

export const jiraSprintMetricsHiddenFilters = [
  { ...baseColumnConfig("Status", "status", { hidden: true }), filterLabel: "Status" },
  baseColumnConfig("Priority", "priority", { hidden: true }),
  baseColumnConfig("Issue Type", "issue_type", { hidden: true }),
  baseColumnConfig("Assignee", "assignee", { hidden: true }),
  baseColumnConfig("Project", "project", { hidden: true }),
  baseColumnConfig("Component", "component", { hidden: true }),
  baseColumnConfig("Label", "label", { hidden: true }),
  baseColumnConfig("Reporter", "reporter", { hidden: true }),
  baseColumnConfig("Fix Version", "fix_version", { hidden: true }),
  baseColumnConfig("Resolution", "resolution", { hidden: true }),
  baseColumnConfig("Status Category", "status_category", { hidden: true }),
  ...jiraCommonTableColumns
];

export const azureSprintMetricsHiddenFilters = [
  { ...baseColumnConfig("Status", "status", { hidden: true }), filterLabel: "Status" },
  baseColumnConfig("Priority", "priority", { hidden: true }),
  baseColumnConfig("Workitem Type", "workitem_type"),
  baseColumnConfig("Assignee", "assignee", { hidden: true }),
  baseColumnConfig("Project", "project", { hidden: true }),
  baseColumnConfig("Component", "component", { hidden: true }),
  baseColumnConfig("Label", "label", { hidden: true }),
  baseColumnConfig("Reporter", "reporter", { hidden: true }),
  baseColumnConfig("Fix Version", "fix_version", { hidden: true }),
  baseColumnConfig("Resolution", "resolution", { hidden: true }),
  baseColumnConfig("Status Category", "status_category", { hidden: true }),
  ...azureCommonTableColumns
];

export const sprintMetricStatUnitColumns = [
  {
    ...baseColumnConfig("Commit Points", "committed_story_points", { align: "center" }),
    render: (value: any, record: any) => {
      return value || 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("committed_story_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: [
      "avg_creep",
      "avg_commit_to_done",
      "avg_commit_to_miss",
      "commit_to_done",
      "avg_creep_done_to_commit",
      "commit_points",
      "commit_to_miss_points"
    ],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Velocity Points", "delivered_story_points", { align: "center" }),
    sorter: sprintMetricStatColumnSorterComparater("delivered_story_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_commit_to_done", "velocity_points", "velocity_points_std"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Commit Done Points", "commit_delivered_story_points", {
      sorter: true,
      align: "center"
    }),
    render: (value: any, record: any) => {
      return value || 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("commit_delivered_story_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_commit_to_miss", "commit_to_done", "commit_to_done_points", "commit_to_miss_points"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Points", "creep_story_points", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("creep_story_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_creep", "avg_creep_to_done", "avg_creep_to_miss", "creep_points", "creep_to_miss_points"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Delivered Creep Points", "delivered_creep_story_points", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("delivered_creep_story_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: [
      "creep_done_points",
      "avg_creep_to_miss",
      "avg_creep_to_done",
      "avg_creep_done_to_commit",
      "creep_to_miss_points"
    ],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Commit Tickets", "committed_keys", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      return value?.length || 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("committed_keys"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["commit_tickets", "commit_missed_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Tickets", "creep_keys", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      return value?.length || 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("creep_keys"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["creep_tickets", "creep_missed_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Delivered Creep Tickets", "delivered_creep_keys", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("delivered_creep_keys"),
    sortDirections: ["descend", "ascend"],
    render: (value: any, record: any) => {
      return value?.length || 0;
    },
    metricKeys: ["creep_done_tickets", "creep_missed_tickets", "done_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Delivered Commit Tickets", "commit_delivered_keys", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("commit_delivered_keys"),
    sortDirections: ["descend", "ascend"],
    render: (value: any, record: any) => {
      return value?.length || 0;
    },
    metricKeys: ["commit_done_tickets", "commit_missed_tickets", "done_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  }
];

export const sprintMetricStatTableConfig = [
  {
    ...baseColumnConfig("Sprint", "additional_key", { align: "center" }),
    sorter: stringSortingComparator(),
    sortDirections: ["descend", "ascend"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false,
    fixed: "left",
    width: 300
  },
  {
    ...baseColumnConfig("Sprint Goal", "sprint_goal", { align: "center", ellipsis: true }),
    width: 300
  },
  {
    ...timeColumn("Completion Date", "key", { align: "center" }),
    sorter: sprintMetricStatColumnSorterComparater("key"),
    sortDirections: ["descend", "ascend"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Delivered to Commit %", "done_to_commit", { sorter: true, align: "center" }),
    render: (value: any, record: any, index: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("avg_commit_to_done", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("avg_commit_to_done"),
    sortDirections: ["descend", "ascend"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false,
    metricKeys: ["avg_commit_to_done", "avg_commit_to_done_std"]
  },
  {
    ...baseColumnConfig("Commit to Delivered %", "commit_to_done", { sorter: true, align: "center" }),
    render: (value: any, record: any, index: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("commit_to_done", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("commit_to_done"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["commit_to_done", "commit_to_done_std"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep to Commit %", "sprint_creep", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("avg_creep", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("avg_creep"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_creep", "avg_creep_std"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Completion %", "creep_completion", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("avg_creep_to_done", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("avg_creep_to_done"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_creep_to_done"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Delivered to Commit %", "creep_done_to_commit", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("avg_creep_done_to_commit"),
    sortDirections: ["descend", "ascend"],
    render: (value: any, record: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("avg_creep_done_to_commit", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    metricKeys: ["avg_creep_done_to_commit"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Miss %", "creep_miss_completion", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("avg_creep_to_miss", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("avg_creep_to_miss"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_creep_to_miss"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Commit Miss %", "commit_miss_completion", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      const { num: calculatedValue } = sprintStatCalculateBasedOnMetric("avg_commit_to_miss", 0, record);
      return calculatedValue ? Math.round(calculatedValue * 100) : 0;
    },
    sorter: sprintMetricStatColumnSorterComparater("avg_commit_to_miss"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["avg_commit_to_miss"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Missed Tickets ", "creep_missed_tickets", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("creep_missed_tickets"),
    sortDirections: ["descend", "ascend"],
    render: (value: any, record: any) => {
      const { num } = sprintStatCalculateBasedOnMetric("creep_missed_tickets", 0, record);
      return num;
    },
    metricKeys: ["creep_missed_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Commit Missed Tickets", "commit_missed_tickets", {
      sorter: true,
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater("commit_missed_tickets"),
    sortDirections: ["descend", "ascend"],
    render: (value: any, record: any) => {
      const { num } = sprintStatCalculateBasedOnMetric("commit_missed_tickets", 0, record);
      return num;
    },
    metricKeys: ["commit_missed_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Delivered Tickets", "done_tickets", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      const { num } = sprintStatCalculateBasedOnMetric("done_tickets", 0, record);
      return num;
    },
    sorter: sprintMetricStatColumnSorterComparater("done_tickets"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["done_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Missed Tickets", "missed_tickets", { sorter: true, align: "center" }),
    render: (value: any, record: any) => {
      const { num } = sprintStatCalculateBasedOnMetric("missed_tickets", 0, record);
      return num;
    },
    sorter: sprintMetricStatColumnSorterComparater("missed_tickets"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["missed_tickets"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Missed Points", "missed_points", { align: "center" }),
    render: (value: any, record: any) => {
      const { num } = sprintStatCalculateBasedOnMetric("missed_points", 0, record);
      return num;
    },
    sorter: sprintMetricStatColumnSorterComparater("missed_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["missed_points"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Commit Missed Points", "commit_to_miss_points", {
      align: "center"
    }),
    render: (value: any, record: any) => {
      return sprintStatCalculateBasedOnMetric("commit_to_miss_points", 0, record).num;
    },
    sorter: sprintMetricStatColumnSorterComparater("commit_to_miss_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["commit_to_miss_points"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Creep Missed Points", "creep_to_miss_points", {
      align: "center"
    }),
    render: (value: any, record: any) => {
      return sprintStatCalculateBasedOnMetric("creep_to_miss_points", 0, record).num;
    },
    sorter: sprintMetricStatColumnSorterComparater("creep_to_miss_points"),
    sortDirections: ["descend", "ascend"],
    metricKeys: ["creep_to_miss_points"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Average Ticket Size Per Sprint", spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT, {
      align: "center"
    }),
    render: (value: any, record: any) => {
      return sprintStatCalculateBasedOnMetric(spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT, 0, record).num.toFixed(
        2
      );
    },
    sorter: sprintMetricStatColumnSorterComparater(spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT),
    sortDirections: ["descend", "ascend"],
    metricKeys: [spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  ...jiraSprintMetricsHiddenFilters
];

export const sprintGoalReportColumns = [
  {
    ...baseColumnConfig("Sprint", "name", { align: "center", width: "30%" }),
    sorter: stringSortingComparator(),
    sortDirections: ["descend", "ascend"],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Sprint Goal", "goal", { align: "center", width: "70%", ellipsis: false })
  }
];

export const sprintMetricChurnRateColumns = [
  {
    ...baseColumnConfig("Churn rate %", sprintReportDataKeyTypes.SPRINT_CHURN_RATE, {
      align: "center"
    }),
    render: (value: string) => {
      return `${(eval(value || "0") * 100).toFixed(2)} (${value || 0})`;
    },
    sortDirections: ["descend", "ascend"],
    sorter: sprintMetricStatColumnSorterComparater(sprintReportDataKeyTypes.SPRINT_CHURN_RATE),
    metricKeys: [sprintReportDataKeyTypes.AVG_CHURN_RATE],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Story Points Planned", sprintReportDataKeyTypes.STORY_POINTS_PLANNED, {
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater(sprintReportDataKeyTypes.STORY_POINTS_PLANNED),
    sortDirections: ["descend", "ascend"],
    metricKeys: [sprintReportDataKeyTypes.AVG_CHURN_RATE],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Story Points Added", sprintReportDataKeyTypes.STORY_POINTS_ADDED, {
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater(sprintReportDataKeyTypes.STORY_POINTS_ADDED),
    sortDirections: ["descend", "ascend"],
    metricKeys: [sprintReportDataKeyTypes.AVG_CHURN_RATE],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Story Points Removed", sprintReportDataKeyTypes.STORY_POINTS_REMOVED, {
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater(sprintReportDataKeyTypes.STORY_POINTS_REMOVED),
    sortDirections: ["descend", "ascend"],
    metricKeys: [sprintReportDataKeyTypes.AVG_CHURN_RATE],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  },
  {
    ...baseColumnConfig("Story Points Changed", sprintReportDataKeyTypes.STORY_POINTS_CHANGED, {
      align: "center"
    }),
    sorter: sprintMetricStatColumnSorterComparater(sprintReportDataKeyTypes.STORY_POINTS_CHANGED),
    sortDirections: ["descend", "ascend"],
    metricKeys: [sprintReportDataKeyTypes.AVG_CHURN_RATE],
    [API_CALL_ON_TABLE_SORT_CHANGE]: false
  }
];
