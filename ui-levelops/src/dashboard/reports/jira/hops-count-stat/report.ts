import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { statReportTransformer } from "custom-hooks/helpers";
import { jiraStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { HopsCountStatReportType } from "model/report/jira/hops-count-stat/hopCountStat.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "../../../constants/filter-name.mapping";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraHopsReportSingleStatFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraHopsCountStat: { hops_counts_stat: HopsCountStatReportType } = {
  hops_counts_stat: {
    name: "Issue Hops Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "hops_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "Hops"
    },
    default_query: statDefaultQuery,
    compareField: "median",
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraStatDrilldown,
      defaultSort: [{ id: "hops", desc: true }]
    },
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    transformFunction: data => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraHopsReportSingleStatFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraHopsCountStat;
