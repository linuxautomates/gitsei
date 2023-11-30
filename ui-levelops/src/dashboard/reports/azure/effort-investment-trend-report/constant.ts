import {
  ALLOW_ZERO_LABELS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE
} from "dashboard/constants/applications/names";
import {
  EIAlignmentReportCSVFiltersTransformer,
  EIDynamicURITransformer,
  EITrendReportCSVColumns,
  EITrendReportCSVDataTransformer
} from "dashboard/constants/bussiness-alignment-applications/BACSVHelperTransformer";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  RequiredFiltersType
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { effortInvestmentTrendChartTooltipTransformer } from "dashboard/constants/chartTooltipTransform/effortInvestmentTrendChartTooltip.transformer";
import { IntervalType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { effortInvestmentXAxisTitleTransformer } from "dashboard/constants/xAxisTitleTransformers/EffortInvestmentTrend.xAxisTransformer";
import moment from "moment";

export const SAMPLE_INTERVAL = [
  { label: "Week", value: "week" },
  { label: "Two Weeks", value: "biweekly" },
  { label: "Month", value: "month" },
  { label: "Quarter", value: "quarter" }
];

export const DEFAULT_QUERY = {
  workitem_resolved_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: IntervalType.BI_WEEK,
  [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_azure_ei_ticket_count"
};

export const TIME_RANGE_FORMAT_CONFIG = {
  week: "DD MMM YYYY",
  biweekly: "DD MMM YYYY",
  month: "MMM YYYY",
  [IntervalType.QUARTER]: "DD MMM YYYY"
};

export const REQUIRED_FILTER_MAPPING_VALUE = {
  [RequiredFiltersType.SCHEME_SELECTION]: true
};

export const CHART_DATA_TRANSFORMER_DEFAULT_VALUE = {
  [CHART_X_AXIS_TITLE_TRANSFORMER]: effortInvestmentXAxisTitleTransformer,
  [CHART_X_AXIS_TRUNCATE_TITLE]: false,
  [CHART_TOOLTIP_RENDER_TRANSFORM]: effortInvestmentTrendChartTooltipTransformer,
  [ALLOW_ZERO_LABELS]: false
};

export const CSV_DOWNLOAD_CONFIG = {
  widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
  widgetDynamicURIGetFunc: EIDynamicURITransformer,
  widgetCSVColumnsGetFunc: EITrendReportCSVColumns,
  widgetCSVDataTransform: EITrendReportCSVDataTransformer
};

export const CHART_PROPS = {
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 },
    className: "ba-bar-chart"
  },
  pieProps: {
    cx: "50%",
    innerRadius: 70
  },
  stacked: true,
  transformFn: (data: any) => {
    return (data as number).toFixed(1) + "%";
  },
  totalCountTransformFn: (data: any) => data + "%"
};

export const REPORT_NAME = "Effort Investment Trend Report";
export const URI = "azure_effort_investment_tickets";
export const FILTERS = {
  across: "workitem_resolved_at"
};

export const METADATA = {
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
