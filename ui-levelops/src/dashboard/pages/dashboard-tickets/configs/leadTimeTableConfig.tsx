import React from "react";
import { Tooltip } from "antd";
import { JiraIssueLink } from "shared-resources/components/jira-issue-link/jira-issue-link-component";
import { baseColumnConfig, columnOptions, timeUnits } from "utils/base-table-config";
import { cloneDeep, forEach, get } from "lodash";
import { AcceptanceTimeUnit } from "../../../../classes/RestVelocityConfigs";
import { convertToDay, getTimeAndIndicator, getTimeAndUnit } from "../../../../custom-hooks/helpers/leadTime.helper";
import "./leadTimeTableConfig.scss";
import { AntPopover, AntText } from "shared-resources/components";
import { AzureIssueLinkProps } from "../../../../shared-resources/components/azure-issue-link/azure-issue-link";
import { azureUtilityDrilldownFilterColumn } from "./jiraTableConfig";
import { prTitleColumn } from "./githubTableConfig";
import { toTitleCase } from "utils/stringUtils";

export const jiraLeadTimeKeyColumn = {
  ...baseColumnConfig("Jira Ticket ID", "additional_key", { width: 128, sorter: true }),
  fixed: "left",
  render: (value: any, record: any, index: any) => {
    const url = `ticket_details?key=${record.additional_key || record?.key}&integration_id=${record.integration_id}`;
    return (
      <JiraIssueLink
        link={url}
        ticketKey={record?.additional_key || record?.key}
        integrationUrl={record?.integration_url}
      />
    );
  }
};

export const azureLeadTimeKeyColumn = {
  ...baseColumnConfig("Azure Ticket ID", "additional_key", { width: 128 }),
  fixed: "left",
  render: (value: any, record: any, index: any) => {
    const url = `ticket_details?key=${record.additional_key}&integration_id=${record.integration_id}`;
    return (
      <AzureIssueLinkProps
        link={url}
        workItemId={record?.additional_key}
        integrationUrl={record?.integration_url}
        organization={record.organization || ""}
        project={record.project || ""}
      />
    );
  }
};

