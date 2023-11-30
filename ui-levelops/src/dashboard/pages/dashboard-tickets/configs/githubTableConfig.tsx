import React from "react";
import moment from "moment";
import { Link, useParams } from "react-router-dom";
import { baseColumnConfig } from "utils/base-table-config";
import {
  userColumn,
  statusColumn,
  timeColumn,
  dateRangeFilterColumn,
  coloredTagsColumn,
  convertToReadableTimestamp,
  totalLinesAdded,
  linesAdded,
  linesChangesColumn
} from "./common-table-columns";
import { zendeskSalesForceHygieneTypes } from "./zendeskTableConfig";
import { scm_issues_resolution_time } from "./jiraTableConfig";
import { AntText, AntPopover, NameAvatar } from "shared-resources/components";
import { get, isArray, upperCase, cloneDeep, set } from "lodash";
import { convertEpochToHumanizedForm, convertToHours } from "utils/timeUtils";
import { toTitleCase } from "utils/stringUtils";
import { WebRoutes } from "../../../../routes/WebRoutes";
import { isSanitizedValue } from "utils/commonUtils";
import { Tooltip } from "antd";
import { ProjectPathProps } from "classes/routeInterface";
import { MAX_COMMIT_MSG_LEN } from "./config-constants";

export const githubCommonColumns = [
  dateRangeFilterColumn("Issue Created At", "issue_created_at"),
  dateRangeFilterColumn("Issue Updated At", "issue_updated_at")
];

export const prTitleColumn = (
  key: "title" | "number" | "message" | "additional_key",
  title = "",
  recordMapping: any = {},
  prefix = "",
  options?: any
) => ({
  ...baseColumnConfig(`${title ? title : toTitleCase("PR " + key)}`, key, options),
  render: (item: any, _record: any) => {
    const record = cloneDeep(_record);
    const truncatedMsg = typeof item === "string" ? item.slice(0, MAX_COMMIT_MSG_LEN) : item;

    //IF BE SEND URL IN METADATA THEN USE THAT URL
    if ((record?.pr_link || "").length > 0) {
      return (
        <a href={record?.pr_link} target={"_blank"}>
          {prefix}
          {truncatedMsg}
        </a>
      );
    }

    Object.keys(recordMapping).forEach(key => {
      if (record.hasOwnProperty(key)) {
        set(record, recordMapping[key], record[key]);
      }
    });
    let baseUrl = record.integration_url;
    const integrationName = record.integration_name;
    if (!isSanitizedValue(baseUrl) && !isSanitizedValue(integrationName)) return truncatedMsg;
    if (!baseUrl && integrationName) {
      baseUrl = `https://${integrationName.split("-")[0]}.com`;
    }
    let _url = `${baseUrl}/${record.repo_id}`;

    switch (record.integration_application || "") {
      case "azure_devops": {
        if (key === "message") {
          _url = record.commit_url;
        }
        break;
      }
      case "github": {
        _url =
          key === "message"
            ? `https://github.com/${record.repo_id}/commits/${record.commit_sha}`
            : `https://github.com/${record.repo_id}/pull/${record.number}`;
        break;
      }
      case "bitbucket_server":
      case "bitbucket": {
        _url = `https://bitbucket.org/${record.repo_id}/pull-requests/${record.number}`;
        break;
      }
      case "gitlab": {
        _url = `https://gitlab.com/${record.repo_id}/-/merge_requests/${record.number}`;
        break;
      }
    }
    return (
      <a href={_url} target={"_blank"}>
        {prefix}
        {truncatedMsg}
      </a>
    );
  }
});

export const prScoreTitleColumn = (
  key: "title" | "number" | "message" | "additional_key",
  title = "",
  recordMapping: any = {},
  prefix = "",
  options?: any
) => ({
  ...baseColumnConfig(`${title ? title : toTitleCase("PR " + key)}`, key, options),
  render: (item: any, _record: any) => {
    const record = cloneDeep(_record);
    const truncatedMsg = typeof item === "string" ? (item || "Commit Message").slice(0, MAX_COMMIT_MSG_LEN) : item;

    Object.keys(recordMapping).forEach(key => {
      if (record.hasOwnProperty(key)) {
        set(record, recordMapping[key], record[key]);
      }
    });
    let baseUrl = record.integration_url;
    const integrationName = record.integration_name;
    if (!isSanitizedValue(baseUrl) && !isSanitizedValue(integrationName)) return truncatedMsg;
    if (!baseUrl && integrationName) {
      baseUrl = `https://${integrationName.split("-")[0]}.com`;
    }
    let _url = `${baseUrl}/${record.repo_id}`;

    switch (record.integration_application || "") {
      case "azure_devops": {
        if (key === "message") {
          _url = record.commit_url;
        }
        break;
      }
      case "github": {
        _url =
          key === "message"
            ? `https://github.com/${record.repo_id}/commits/${record.commit_sha}`
            : `https://github.com/${record.repo_id}/pull/${record.number}`;
        break;
      }
      case "gitlab": {
        _url =
          key === "message"
            ? record.hasOwnProperty("commit_url")
              ? record.commit_url
              : `https://gitlab.com/${record.repo_id}/-/commits/${record.commit_sha}`
            : `https://gitlab.com/${record.repo_id}/-/merge_requests/${record.number}`;
        break;
      }
      case "bitbucket_server":
      case "bitbucket": {
        _url = `https://bitbucket.org/${record.repo_id}/pull-requests/${record.number}`;
        break;
      }
    }
    return (
      <a href={_url} target={"_blank"}>
        {prefix}
        {truncatedMsg}
      </a>
    );
  }
});

