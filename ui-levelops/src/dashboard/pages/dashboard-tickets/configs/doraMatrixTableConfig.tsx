import { AcceptanceTimeUnit } from "classes/RestWorkflowProfile";
import { ROLLBACK_KEY } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { convertToDay } from "custom-hooks/helpers/leadTime.helper";
import { get, round } from "lodash";
import React from "react";
import { AntPopover, AntText, AntTooltip } from "shared-resources/components";
import { baseColumnConfig, columnOptions, timeUnits } from "utils/base-table-config";
import { commaSepColumnConfig, booleanToStringColumn, timeColumn, utcTimeColumn } from "./common-table-columns";
import { prTitleColumn } from "./githubTableConfig";
import { azureWorkitemColumn, jiraKeyColumn } from "./jiraTableConfig";
import { MAX_COMMIT_MSG_LEN } from "./config-constants";

export const DeploymentFrequencyTableConfigIM = [
  { ...jiraKeyColumn, title: "Issue", width: "7%" },
  baseColumnConfig("Ticket Summary", "summary", { width: "22%" }),
  baseColumnConfig("Issue Type", "issue_type", { width: "7%" }),
  baseColumnConfig("Status", "status", { width: "5%" }),
  baseColumnConfig("Priority", "priority", { width: "8%" }),
  baseColumnConfig("Project", "project_im", { width: "5%" }),
  timeColumn("Date Created", "issue_created_at", { width: "10%" }),
  timeColumn("Date Closed", "issue_resolved_at", { width: "10%" }),
  baseColumnConfig("Assignee", "assignee"),
  baseColumnConfig("Epic", "epic"),
  baseColumnConfig("Status Category", "status_category")
];

export const DeploymentFrequencyTableConfigIMAdo = [
  azureWorkitemColumn,
  baseColumnConfig("Ticket Summary", "summary"),
  baseColumnConfig("Issue Type", "issue_type"),
  baseColumnConfig("Status", "status"),
  baseColumnConfig("Priority", "priority"),
  baseColumnConfig("Project", "project_im"),
  timeColumn("Date Created", "workitem_created_at"),
  timeColumn("Date Closed", "workitem_resolved_at"),
  baseColumnConfig("Assignee", "assignee"),
  baseColumnConfig("Epic", "epic"),
  baseColumnConfig("Status Category", "status_category")
];

export const DeploymentFrequencyTableConfigSCM = [
  prTitleColumn("number", "", {}, "", { width: "7%" }),
  baseColumnConfig("PR Title", "title", { width: "25%" }),
  baseColumnConfig("Repo Name", "project", { width: "20%" }),
  baseColumnConfig("Creator", "creator", { width: "10%" }),
  timeColumn("Date Created", "pr_created_at", { width: "10%" }),
  timeColumn("Date Closed", "pr_closed_at", { width: "10%" }),
  baseColumnConfig("Target Branch", "target_branch"),
  timeColumn("Pr Merged At", "pr_merged_at"),
  baseColumnConfig("State", "state"),
  baseColumnConfig("Repo Id", "repo_id")
];

export const DoraSCMCommitsTableConfigSCM = [
  baseColumnConfig("Commit ID", "commit_sha", { width: "25%" }),
  baseColumnConfig("Committer", "committer", { width: "25%" }),
  timeColumn("Committed At", "committed_at", { width: "25%" }),
  timeColumn("Pushed At", "commit_pushed_at", { width: "25%" }),
  baseColumnConfig("Branch", "branch", { width: "25%%" }),
  {
    ...baseColumnConfig("Commit Message", "message", { width: "20%" }),
    render: (value: any) => {
      return typeof value === "string" ? value.slice(0, MAX_COMMIT_MSG_LEN) : value;
    }
  }
];

export const DeploymentFrequencyTableConfigCICD = [
  baseColumnConfig("Pipeline", "job_name"),
  baseColumnConfig("Status", "status"),
  baseColumnConfig("Job Duration (Seconds)", "duration"),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2"),
  utcTimeColumn("End Time (UTC)", "end_time", "f2"),
  baseColumnConfig("Project", "project_name"),
  baseColumnConfig("Instance Name", "cicd_instance_name"),
  baseColumnConfig("Triggered By", "cicd_user_id")
];

export const DoraLeadTimeColumn = {
  ...baseColumnConfig("Lead Time", "total", { width: 180, sorter: true }),
  render: (value: number) => {
    const LeadTime = round(convertToDay(value, AcceptanceTimeUnit.SECONDS), 1);
    const unit = LeadTime > 1 ? "Days" : "Day";
    return (
      <div>
        {LeadTime} {unit}
      </div>
    );
  }
};

