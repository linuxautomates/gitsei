export enum LEVELOPS_REPORTS {
  ASSESSMENT_COUNT_REPORT = "levelops_assessment_count_report",
  ASSESSMENT_RESPONSE_TIME_REPORT = "levelops_assessment_response_time_report",
  ASSESSMENT_RESPONSE_TIME_TABLE_REPORT = "levelops_assessment_response_time__table_report",
  ASSESSMENT_COUNT_REPORT_TRENDS = "levelops_assessment_count_report_trends",
  WORKITEM_COUNT_REPORT = "levelops_workitem_count_report",
  WORKITEM_COUNT_REPORT_TRENDS = "levelops_workitem_count_report_trends",
  TABLE_REPORT = "levelops_table_report",
  TABLE_STAT_REPORT = "levelops_table_single_stat"
}

// Only these reports need to use the new levelopsReportsTrendReportTransformer
export const LevelopsReportsForNewTrendTransformer: LEVELOPS_REPORTS[] = [
  LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_REPORT,
  LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_TABLE_REPORT,
  LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT,
  LEVELOPS_REPORTS.WORKITEM_COUNT_REPORT,
  LEVELOPS_REPORTS.WORKITEM_COUNT_REPORT_TRENDS,
  LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT_TRENDS
];

export const LevelopsTrendReports: LEVELOPS_REPORTS[] = [
  LEVELOPS_REPORTS.WORKITEM_COUNT_REPORT_TRENDS,
  LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT_TRENDS
];

// Reports with response line min, max, median
export const AssessmentResponseTimeReports: LEVELOPS_REPORTS[] = [
  LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_REPORT,
  LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_TABLE_REPORT
];

export const LevelopsReportsWithMaxRecords: LEVELOPS_REPORTS[] = [
  LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT,
  LEVELOPS_REPORTS.WORKITEM_COUNT_REPORT
];
