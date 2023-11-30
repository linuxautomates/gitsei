import React from "react";
import { basicMappingType, optionType } from "dashboard/dashboard-types/common-types";
import { ouScoreWidgetsConfig, profileExtraInfoType } from "dashboard/dashboard-types/engineerScoreCard.types";
import { ouScoreTableConfig } from "../dev-productivity-report/component/ouTableConfig";
import { ouScoreTableCSVTransformer } from "dashboard/helpers/csv-transformers/ouScoreTableCSVtransformer";
import {
  GithubCommitsTableConfig,
  GithubPRSTableConfig,
  JiraTableConfig,
  scmCommittersTableConfig
} from "../dashboard-tickets/configs";
import { azureTableConfig } from "../dashboard-tickets/configs/jiraTableConfig";
import { devProdDrilldownTypes } from "./types";
import { convertToReadableTimestamp, timeColumn } from "../dashboard-tickets/configs/common-table-columns";
import { baseColumnConfig } from "utils/base-table-config";
import { prScoreTitleColumn } from "../dashboard-tickets/configs/githubTableConfig";
import { get } from "lodash";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";
import { withBaseStaticUrl } from "helper/helper";

// id for dev productivity api call
export const DEV_PROD_ID = "DEV_PROD_ID";
export const DEV_PROD_USER_SNAPSHOT_ID = "DEV_PROD_USER_SNAPSHOT_ID";
export const DEV_PROD_DRILLDOWN_ID = "DEV_PROD_DRILLDOWN_ID";
export const DEV_PROD_RELATIVE_SCORE_ID = "DEV_PROD_RELATIVE_SCORE_ID";
export const DEV_PROD_PR_ACTIVITY_ID = "DEV_PROD_PR_ACTIVITY_ID";

// used for extra info on engineer overview card
export const profileExtraInfo: Array<profileExtraInfoType & optionType> = [
  {
    label: "Manager",
    value: "manager",
    path: ["full_name"]
  }
];

// tab keys for scorecard dashboard
export enum scorecardTabKeys {
  SCORECARD = "scorecard",
  EFFORT_INVESTMENT = "effort_investment",
  FEEDBACK = "feedback"
}

export enum scoreWidgetType {
  SCORE_OVERVIEW = "score_overview",
  FEATURE = "feature",
  ORG_SCORE_OVERVIEW = "org_score_overview",
  SCORE_TABLE = "score_table",
  ORGANIZATION_UNIT_PRODUCTIVITY_SCORE = "organization_unit_productivity_score"
}

export enum engineerCategoryType {
  QUALITY = "Quality",
  IMPACT = "Impact",
  VOLUME = "Volume",
  SPEED = "Speed",
  PROFICIENCY = "Proficiency",
  LEARDERSHIP = "Leadership & Collaboration",
  COLLABORATION = "Collaboration"
}

export const engineerCategoryIconsMapping = () => ({
  [engineerCategoryType.IMPACT]: withBaseStaticUrl("static/ScoreCardAssets/rocket.svg"),
  [engineerCategoryType.LEARDERSHIP]: withBaseStaticUrl("static/ScoreCardAssets/group.svg"),
  [engineerCategoryType.COLLABORATION]: withBaseStaticUrl("static/ScoreCardAssets/group.svg"),
  [engineerCategoryType.PROFICIENCY]: withBaseStaticUrl("static/ScoreCardAssets/education.svg"),
  [engineerCategoryType.VOLUME]: withBaseStaticUrl("static/ScoreCardAssets/circles.svg"),
  [engineerCategoryType.SPEED]: withBaseStaticUrl("static/ScoreCardAssets/speed.svg"),
  [engineerCategoryType.QUALITY]: withBaseStaticUrl("static/ScoreCardAssets/diamond.svg")
});

export const widthMappingCategoryIcon = {
  [engineerCategoryType.IMPACT]: "3rem",
  [engineerCategoryType.LEARDERSHIP]: "2.625rem",
  [engineerCategoryType.PROFICIENCY]: "2.625rem",
  [engineerCategoryType.VOLUME]: "2rem",
  [engineerCategoryType.SPEED]: "2.625rem",
  [engineerCategoryType.QUALITY]: "2.625rem"
};

