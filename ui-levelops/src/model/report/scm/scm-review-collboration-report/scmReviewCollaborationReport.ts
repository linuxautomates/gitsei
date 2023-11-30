import { GET_GRAPH_FILTERS } from "dashboard/constants/applications/names";
import { FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS } from "dashboard/constants/filter-key.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMReviewCollaborationReportType extends BaseSCMReportTypes {
  shouldFocusOnDrilldown: boolean;
  includeMissingFieldsInPreview: boolean;
  onUnmountClearData: boolean;
  [WIDGET_MIN_HEIGHT]: string;
  [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: Array<string>;
  [GET_GRAPH_FILTERS]: any;
}
