import { CustomDrillDownType } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { completedDateOptions, modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SprintImpactOfUnestimatedTicketsReportType } from "model/report/jira/sprint-impact-of-unestimated-tickets/sprintImpactOfUnestimatedTickets.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintImpactTransformer } from "transformers/reports/sprintMetricsPercentReportTransformer";
import { sprintImpactOfUnestimatedTicketsChartProps } from "./constants";
import {
  DEFAULT_METADATA,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_MAPPING_KEY,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  IS_FRONTEND_REPORT,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "dashboard/constants/filter-key.mapping";
import { issue_resolved_at, jiraApiBasedFilterKeyMapping, sprintDefaultMeta } from "../commonJiraReports.constants";
import {
  JIRA_PARTIAL_FILTER_KEY_MAPPING,
  requiredOneFiltersKeys,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY
} from "../constant";
import { FE_BASED_FILTERS, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { JiraSprintImpactOfUnestimatedTicketReportFiltersConfig } from "./filter.config";
import { handleRequiredForFilters } from "../commonJiraReports.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const sprintImpactOfUnestimatedTicketsReport: {
  sprint_impact_estimated_ticket_report: SprintImpactOfUnestimatedTicketsReportType;
} = {
  sprint_impact_estimated_ticket_report: {
    name: "Sprint Impact of Unestimated Tickets Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_sprint_report",
    method: "list",
    filters: {
      include_issue_keys: true
    },
    defaultAcross: "week",
    default_query: {
      completed_at: modificationMappedValues("last_month", completedDateOptions),
      view_by: "Points"
    },
    xaxis: false,
    chart_props: sprintImpactOfUnestimatedTicketsChartProps,
    supported_filters: jiraSupportedFilters,
    drilldown: {} as CustomDrillDownType,
    transformFunction: data => sprintImpactTransformer(data),
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [REPORT_FILTERS_CONFIG]: JiraSprintImpactOfUnestimatedTicketReportFiltersConfig,
    [IS_FRONTEND_REPORT]: true,
    [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintImpactOfUnestimatedTicketsReport;
