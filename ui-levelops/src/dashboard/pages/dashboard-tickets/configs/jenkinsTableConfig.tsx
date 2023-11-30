import { JENKINS_BUILD_URL_WARNING } from "constants/formWarnings";
import { get, forEach } from "lodash";
import React from "react";
import { AntTooltip } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { utcTimeColumn, statusColumn } from "./common-table-columns";
import { ROLLBACK_KEY } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";

export const JenkinsJobCountTableConfig = [
  baseColumnConfig("Job Name", "job_name"),
  baseColumnConfig("User ID", "cicd_user_id"),
  utcTimeColumn("Config Change Time (UTC)", "change_time", "f2")
];

export const JenkinsGithubTableConfig = [
  baseColumnConfig("Job Name", "job_name"),
  statusColumn(),
  {
    ...baseColumnConfig("Job duration (Minutes)", "duration"),
    render: (value: any, record: any, index: any) => {
      const duration = record?.duration || 0;
      if (duration <= 0) return 0;
      return (duration / 60).toFixed(2);
    }
  },
  baseColumnConfig("User ID", "cicd_user_id"),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2")
];

export const JenkinsGithubJobRunTableConfig = [
  baseColumnConfig("Project", "project_name", { sorter: true }),
  baseColumnConfig("Pipeline", "job_name", { sorter: true }),
  statusColumn("Status", "status", { sorter: true }),
  baseColumnConfig("Triggered By", "cicd_user_id", { sorter: true }),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2", { sorter: true }),
  utcTimeColumn("End Time (UTC)", "end_time", "f2", { sorter: true }),
  baseColumnConfig("Job Run", "job_run_number", { sorter: true }),
  {
    ...baseColumnConfig("Job duration (Minutes)", "duration", { sorter: true }),
    render: (value: any, record: any, index: any) => {
      const duration = record?.duration || 0;
      if (duration <= 0) return 0;
      return (duration / 60).toFixed(2);
    }
  },
  baseColumnConfig("Project", "project_name", { hidden: true }),
  baseColumnConfig("Instance Name", "instance_name", { hidden: true }),
  baseColumnConfig("Qualified name", "job_normalized_full_name", { hidden: true })
];

export const JenkinsPipelineTableConfig = [
  baseColumnConfig("Job Name", "job_name"),
  baseColumnConfig("Duration (seconds)", "duration"),
  statusColumn(),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2")
];

export const JenkinsPipelineJobTableConfig = [
  baseColumnConfig("Pipeline", "job_name"),
  {
    ...baseColumnConfig("Duration (Minutes)", "duration"),
    render: (value: any) => {
      const duration = value || 0;
      if (duration <= 0) return 0;
      return (duration / 60).toFixed(2);
    }
  },
  statusColumn(),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2")
];

export const jenkinsBuildColumn = {
  ...baseColumnConfig("Build Id", "buildId"),
  render: (item: any, record: any, index: number) => {
    const buildUrl = get(record, ["cicd_build_url"], undefined);
    const columnValue = `${get(record, ["job_name"], "")}/${get(record, ["job_run_number"], "")}`;
    if (buildUrl) {
      return (
        <a href={buildUrl} target="_blank">
          {columnValue}
        </a>
      );
    } else {
      return (
        <AntTooltip title={JENKINS_BUILD_URL_WARNING} placement="topLeft">
          <div>
            <p style={{ color: "var(--grey3)", margin: "auto 0" }}>{columnValue}</p>
          </div>
        </AntTooltip>
      );
    }
  }
};

export const CommitTitleColumn = () => {
  return {
    ...baseColumnConfig("Commit Title", "commit_title"),
    render: (item: any, record: any) => {
      const commits = [...(record?.commits || [])] || [];
      const initial_commit = commits.pop();
      const title = get(initial_commit, ["message"], "");
      const commit_url = get(initial_commit, ["commit_url"], "");

      return (
        <a href={commit_url} target={"_blank"}>
          {title}
        </a>
      );
    }
  };
};

export const InitialCommitToDeploymentTimeColumn = () => {
  return {
    ...baseColumnConfig("Initial Commit To Deployment Time", "initial_commit_to_deploy_time", {
      width: "12%",
      sorter: true
    }),
    render: (item: number) => {
      const initial_commit_to_deployment_time = (item / 3600).toFixed(2); // converted to hrs
      return `${initial_commit_to_deployment_time} hrs`;
    }
  };
};

export const ScmCicdTableConfig = [
  CommitTitleColumn(),
  baseColumnConfig("Job Name", "job_name", { sorter: true }),
  utcTimeColumn("Job End Time", "end_time", "f2", { sorter: true }),
  InitialCommitToDeploymentTimeColumn(),
  baseColumnConfig("Lines Modified", "lines_modified", { sorter: true }),
  baseColumnConfig("Files Modified", "files_modified", { sorter: true }),
  baseColumnConfig("User ID", "cicd_user_id", { sorter: true }),
  baseColumnConfig("Job Run", "job_run_number", { sorter: true }),
  baseColumnConfig("Duration (seconds)", "duration", { sorter: true }),
  statusColumn("Status", "status", { sorter: true }),
  utcTimeColumn("Start Time (UTC)", "start_time", "f2", { sorter: true })
];

export const HarnessTableConfigOpenReport = [
  baseColumnConfig("Rollback", ROLLBACK_KEY, { hidden: true }),
  baseColumnConfig("Services id", "service", { hidden: true }),
  baseColumnConfig("Environment id", "environment", { hidden: true }),
  baseColumnConfig("Infrastructure id", "infrastructure", { hidden: true }),
  baseColumnConfig("Deployment type", "deployment_type", { hidden: true }),
  baseColumnConfig("Repository URL", "repository", { hidden: true }),
  baseColumnConfig("Branch", "branch", { hidden: true }),
  baseColumnConfig("Tags", "tag", { hidden: true }),
];