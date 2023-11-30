import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface IssueAssigneeTimeReportType extends BaseJiraReportTypes {
  hidden_filters: basicMappingType<any>;
  xaxis: false;
}
