import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";
import { IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { BaseReportTypes } from "model/report/baseReport.constant";
import React from "react";

type ReportData = {
  tableId: string; ou_id: string
}

export interface TableReportType extends Partial<BaseReportTypes> {
  [WIDGET_MIN_HEIGHT]: string;
  supported_widget_types?: Array<string>;
  [IS_FRONTEND_REPORT]?: boolean;
  widget_filters_preview_component?: React.FC<any>;
  [WIDGET_VALIDATION_FUNCTION]?: Function;
  [STORE_ACTION]?: (payload: any) => any;
}