const hoursColumn = (title: string = "Number of hours", key: string = "pr_created_at") => ({
  ...baseColumnConfig(title, key),
  render: (createdAt: any, record: any) => {
    let mergedAt = get(record, "pr_merged_at", undefined);
    if (!mergedAt) {
      mergedAt = Math.round(Date.now() / 1000);
    }
    return convertToHours(mergedAt - createdAt);
  }
});

export const GithubPRSTableConfig = [
  prTitleColumn("number", "PR Number"),
  prTitleColumn("title"),
  {
    ...hoursColumn(),
    label: "PR Created At",
    filterType: "dateRange",
    filterField: "pr_created_at",
    filterLabel: "PR Created At"
  },
  baseColumnConfig("Repo ID", "repo_id"),
  userColumn("Creator", "creator"),
  statusColumn("State", "state"),
  // baseColumnConfig("Branch", "branch"),
  baseColumnConfig("Source Branch", "source_branch"),
  baseColumnConfig("Destination Branch", "target_branch"),
  userColumn("Assignees", "assignees", "users"),
  {
    ...timeColumn("PR Merged At", "pr_merged_at"),
    filterType: "dateRange",
    filterField: "pr_merged_at"
  },
  convertToReadableTimestamp("Avg Author Response Time", "avg_author_response_time", "time"),
  convertToReadableTimestamp("Avg Reviewer Response Time", "avg_reviewer_response_time", "time"),
  convertToReadableTimestamp("Comment Time", "comment_time", "time"),
  convertToReadableTimestamp("Approval Time", "approval_time", "time"),
  {
    ...timeColumn("PR Updated", "pr_updated_at"),
    render: (item: any) => moment.unix(item).fromNow()
  },
  timeColumn("PR Closed", "pr_closed_at"),
  baseColumnConfig("Lines Added", "lines_added"),
  baseColumnConfig("Lines Deleted", "lines_deleted"),
  baseColumnConfig("Lines Changed", "lines_changed"),
  baseColumnConfig("Files Changed", "files_changed")
];

export const GithubCommitsTableConfig = [
  baseColumnConfig("Repo ID", "repo_id"),
  userColumn("Committer", "committer"),
  prTitleColumn("message", "Commit Message"),
  baseColumnConfig("Total Refactored Lines", "total_refactored_lines"),
  baseColumnConfig("Percentage Rework ", "pct_refactored_lines"),
  baseColumnConfig("Total Legacy Lines", "total_legacy_lines"),
  baseColumnConfig("Percentage of Legacy Rework", "pct_legacy_refactored_lines"),

  {
    ...baseColumnConfig("File Type", "file_types"),
    render: (item: any, record: any, index: any) => {
      return <AntText>{upperCase(record.file_types) === "NA" ? "N/A" : upperCase(record.file_types)}</AntText>;
    },
    filterType: "multiSelect",
    filterLabel: "File Types",
    filterField: "file_types",
    key: "file_type"
  },
  userColumn("Author", "author"),
  baseColumnConfig("Integration", "integration_id"),
  timeColumn(),
  linesAdded,
  totalLinesAdded,
  timeColumn("Committed At", "committed_at"),
  ...githubCommonColumns
];

export const GithubIssuesTableConfig = [
  {
    ...baseColumnConfig("Issue ID", "number"),
    render: (item: any, record: any) => {
      return (
        <AntText>
          <a onClick={() => window.open(record?.url, "_blank")}>{record?.number}</a>
        </AntText>
      );
    }
  },
  userColumn("Creator", "creator"),
  baseColumnConfig("Title", "title"),
  baseColumnConfig("Comment number", "num_comments"),
  timeColumn("Issue Created At", "issue_created_at"),
  baseColumnConfig("Label", "labels"),
  baseColumnConfig("State", "state", { hidden: true }),
  baseColumnConfig("Repo ID", "repo_id", { hidden: true }),
  baseColumnConfig("Assignee", "assignee", { hidden: true }),
  ...githubCommonColumns
];

