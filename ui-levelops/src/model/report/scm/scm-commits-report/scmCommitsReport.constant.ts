import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface ScmCommitsReportType extends BaseSCMReportTypes {
  get_widget_chart_props: (data: any) => any;
  CHART_DATA_TRANSFORMERS: any;
  prev_report_transformer: (widget: any) => any;
}