export const totalLeadTimeColumn = {
  ...baseColumnConfig("Lead Time", "total", { width: 180, sorter: true }),
  align: "left",
  render: (value: any, record: any, index: any) => {
    const data = record.data;
    let lower_limit = 0,
      upper_limit = 0,
      total_lead_time = 0;
    (data || []).forEach((stage: any) => {
      const lower_limit_unit = get(stage, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
      const lower_limit_value = get(stage, ["velocity_stage_result", "lower_limit_value"], 0);
      const upper_limit_unit = get(stage, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
      const upper_limit_value = get(stage, ["velocity_stage_result", "upper_limit_value"], 0);
      total_lead_time += convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS);
      lower_limit += convertToDay(lower_limit_value, lower_limit_unit);
      upper_limit += convertToDay(upper_limit_value, upper_limit_unit);
    });
    const time = getTimeAndIndicator(
      record.data?.total ? convertToDay(record.data.total, AcceptanceTimeUnit.SECONDS) : total_lead_time,
      lower_limit,
      upper_limit
    );

    return (
      <div className={`total-lead-time-column-${time.rating}`}>
        {time.duration} {time.unit}
      </div>
    );
  }
};

export const totalTimeSpentInStageColumn = {
  ...baseColumnConfig("Total Time", "total", { width: 180 }),
  align: "left",
  render: (value: any, record: any) => {
    const lower_limit_unit = get(record, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
    const lower_limit_value = get(record, ["velocity_stage_result", "lower_limit_value"], 0);
    const upper_limit_unit = get(record, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
    const upper_limit_value = get(record, ["velocity_stage_result", "upper_limit_value"], 0);
    const lower_limit = convertToDay(lower_limit_value, lower_limit_unit);
    const upper_limit = convertToDay(upper_limit_value, upper_limit_unit);
    let totalTime = 0;
    // CHECK IF KEY EXISTS ONLY USE IN THIS WIDGET LEAD TIME BY TIME SPENT IN STAGES
    const velocityStageKey = record.hasOwnProperty("velocity_stage_total_time");
    if (velocityStageKey) {
      totalTime = record?.velocity_stage_total_time ?? 0;
    } else {
      forEach(get(record, ["velocity_stages"], []), (stage: { time_spent: number }) => {
        totalTime += stage?.time_spent ?? 0;
      });
    }
    totalTime = convertToDay(totalTime, AcceptanceTimeUnit.SECONDS);

    const time = getTimeAndIndicator(totalTime, lower_limit, upper_limit);
    return (
      <div className={`total-lead-time-column-${time.rating}`}>
        {time.duration} {time.unit}
      </div>
    );
  }
};

export const timeSpentInStageColumnConfig = (title: string, key: string, unit: timeUnits, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  width: 180,
  align: "left",
  title: <Tooltip title={title}>{toTitleCase(title || "")}</Tooltip>,
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
    const stage = (record?.velocity_stages || []).find((item: any) => item?.stage === title);
    if (stage) {
      const lead_time = convertToDay(get(stage, ["time_spent"], 0), AcceptanceTimeUnit.SECONDS);
      const lower_limit = convertToDay(
        get(stage, ["velocity_stage_result", "lower_limit_value"], 0),
        get(stage, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const upper_limit = convertToDay(
        get(stage, ["velocity_stage_result", "upper_limit_value"], 0),
        get(stage, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const time = getTimeAndIndicator(lead_time, lower_limit, upper_limit);
      const rating = time?.rating === "acceptable" ? "needs_attention" : time?.rating;
      return (
        <div className="stage-by-wrapper">
          <div className={`stage-circle-${rating}`}></div>
          <AntText>
            {time.duration} {!!time.duration && time.unit}
          </AntText>
        </div>
      );
    }

    const time = getTimeAndUnit(value);
    return (
      <>
        {time.time} {!!time.time && time.unit}
      </>
    );
  }
});

export const azureTotalLeadTimeColumn = {
  ...baseColumnConfig("Lead Time", "total", { width: 130, sorter: true }),
  align: "right",
  render: (value: any, record: any, index: any) => {
    const data = record.data;
    let lower_limit = 0,
      upper_limit = 0,
      total_lead_time = 0;
    (data || []).forEach((stage: any) => {
      const lower_limit_unit = get(stage, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
      const lower_limit_value = get(stage, ["velocity_stage_result", "lower_limit_value"], 0);
      const upper_limit_unit = get(stage, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
      const upper_limit_value = get(stage, ["velocity_stage_result", "upper_limit_value"], 0);
      total_lead_time += convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS);
      lower_limit += convertToDay(lower_limit_value, lower_limit_unit);
      upper_limit += convertToDay(upper_limit_value, upper_limit_unit);
    });
    const time = getTimeAndIndicator(
      record.data.total ? convertToDay(record.data.total, AcceptanceTimeUnit.SECONDS) : total_lead_time,
      lower_limit,
      upper_limit
    );
    return (
      <div className={`total-lead-time-column-${time.rating}`}>
        {time.duration} {time.unit}
      </div>
    );
  }
};

export const stageTimeColumn = (title: string, key: string, unit: timeUnits, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  width: 180,
  align: "right",
  title: <Tooltip title={title}>{title || ""}</Tooltip>,
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
    const stage = (record?.data || []).find((item: any) => item.key === title);
    if (stage) {
      const lead_time = convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS);
      const lower_limit = convertToDay(
        get(stage, ["velocity_stage_result", "lower_limit_value"], 0),
        get(stage, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const upper_limit = convertToDay(
        get(stage, ["velocity_stage_result", "upper_limit_value"], 0),
        get(stage, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const time = getTimeAndIndicator(lead_time, lower_limit, upper_limit);
      return (
        <span className={`stage-${time.rating}`}>
          {time.duration} {!!time.duration && time.unit}
        </span>
      );
    }

    const time = getTimeAndUnit(value);
    return (
      <>
        {time.time} {!!time.time && time.unit}
      </>
    );
  }
});

export const leadTimeStageByTimeColumn = (title: string, key: string, unit: timeUnits, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  width: 180,
  align: "left",
  title: <Tooltip title={title}>{title || ""}</Tooltip>,
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
    const stage = (record?.data || []).find((item: any) => item.key === title);
    if (stage) {
      const rating = get(stage, ["velocity_stage_result", "rating"], undefined).toLowerCase();
      const lead_time = convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS);
      const lower_limit = convertToDay(
        get(stage, ["velocity_stage_result", "lower_limit_value"], 0),
        get(stage, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const upper_limit = convertToDay(
        get(stage, ["velocity_stage_result", "upper_limit_value"], 0),
        get(stage, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const time = getTimeAndIndicator(lead_time, lower_limit, upper_limit);
      return (
        <div className="stage-by-wrapper">
          <div className={`stage-circle-${rating}`}></div>
          <AntText>
            {time.duration} {!!time.duration && time.unit}
          </AntText>
        </div>
      );
    }

    const time = getTimeAndUnit(value);
    return (
      <>
        {time.time} {!!time.time && time.unit}
      </>
    );
  }
});

export const azureStageTimeColumn = (title: string, key: string, unit: timeUnits, options?: columnOptions) => ({
  ...baseColumnConfig(title, key, { ...(options || {}) }),
  width: key.toLowerCase().includes("lead") ? 190 : undefined,
  align: "right",
  title: <Tooltip title={title}>{title || ""}</Tooltip>,
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

    const stage = (record?.data || []).find((item: any) => item.key === title);
    if (stage) {
      const lead_time = convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS);
      const lower_limit = convertToDay(
        get(stage, ["lower_limit_value"], 0),
        get(stage, ["lower_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const upper_limit = convertToDay(
        get(stage, ["upper_limit_value"], 0),
        get(stage, ["upper_limit_unit"], AcceptanceTimeUnit.SECONDS)
      );
      const time = getTimeAndIndicator(lead_time, lower_limit, upper_limit);
      return (
        <span className={`stage-${time.rating}`}>
          {time.duration} {!!time.duration && time.unit}
        </span>
      );
    }

    const time = getTimeAndUnit(value);
    return (
      <>
        {time.time} {!!time.time && time.unit}
      </>
    );
  }
});

export const JiraLeadTimeTableConfig = [
  jiraLeadTimeKeyColumn,
  baseColumnConfig("Jira Ticket Summary", "title", { width: 180 }),
  totalLeadTimeColumn
];

export const LeadTimeByTimeSpentInStageTableConfig = [
  { ...jiraLeadTimeKeyColumn, title: "Jira Ticket ID" },
  baseColumnConfig("Jira Ticket Summary", "summary", { width: 180 }),
  totalTimeSpentInStageColumn
];

export const AzureLeadTimeTableConfig = [
  azureLeadTimeKeyColumn,
  baseColumnConfig("Project", "project", { width: 180 }),
  baseColumnConfig("Azure Ticket Summary", "title", { width: 180 }),
  azureTotalLeadTimeColumn,
  ...azureUtilityDrilldownFilterColumn
];

const leadTimeTablePRTitleRecordMapping = {
  repo_ids: "repo_id",
  additional_key: "number"
};

export const SCMLeadTimeTableConfig = [
  prTitleColumn("additional_key", "SCM PR ID", leadTimeTablePRTitleRecordMapping, "#", { width: 150 }),
  baseColumnConfig("PR Title", "title", { width: 200 }),
  totalLeadTimeColumn
];
