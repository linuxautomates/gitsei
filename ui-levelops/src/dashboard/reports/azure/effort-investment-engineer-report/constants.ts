import {
  EIAlignmentReportCSVFiltersTransformer,
  EIDynamicURITransformer,
  EIEngineerCSVDataTransformer,
  EIEngineerReportCSVColumns
} from "dashboard/constants/bussiness-alignment-applications/BACSVHelperTransformer";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  EXCLUDE_SUB_COLUMNS_FOR,
  RequiredFiltersType,
  SUB_COLUMNS_TITLE,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { EffortType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import moment from "moment";

export const DEFAULT_QUERY = {
  workitem_resolved_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]: "azure_effort_investment_tickets",
  [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_azure_ei_ticket_count"
};
export const TIME_RANGE_DEF_METADATA = {
  [RANGE_FILTER_CHOICE]: {
    workitem_resolved_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    },
    committed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};
export const DEFAULT_METADATA_VALUE = {
  ...TIME_RANGE_DEF_METADATA,
  effort_type: EffortType.COMPLETED_EFFORT
};

export const REQUIRED_FILTERS_MAPPING_VALUE = {
  [RequiredFiltersType.SCHEME_SELECTION]: true
};

export const CSV_CONFIG = {
  widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
  widgetDynamicURIGetFunc: EIDynamicURITransformer,
  widgetCSVColumnsGetFunc: EIEngineerReportCSVColumns,
  widgetCSVDataTransform: EIEngineerCSVDataTransformer,
  [SUB_COLUMNS_TITLE]: ["Total", "Percentage"],
  [EXCLUDE_SUB_COLUMNS_FOR]: ["Engineer", "Remaining Allocation"]
};

export const REPORT_NAME = "Effort Investment By Engineer";
export const DEFAULT_ACROSS = "assignee";
export const URI = "azure_effort_investment_tickets";
export const MIN_WIDTH = "36rem";
export const EFFORT_UNIT = "azure_effort_investment_tickets";