export const devProductivityWidgets: ouScoreWidgetsConfig[] = [
  {
    name: "Trellis Score Score Over Time",
    widget_type: scoreWidgetType.ORGANIZATION_UNIT_PRODUCTIVITY_SCORE,
    width: "100%",
    height: "23.875rem",
    uri: "organization_unit_productivity_score",
    method: "list"
  },
  {
    widget_type: scoreWidgetType.SCORE_TABLE,
    width: "100%",
    height: "46rem",
    name: "Trellis Score",
    columns: ouScoreTableConfig({ count: 0 }).map(column => ({
      title: get(TRELLIS_SECTION_MAPPING, [column?.dataIndex ?? ""], column?.dataIndex)
    })),
    uri: "dev_productivity_report_orgs_users",
    method: "list",
    csvTransformer: ouScoreTableCSVTransformer
  }
];
// keeping this for future reference in case we need renaming some feature in future
export const engineerCategoryMapping: basicMappingType<string> = {};

export const drillDownColumnsMapping: basicMappingType<any> = {
  SCM_COMMITS: [
    ...GithubCommitsTableConfig?.filter((column: any) => !["message"].includes(column?.key)),
    prScoreTitleColumn("message", "Commit Message")
  ],
  SCM_PRS: [
    ...GithubPRSTableConfig?.filter(
      (column: any) =>
        !["approval_time", "comment_time", "avg_author_response_time", "avg_reviewer_response_time"].includes(
          column?.key
        )
    ),
    convertToReadableTimestamp("Approval Time", "approval_time", "days"),
    convertToReadableTimestamp("Comment Time", "comment_time", "days"),
    convertToReadableTimestamp("PR Cycle Time", "avg_author_response_time", "days"),
    convertToReadableTimestamp("PR Cycle Time", "avg_reviewer_response_time", "days")
  ],
  JIRA_ISSUES: [
    ...JiraTableConfig,
    timeColumn("Issue Resolved Date", "issue_resolved_at"),
    baseColumnConfig("Severity", "severity"),
    convertToReadableTimestamp("Response Time", "response_time", "days"),
    convertToReadableTimestamp("Closed Date", "solve_time", "days"),
    baseColumnConfig("Story Point Portion", "story_points_portion"),
    baseColumnConfig("Ticket Portion", "ticket_portion"),
    convertToReadableTimestamp("Time Spent", "assignee_time", "days")
  ],
  WORKITEM_ISSUES: [
    ...azureTableConfig,
    baseColumnConfig("Story Point Portion", "story_points_portion"),
    baseColumnConfig("Ticket Portion", "ticket_portion"),
    convertToReadableTimestamp("Time Spent", "assignee_time", "days")
  ],
  SCM_CONTRIBUTION: scmCommittersTableConfig,
  SCM_CONTRIBUTIONS: scmCommittersTableConfig
};

