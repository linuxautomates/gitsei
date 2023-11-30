export interface DemoSupportedColumn {
  key: string;
  name: string;
  show_value_in: string;
  color?: string;
  width?: string;
  className?: string;
}

export interface DemoDrillDownProps {
  data: any;
  xAxis: string;
  currentAllocation: boolean;
  drilldown_count?: number;
}

export enum DrilldownShowValueIn {
  TAGS = "tags",
  STRING = "string",
  LINK = "link",
  JOB_STATUS = "job_status",
  DURATION = "duration", // will remove once I update the data
  AVATAR = "avatar",
  AVATARS_STRING = "avatars_string",
  COMPONENT = "component",
  ASSIGNEE_NAME = "assignee_name",
  TAGS_BGCOLOR = "tags_bgcolor",
  TIME = "time",
  LEAD_TOTAL = "lead_time_total",
  STAGE_TIME = "stage_time"
}
