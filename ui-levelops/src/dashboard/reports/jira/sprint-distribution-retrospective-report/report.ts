import { FE_BASED_FILTERS, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { defaultFilter, aggMetric, percentile } from "dashboard/constants/FE-BASED/jira.FEbased";
import {
  IGNORE_FILTER_KEYS_CONFIG,
  jiraSprintIgnoreConfig,
  ALLOW_KEY_FOR_STACKS,
  SHOW_FILTERS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB,
  SHOW_WEIGHTS_TAB,
  SHOW_AGGREGATIONS_TAB,
  BAR_CHART_REF_LINE_STROKE,
  DEFAULT_METADATA,
  PARTIAL_FILTER_MAPPING_KEY,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "dashboard/constants/filter-key.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SprintDistributionRetrospectiveReportTypes } from "model/report/jira/sprint_distribution_retrospective_report/sprintDistributionRetrospectiveReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricsDistributionTransformer } from "transformers/reports";
import {
  sprintDistributionRetrospectiveReportInfo,
  sprintDistributionRetrospectiveReportChartTypes,
  sprint_end_date,
  sprintDefaultMeta
} from "./constants";
import {
  JIRA_PARTIAL_FILTER_KEY_MAPPING,
  requiredOneFiltersKeys,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY
} from "../constant";
import { JiraSprintDistributionRetrospectiveReportFiltersConfig } from "./filter.config";
import { handleRequiredForFilters } from "../commonJiraReports.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const sprintDistributionRetrospectiveReport: {
  sprint_distribution_retrospective_report: SprintDistributionRetrospectiveReportTypes;
} = {
  sprint_distribution_retrospective_report: {
    name: "Sprint Distribution Retrospective Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "sprint_distribution_report",
    xaxis: false,
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    show_max: false,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    columnWithInformation: true,
    columnsWithInfo: sprintDistributionRetrospectiveReportInfo,
    chart_props: sprintDistributionRetrospectiveReportChartTypes,
    doneStatusFilter: {
      valueKey: "distribution_stages" // making object as in future we can add default, labal etc as required
    },
    default_query: defaultFilter,
    [ALLOW_KEY_FOR_STACKS]: true,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: (data: any) => sprintMetricsDistributionTransformer(data),
    onChartClickPayload: ({ data }: any) => {
      return { across: data?.activeLabel, keys: data?.activePayload?.[0]?.payload?.delivered_keys };
    },
    [SHOW_FILTERS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_WEIGHTS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: true,
    [BAR_CHART_REF_LINE_STROKE]: "#4f4f4f",
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      // @ts-ignore
      sprint_end_date: { ...sprint_end_date, required: true },
      aggMetric,
      percentile
    },
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [REPORT_FILTERS_CONFIG]: JiraSprintDistributionRetrospectiveReportFiltersConfig,
    [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintDistributionRetrospectiveReport;
