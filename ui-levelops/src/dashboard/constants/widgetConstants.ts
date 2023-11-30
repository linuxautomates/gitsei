
import DevProductivityReports from "dashboard/reports/dev-productivity";
import jiraReports from "../reports/jira";
import jiraSalesforceReports from "../reports/jiraSalesforce";
import jiraZendeskReports from "../reports/jiraZendesk";
import scmReports from "dashboard/reports/scm";
import azureReports from "dashboard/reports/azure";
import zendeskReports from "dashboard/reports/zendesk";
import salesforceReports from "dashboard/reports/salesforce";
import doraReports from "dashboard/reports/dora";
import miscellaneousReports from "dashboard/reports/miscellaneous";
import { JenkinsDashboards } from "./applications/jenkins.application";
import { PagerDutyDashboards } from "./applications/pagerduty.application";
import { SonarQubeDashboard } from "./applications/sonarqube.application";
import { TestrailsDashboard } from "./applications/testrails.application";
import { LevelOpsDashboard } from "./applications/levelops.application";
import { JiraSalesforceZendeskDashboard } from "./applications/jira_salesforce_zendesk.application";
import { PraetorianDashboards } from "./applications/praetorian.application";
import { BullseyeDashboard } from "./applications/bullseye.application";
import { NccGroupDashboards } from "./applications/ncc_group.application";
import { MicrosoftDashboard } from "./applications/microsoft.application";
import { SnykDashboards } from "./applications/snyk.application";
import { jiraBussinessAlignmentDashboard } from "./bussiness-alignment-applications/jira-bussiness-alignment.application";
import { levelopsTable } from "./applications/levelops-table";
import { NotesWidget } from "./header-widget.constant";
import { CoverityReports } from "./applications/coverity.application";
import MultiTimeSeriesReport from "./applications/multiTimeSeries.application";
import { get } from "lodash";

const widgetConstants = {
  ...jiraReports,
  ...azureReports,
  ...jiraSalesforceReports,
  ...jiraZendeskReports,
  ...JenkinsDashboards,
  ...PagerDutyDashboards,
  ...zendeskReports,
  ...salesforceReports,
  ...JiraSalesforceZendeskDashboard,
  ...SonarQubeDashboard,
  ...TestrailsDashboard,
  ...LevelOpsDashboard,
  ...PraetorianDashboards,
  ...BullseyeDashboard,
  ...NccGroupDashboards,
  ...MicrosoftDashboard,
  ...SnykDashboards,
  ...jiraBussinessAlignmentDashboard,
  ...levelopsTable,
  ...NotesWidget,
  ...CoverityReports,
  ...MultiTimeSeriesReport,
  ...DevProductivityReports,
  ...scmReports,
  ...doraReports,
  ...miscellaneousReports
};

export const getWidgetConstant = (
  reportType: string | undefined,
  data: string | string[] | undefined,
  defaultValue?: any
) => {
  if (!reportType || !data) {
    return defaultValue;
  }

  const path = typeof data === "string" ? [data] : data;
  return get(widgetConstants, [reportType, ...path], defaultValue || undefined);
};


export default widgetConstants;