export const LeadTimeForChangeTableConfigIMAdo = [
  {
    ...baseColumnConfig("PR ID", "additional_key", { width: 100 }),
    render: (item: any, record: any) => {
      const url = `https://github.com/${record?.project}/pull/${item}`;
      return (
        <AntText>
          <a href={url} target="_blank">
            {record?.additional_key}
          </a>
        </AntText>
      );
    }
  },
  {
    ...baseColumnConfig("PR Details", "title", { width: 200 }),
    render: (item: string) => {
      return <AntTooltip title={item}>{item}</AntTooltip>;
    }
  },
  baseColumnConfig("Repo Name", "project", { width: 180 }),
  timeColumn("PR Creation Date", "created_at", { width: 180, sorter: false }),
  baseColumnConfig("PR Creator", "creator", { width: 180 }),
  DoraLeadTimeColumn
];

export const LeadTimeForChangeTableConfigSCM = [
  {
    ...baseColumnConfig("PR ID", "additional_key", { width: 100 }),
    render: (item: any, record: any) => {
      const url = `https://github.com/${record?.project}/pull/${item}`;
      return (
        <AntText>
          <a href={url} target="_blank">
            {record?.additional_key}
          </a>
        </AntText>
      );
    }
  },
  {
    ...baseColumnConfig("PR Details", "title", { width: 200 }),
    render: (item: string, record: any) => {
      return <AntTooltip title={item}>{item}</AntTooltip>;
    }
  },
  baseColumnConfig("Repo Name", "project", { width: 180 }),
  timeColumn("PR Creation Date", "created_at", { width: 180, sorter: false }),
  baseColumnConfig("PR Creator", "creator", { width: 180 }),
  DoraLeadTimeColumn
];
export const doraLeadTimeByChangeTimeColumn = (
  title: string,
  key: string,
  unit: timeUnits,
  options?: columnOptions
) => ({
  ...baseColumnConfig(title, key, { ...(options || {}), sorter: true }),
  width: 180,
  sortDirections: ["descend", "ascend"],
  render: (value: any, record: any) => {
    if (!value && value !== 0) {
      return (
        <AntPopover
          content={
            <div style={{ paddingBottom: "15px", marginTop: "1rem", maxWidth: "300px" }}>
              <AntText>Stage is not applicable to issue or PR</AntText>
            </div>
          }
          placement="bottom">
          <div style={{ cursor: "pointer" }}>-</div>
        </AntPopover>
      );
    }
    const stage = (record?.data || []).find((item: any) => item?.key === title);
    if (stage) {
      const rating = get(stage, ["velocity_stage_result", "rating"], "missing").toLowerCase();
      const stageTime = round(convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS), 1);
      const unit = stageTime > 1 ? "Days" : "Day";
      return (
        <div className="stage-by-wrapper">
          <div className={`stage-circle-${rating}`}></div>
          <AntText>
            {stageTime} {unit}
          </AntText>
        </div>
      );
    }
  }
});

export const DeploymentFrequencyTableConfigHarnessCICDDefaultColumn = [
  baseColumnConfig("Pipeline", "job_name", { width: "20%" }),
  baseColumnConfig("Status", "status"),
  commaSepColumnConfig("Services id", "service_ids", { width: "15%" }),
  commaSepColumnConfig("Environment id", "env_ids", { width: "15%" }),
  commaSepColumnConfig("Infrastructure id", "infra_ids", { width: "15%" }),
  commaSepColumnConfig("Deployment type", "service_types", { width: "15%" }),
  baseColumnConfig("Duration (Seconds)", "duration"),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2"),
  utcTimeColumn("End Time (UTC)", "end_time", "f2")
];

export const DeploymentFrequencyTableConfigHarnessCICDAllColumn = [
  ...DeploymentFrequencyTableConfigHarnessCICDDefaultColumn,
  baseColumnConfig("Triggered by", "cicd_user_id"),
  baseColumnConfig("Repository URL", "repo_url"),
  baseColumnConfig("Branch", "cicd_branch"),
  commaSepColumnConfig("Tags", "tags"),
  booleanToStringColumn("Rollback", ROLLBACK_KEY)
];

export const DeploymentFrequencyTableConfigReleaseIM = [
  baseColumnConfig("Release version", "name", { width: "25%" }),
  baseColumnConfig("Number of tickets", "issue_count", { width: "25%" }),
  baseColumnConfig("Project", "project", { width: "25%" }),
  timeColumn("Release Date", "released_end_time", { width: "25%" })
];
