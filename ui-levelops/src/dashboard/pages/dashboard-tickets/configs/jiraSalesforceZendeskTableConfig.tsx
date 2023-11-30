import React from "react";
import { baseColumnConfig } from "../../../../utils/base-table-config";

import {
  userColumn,
  statusColumn,
  coloredTagsColumn,
  timeColumn,
  dateRangeFilterColumn,
  utcTimeColumn,
  priorityColumn
} from "./common-table-columns";

import { ZendeskTableConfig, JiraTableConfig, SalesForceTableConfig } from "./index";
import { get } from "lodash";

export const zendeskSalesForceHygieneTypes = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];

export const ZendeskJiraTableConfig = {
  zendesk: [
    ...ZendeskTableConfig,
    dateRangeFilterColumn("Issue Created At", "jira_issue_created_at"),
    dateRangeFilterColumn("Issue Updated At", "jira_issue_updated_at")
  ],
  zendesk_list: [
    baseColumnConfig("Jira Issues", "jira_keys"),
    userColumn("Assignee", "assignee_email"),
    userColumn("Submitter", "submitter_email"),
    userColumn("Requester", "requester_email"),
    {
      ...statusColumn("Status", "zendesk_status"),
      filterLabel: "Zendesk Status",
      dataIndex: "status"
    },
    {
      ...baseColumnConfig("Brand", "zendesk_brand"),
      filterLabel: "Zendesk Brand",
      dataIndex: "brand"
    },
    {
      ...baseColumnConfig("Type", "zendesk_type"),
      filterLabel: "Zendesk Type",
      dataIndex: "type"
    },
    timeColumn("Created At", "created_at", { align: "left" }),
    timeColumn("Updated At", "updated_at", { align: "left" }),
    {
      ...baseColumnConfig("Hygiene", "id1", { hidden: true }),
      filterType: "multiSelect",
      filterField: "hygiene_types",
      options: zendeskSalesForceHygieneTypes,
      filterLabel: "Zendesk Hygiene"
    },
    {
      ...baseColumnConfig("", "jira_priority", { hidden: true }),
      filterLabel: "Jira Priority"
    },
    {
      ...baseColumnConfig("", "jira_status", { hidden: true }),
      filterLabel: "Jira Status"
    },
    {
      ...baseColumnConfig("", "jira_assignee", { hidden: true }),
      filterLabel: "Jira Assignee"
    },
    {
      ...baseColumnConfig("", "jira_issue_type", { hidden: true }),
      filterLabel: "Jira Issue Type"
    },
    {
      ...baseColumnConfig("", "jira_project", { hidden: true }),
      filterLabel: "Jira Project"
    },
    {
      ...baseColumnConfig("", "jira_component", { hidden: true }),
      filterLabel: "Jira Component"
    },
    {
      ...baseColumnConfig("", "jira_label", { hidden: true }),
      filterLabel: "Jira Label"
    },
    {
      ...dateRangeFilterColumn("Created At", "created_at"),
      rangeDataType: "string"
    }
  ],
  jira: JiraTableConfig,

  commit: [
    coloredTagsColumn("Jira Issues", "jira_keys"),
    userColumn("Committer", "committer"),
    userColumn("Author", "author"),
    baseColumnConfig("Message", "message"),
    baseColumnConfig("Additions", "additions"),
    baseColumnConfig("Deletions", "deletions"),
    baseColumnConfig("Files CT", "files_ct"),
    baseColumnConfig("Commit URL", "commit_url"),
    baseColumnConfig("Commit SHA", "commit_sha"),
    timeColumn("Committed At", "committed_at"),
    dateRangeFilterColumn("Issue Created At", "jira_issue_created_at"),
    dateRangeFilterColumn("Issue Updated At", "jira_issue_updated_at")
  ]
};

export const SalesforceJiraTableConfig = {
  salesforce: [
    ...SalesForceTableConfig,
    dateRangeFilterColumn("Issue Created At", "jira_issue_created_at"),
    dateRangeFilterColumn("Issue Updated At", "jira_issue_updated_at")
  ],
  salesforce_list: [
    coloredTagsColumn("Jira Issues", "jira_issues"),
    {
      ...userColumn("Contact", "salesforce_contact"),
      dataIndex: "contact",
      filterLabel: "Salesforce Contact"
    },
    {
      ...userColumn("Creator", "salesforce_creator"),
      dataIndex: "creator"
    },
    {
      ...baseColumnConfig("Origin", "salesforce_origin"),
      dataIndex: "origin"
    },
    {
      ...statusColumn("Status", "salesforce_status"),
      dataIndex: "status",
      filterLabel: "Salesforce Status"
    },
    {
      ...baseColumnConfig("Type", "salesforce_type"),
      dataIndex: "type",
      filterLabel: "Salesforce Type"
    },
    {
      ...priorityColumn("Priority", "salesforce_priority"),
      dataIndex: "priority",
      filterLabel: "Salesforce Priority"
    },
    utcTimeColumn("Created At", "sf_created_at"),
    utcTimeColumn("Updated At", "sf_modified_at"),
    {
      ...baseColumnConfig("Hygiene", "id1", { hidden: true }),
      filterType: "multiSelect",
      filterField: "hygiene_types",
      options: zendeskSalesForceHygieneTypes,
      filterLabel: "Zendesk Hygiene"
    },
    {
      ...baseColumnConfig("", "jira_priority", { hidden: true }),
      filterLabel: "Jira Priority"
    },
    {
      ...baseColumnConfig("", "jira_status", { hidden: true }),
      filterLabel: "Jira Status"
    },
    {
      ...baseColumnConfig("", "jira_assignee", { hidden: true }),
      filterLabel: "Jira Assignee"
    },
    {
      ...baseColumnConfig("", "jira_issue_type", { hidden: true }),
      filterLabel: "Jira Issue Type"
    },
    {
      ...baseColumnConfig("", "jira_project", { hidden: true }),
      filterLabel: "Jira Project"
    },
    {
      ...baseColumnConfig("", "jira_component", { hidden: true }),
      filterLabel: "Jira Component"
    },
    {
      ...baseColumnConfig("", "jira_label", { hidden: true }),
      filterLabel: "Jira Label"
    },
    dateRangeFilterColumn("Issue Created At", "jira_issue_created_at"),
    dateRangeFilterColumn("Issue Updated At", "jira_issue_updated_at")
  ],
  jira: JiraTableConfig,
  commit: ZendeskJiraTableConfig.commit
};
