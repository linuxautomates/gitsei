import { baseColumnConfig } from "utils/base-table-config";
import { statusColumn, userColumn, priorityColumn, timeColumn, commaSepColumnConfig } from "./common-table-columns";

export const TestrailsTableConfig = [
  baseColumnConfig("Project", "project"),
  baseColumnConfig("Test Case Title", "title"),
  statusColumn("Current Status"),
  userColumn(),
  priorityColumn(),
  baseColumnConfig("Type", "type"),
  baseColumnConfig("Test Plan", "test_plan"),
  baseColumnConfig("Test Run", "test_run"),
  baseColumnConfig("Milestone", "milestone"),
  baseColumnConfig("Estimate forecast", "estimate_forecast"),
  baseColumnConfig("Estimate", "estimate"),
  baseColumnConfig("Case ID", "case_id"),
  commaSepColumnConfig("Defects", "defects"),
];
