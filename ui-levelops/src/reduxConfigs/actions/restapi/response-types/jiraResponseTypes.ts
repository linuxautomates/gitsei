export interface AssigneeTimeReportCompactResponse {
  assignee: string;
  integration_id: string;
  key: string;
  summary: string;
  total_time: number;
}

export interface AssigneeTimeReportResponse extends AssigneeTimeReportCompactResponse {
  integration_name: string;
  integration_url: string;
  integration_application: string;
}
