import { azureSprintMetricSingleStatDrilldown } from "dashboard/constants/drilldown.constants";
import { completedDateOptions } from "dashboard/graph-filters/components/Constants";
import { modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { spritnMetricKeyTypes } from "dashboard/graph-filters/components/sprintFilters.constant";

export const METRICS_OPTIONS = [
  /**
   * Ratios
   */
  { label: "Delivered to Commit Ratio", value: "avg_commit_to_done" },
  { label: "Creep to Commit Ratio", value: "avg_creep" },
  { label: "Creep Done to Commit Ratio", value: "avg_creep_done_to_commit" },
  { label: "Creep Done Ratio", value: "avg_creep_to_done" },
  { label: "Creep Missed Ratio", value: "avg_creep_to_miss" },
  { label: "Commit Missed Ratio", value: "avg_commit_to_miss" },
  { label: "Commit Done Ratio", value: "commit_to_done" },
  /**
   * Ratios STD
   */
  { label: "Delivered to Commit Ratio STDEV", value: "avg_commit_to_done_std" },
  { label: "Commit Done Ratio STDEV", value: "commit_to_done_std" },
  { label: "Creep to Commit Ratio STDEV", value: "avg_creep_std" },
  /**
   * Points
   */
  { label: "Velocity Points", value: "velocity_points" },
  { label: "Creep Points", value: "creep_points" },
  { label: "Creep Done Points", value: "creep_done_points" },
  { label: "Velocity Points STDEV", value: "velocity_points_std" },
  { label: "Commit Missed Points", value: "commit_to_miss_points" },
  { label: "Creep Missed Points", value: "creep_to_miss_points" },
  { label: "Commit Done Points", value: "commit_to_done_points" },
  { label: "Missed Points", value: "missed_points" },
  { label: "Commit Points", value: "commit_points" },
  /**
   * Tickets
   */
  { label: "Creep Tickets", value: "creep_tickets" },
  { label: "Creep Done Tickets", value: "creep_done_tickets" },
  { label: "Creep Missed Tickets", value: "creep_missed_tickets" },
  { label: "Commit Tickets", value: "commit_tickets" },
  { label: "Commit Done Tickets", value: "commit_done_tickets" },
  { label: "Commit Missed Tickets", value: "commit_missed_tickets" },
  { label: "Done Tickets", value: "done_tickets" },
  { label: "Missed Tickets", value: "missed_tickets" },
  /**
   * Averages
   */
  { label: "Average Ticket Size Per Sprint", value: spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT }
];

export const DEFAULT_QUERY = {
  agg_type: "average",
  completed_at: modificationMappedValues("last_month", completedDateOptions),
  metric: "avg_commit_to_done"
};

export const REPORT_NAME = "Sprint Metrics Single Stat";
export const URI = "issue_management_sprint_report";
export const FILTERS = {
  include_workitem_ids: true
};
export const CHART_PROPS = {
  unit: "%"
};
export const COLUMNS_WITH_INFO = {
  additional_key: "Sprint name",
  key: "Sprint completion date",
  creep_story_points:
    "Number of story points added to the sprint after start. Doesn’t include points removed from the sprint.",
  delivered_creep_story_points: "Number of Creep points completed in the sprint. ",
  creep_completion: "Creep Points / Committed Points",
  committed_story_points: "Number of points committed at the beginning of the sprint.",
  sprint_creep: "Creep Points / Committed Points",
  delivered_story_points:
    "Number of points delivered at the end of a sprint (or on sprint completion date). Excludes points completed outside the sprint.",
  done_to_commit: "Done Points / Committed Points"
};
export const SUPPORTED_WIDGET_TYPES = ["stats"];
export const DRILL_DOWN = {
  allowDrilldown: true,
  ...azureSprintMetricSingleStatDrilldown
};