export const SCMResolutionTableConfig = [
  {
    ...baseColumnConfig("Issue ID", "issue_id", { sorter: true }),
    render: (item: any, record: any, index: any) => {
      return (
        <AntText>
          <a onClick={() => window.open(record.url, "_blank")}>{record.issue_id}</a>
        </AntText>
      );
    }
  },
  baseColumnConfig("Title", "title"),
  statusColumn("State", "state"),
  scm_issues_resolution_time,
  baseColumnConfig("Repo ID", "repo_id"),
  userColumn("Creator", "creator"),
  baseColumnConfig("Assignee", "assignees"),
  baseColumnConfig("Label", "labels"),
  baseColumnConfig("Assignee", "assignee", { hidden: true }),
  ...githubCommonColumns
];

export const SCMIssueTimeAcrossStagesTableConfig = [
  {
    ...baseColumnConfig("Issue ID", "issue_id", { sorter: true }),
    render: (item: any, record: any, index: any) => {
      return (
        <AntText>
          <a onClick={() => window.open(record.content_url, "_blank")}>{record.issue_id}</a>
        </AntText>
      );
    }
  },
  baseColumnConfig("Title", "issue_title"),
  statusColumn("Current Status", "issue_state"),
  scm_issues_resolution_time,
  baseColumnConfig("Repo", "issues_repo_id"),
  userColumn("Creator", "creator"),
  baseColumnConfig("Assignee", "issues_assignees"),
  baseColumnConfig("Label", "issues_labels"),
  baseColumnConfig("Assignee", "assignee", { hidden: true }),
  dateRangeFilterColumn("Issue Created In", "issue_created_at"),
  dateRangeFilterColumn("Issue Closed In", "issue_closed_at")
];

export const SCMFilesTableConfig = [
  baseColumnConfig("Repo ID", "repo_id"),
  baseColumnConfig("File Name", "filename"),
  baseColumnConfig("Number of Commits", "num_commits", { align: "center" }),
  baseColumnConfig("Total Changes", "total_changes", { align: "center" }),
  baseColumnConfig("Total Additions", "total_additions", { align: "center" }),
  baseColumnConfig("Total Deletions", "total_deletions", { align: "center" }),
  ...githubCommonColumns
];

export const SCMJiraFilesTableConfig = [
  baseColumnConfig("Repo ID", "repo_id"),
  baseColumnConfig("File Name", "filename"),
  baseColumnConfig("Number of Issues", "num_issues", { align: "center" }),
  coloredTagsColumn("Jira Issue Keys", "jira_issue_keys"),
  ...githubCommonColumns
];

export const repoColumn = {
  ...baseColumnConfig("Repo", "name")
};

const ScoreCardLink = ({ user_id, name }: { user_id: string; name?: string }) => {
  const projectParams = useParams<ProjectPathProps>();
  return (
    <Link
      to={WebRoutes.dashboard.scorecard(projectParams, user_id, null, "integration_user_ids")}
      style={{ color: "#2967dd" }}
      target={"_blank"}>
      {name}
    </Link>
  );
};
export const committerColumn = {
  ...baseColumnConfig("Committer", "name"),
  render: (item: string, record: any) => {
    if (record.id) {
      return <ScoreCardLink user_id={record.id} name={record.name} />;
    }

    return <AntText>{record?.name}</AntText>;
  }
};

export const fileTypeColumn = {
  ...baseColumnConfig("File Type", "name"),
  render: (item: any) => {
    return <AntText>{upperCase(item) === "NA" || !item ? "N/A" : upperCase(item)}</AntText>;
  }
};