export const DRILLDOWN_MAPPING: devProdDrilldownTypes = {
  "Percentage of Rework": {
    DRILLDOWN_TITLE: "List of Commits and Percentage Rework for Commits",
    SCM_COMMITS: ["repo_id", "message", "total_refactored_lines", "total_lines_added"],
    JIRA_ISSUES: ["repo_id", "message", "total_refactored_lines", "total_lines_added"]
  },
  "Percentage of Legacy Rework": {
    DRILLDOWN_TITLE: "List of Commits and Percentage Legacy Rework for Commits",
    SCM_COMMITS: ["repo_id", "message", "total_legacy_lines", "total_lines_added"],
    JIRA_ISSUES: ["repo_id", "message", "total_legacy_lines", "total_lines_added"]
  },
  "High Impact bugs worked on per month": {
    DRILLDOWN_TITLE: "List of High Impact Bugs worked-on in the TIMESTAMP",
    WORKITEM_ISSUES: ["workitem_id", "summary", "workitem_resolved_at", "priority", "severity", "ticket_portion"],
    JIRA_ISSUES: ["key", "summary", "issue_resolved_at", "priority", "severity", "status", "ticket_portion"]
  },
  "High Impact stories worked on per month": {
    DRILLDOWN_TITLE: "List of High Impact Stories worked-on in the TIMESTAMP",
    WORKITEM_ISSUES: ["workitem_id", "summary", "workitem_resolved_at", "priority", "severity", "ticket_portion"],
    JIRA_ISSUES: ["key", "summary", "issue_resolved_at", "priority", "severity", "status", "ticket_portion"]
  },
  "Number of Commits per month": {
    DRILLDOWN_TITLE: "List of Commits created in the TIMESTAMP",
    SCM_COMMITS: ["repo_id", "message", "committed_at"],
    JIRA_ISSUES: ["repo_id", "message", "committed_at"]
  },
  "Number of bugs worked on per month": {
    DRILLDOWN_TITLE: "List of Bugs worked-on in the TIMESTAMP",
    WORKITEM_ISSUES: ["workitem_id", "summary", "workitem_resolved_at", "ticket_portion"],
    JIRA_ISSUES: ["key", "summary", "issue_resolved_at", "ticket_portion"]
  },
  "Lines of Code per month": {
    DRILLDOWN_TITLE: "List of Commits created and number of lines for each Commit in the TIMESTAMP",
    SCM_COMMITS: ["repo_id", "message", "lines_added", "committed_at"],
    JIRA_ISSUES: ["repo_id", "message", "lines_added", "committed_at"]
  },
  "Number of stories worked on per month": {
    DRILLDOWN_TITLE: "List of Issues worked in the TIMESTAMP",
    WORKITEM_ISSUES: ["workitem_id", "summary", "workitem_resolved_at", "ticket_portion"],
    JIRA_ISSUES: ["key", "summary", "issue_resolved_at", "ticket_portion"]
  },
  "Number of Story Points worked on per month": {
    DRILLDOWN_TITLE: "List of Issues worked-on in the TIMESTAMP with their Story Points",
    WORKITEM_ISSUES: ["workitem_id", "summary", "workitem_resolved_at", "story_points_portion"],
    JIRA_ISSUES: ["key", "summary", "issue_resolved_at", "story_points_portion"]
  },
  "Number of PRs per month": {
    DRILLDOWN_TITLE: "List of PRs closed in the TIMESTAMP",
    SCM_PRS: ["number", "title", "repo_id", "pr_closed_at", "target_branch"]
  },
  "Number of Issues resolved per month": {
    DRILLDOWN_TITLE: "List of Issues resolved in the TIMESTAMP",
    WORKITEM_ISSUES: ["workitem_id", "summary", "workitem_resolved_at"],
    JIRA_ISSUES: ["key", "summary", "issue_resolved_at"]
  },
  "Average Coding days per week": {
    DRILLDOWN_TITLE: "List of Commits created in the TIMESTAMP",
    SCM_COMMITS: ["repo_id", "message", "committed_at"]
  },
  "Average PR Cycle Time": {
    DRILLDOWN_TITLE: "List of PRs with PRs Cycle Time",
    SCM_PRS: ["number", "title", "avg_author_response_time", "pr_created_at", "pr_merged_at"]
  },
  "Average time spent working on Issues": {
    DRILLDOWN_TITLE: "List of Issues worked-on and response time for each issue",
    JIRA_ISSUES: ["key", "summary", "story_points_portion", "status", "assignee_time"],
    WORKITEM_ISSUES: ["workitem_id", "summary", "story_points_portion", "status", "assignee_time"]
  },
  "Average response time for PR": {
    DRILLDOWN_TITLE: "List of PRs and response time for each PR",
    SCM_PRS: ["number", "title", "repo_id", "pr_closed_at"]
  },
  "Technical Breadth - Number of unique file extension": {
    DRILLDOWN_TITLE: "List of Commits, and file types for each Commit",
    SCM_CONTRIBUTIONS: ["name", "num_commits", "num_prs", "file_types", "tech_breadth"]
  },
  "Repo Breadth - Number of unique repo": {
    DRILLDOWN_TITLE: "List of Repos and Commits",
    SCM_CONTRIBUTIONS: ["name", "num_repos", "repo_breadth", "num_commits"]
  },
  "Number of PRs reviewed per month": {
    DRILLDOWN_TITLE: "List of PRs the user has approved, declined or added comments",
    SCM_PRS: ["number", "title", "repo_id", "pr_closed_at", "pr_created_at"]
  },
  "Number of PRs approved per month": {
    DRILLDOWN_TITLE: "List of PRs the user has approved",
    SCM_PRS: ["number", "title", "repo_id", "pr_closed_at"]
  },
  "Number of PRs commented on per month": {
    DRILLDOWN_TITLE: "List of PRs the user has commented on",
    SCM_PRS: ["number", "title", "repo_id", "pr_closed_at"]
  },
  "Average response time for PR approvals": {
    DRILLDOWN_TITLE: "List of PRs and the response time to approve the PRs",
    SCM_PRS: ["number", "title", "repo_id", "approval_time", "pr_closed_at"]
  },
  "Average response time for PR comments": {
    DRILLDOWN_TITLE: "List of PRs and the response time to comment on the PRs",
    SCM_PRS: ["number", "title", "repo_id", "comment_time", "pr_closed_at"]
  }
};

export const REQUIRE_TRELLIS_TEXT = "Please Select the trellis profile";