export const topCustomerTableConfig = [
  baseColumnConfig("Name", "key"),
  baseColumnConfig("Total Tickets", "total_tickets"),
  {
    ...baseColumnConfig("Brand", "brand", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Brands",
    filterField: "brands"
  },
  {
    ...baseColumnConfig("Type", "type", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Types",
    filterField: "types"
  },
  {
    ...baseColumnConfig("Priority", "priority", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Priorities",
    filterField: "priorities"
  },
  {
    ...baseColumnConfig("Status", "status", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Status",
    filterField: "statuses"
  },
  {
    ...baseColumnConfig("Organization", "organization", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Collections",
    filterField: "organizations"
  },
  {
    ...baseColumnConfig("Assignee", "assignee", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Assignees",
    filterField: "assignees"
  },
  {
    ...baseColumnConfig("Requester", "requester", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Requesters",
    filterField: "requesters"
  },
  {
    ...baseColumnConfig("submitter", "submitter", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Submitters",
    filterField: "submitters"
  },
  {
    ...baseColumnConfig("Hygiene", "id1", { hidden: true }),
    filterType: "multiSelect",
    filterField: "hygiene_types",
    options: zendeskSalesForceHygieneTypes,
    filterLabel: "Hygiene"
  }
];

const scmTableCommonColumns = [
  baseColumnConfig("Number of Commits", "num_commits", { align: "center", sorter: true }),
  baseColumnConfig("Number of PRS", "num_prs", { sorter: true, align: "center" }),
  baseColumnConfig("Number of Jira Issues", "num_jira_issues", { sorter: true, align: "center" }),
  linesChangesColumn,
  baseColumnConfig("Number of Line Added", "num_additions", { align: "center" }),
  baseColumnConfig("Number of Line Removed", "num_deletions", { align: "center" }),
  {
    ...baseColumnConfig("Repos", "repo_id", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Repos",
    filterField: "repo_ids"
  },
  {
    ...baseColumnConfig("Committers", "committer", { hidden: true }),
    filterType: "multiSelect",
    filterLabel: "Committers",
    filterField: "committers"
  },
  {
    ...dateRangeFilterColumn("Time", "time_range"),
    rangeDataType: "string"
  },
  ...githubCommonColumns,
  baseColumnConfig("Number of Repos", "num_repos", { align: "center" })
];

export const scmFileTypeConfig = [
  {
    ...fileTypeColumn,
    filterType: "multiSelect",
    filterLabel: "File Types",
    filterField: "file_types",
    key: "file_type"
  },
  ...scmTableCommonColumns
];

const scmCommitterColumns = [
  {
    ...baseColumnConfig("Tech Breadth", "tech_breadth", { align: "center" }),
    ellipses: true,
    render: (value: any, record: any) => (isArray(value) ? value.join(", ") : value)
  },
  {
    ...baseColumnConfig("Repo Breadth", "repo_breadth", { align: "center" }),
    ellipses: true,
    render: (value: any, record: any) => (isArray(value) ? value.join(", ") : value)
  },
  {
    ...baseColumnConfig("File Extensions", "file_types", { align: "center" }),
    ellipses: true,
    render: (value: any, record: any) => (isArray(value) ? value.join(", ") : value)
  }
];

export const scmReposTableConfig = [repoColumn, ...scmTableCommonColumns];
export const scmCommittersTableConfig = [
  baseColumnConfig("Committer", "name"),
  ...scmTableCommonColumns,
  ...scmCommitterColumns
];

export const scmPRSFirstReviewTableConfig = [
  ...GithubPRSTableConfig,
  userColumn("PR Reviewers", "reviewers", "users"),
  baseColumnConfig("Review Type", "review_type"),
  userColumn("Approvers", "approvers", "users")
];

export const scmReviewCollaboration = [
  prTitleColumn("number"),
  baseColumnConfig("PR Title", "title"),
  {
    ...baseColumnConfig("PR Reviewers", "reviewers"),
    render: (value: string[], record: any) => {
      value = (value || []).filter((item: string) => item !== "NONE");
      const sliceAbleLength = Math.min(value.length, 3);
      const firstThree = value.slice(0, sliceAbleLength);
      const leftOutTickets = value.slice(sliceAbleLength) || [];
      return (
        <div className="flex">
          <div className="flex" style={{ height: "100%" }}>
            {(firstThree || []).map(assignee => {
              return (
                <div style={{ paddingRight: "10px" }}>
                  <NameAvatar name={assignee} />
                </div>
              );
            })}
          </div>
          {leftOutTickets.length > 0 && (
            <div className="flex align-center justify-center pl-9">
              <Tooltip placement="topLeft" title={leftOutTickets.join(", ")}>
                <p style={{ color: "var(--link-and-actions)", marginBottom: "0" }}>{`+ ${leftOutTickets.length}`}</p>
              </Tooltip>
            </div>
          )}
        </div>
      );
    }
  },
  {
    ...baseColumnConfig("Collaboration State", "collab_state"),
    render: (value: string) => toTitleCase(value?.replaceAll("-", " "))
  },
  baseColumnConfig("PR Author", "creator"),
  {
    ...baseColumnConfig("PR Review Time", "pr_review_time"),
    render: (value: any, record: any, index: any) => {
      const pr_closed_at = get(record, ["pr_closed_at"], 0);
      const pr_created_at = get(record, ["pr_created_at"], 0);
      const timestamp = pr_closed_at - pr_created_at;
      if (pr_closed_at === 0 || pr_created_at === 0) {
        return "NA";
      }
      return convertEpochToHumanizedForm("time", timestamp);
    }
  }
];
