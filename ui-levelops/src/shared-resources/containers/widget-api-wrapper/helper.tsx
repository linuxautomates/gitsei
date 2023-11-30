import { WIDGET_FILTER_TAB_ORDER } from "constants/widgets";
import { customFieldFiltersSanitize } from "custom-hooks/helpers/zendeskCustomFieldsFiltersTransformer";
import { ACTIVE_SPRINT_TYPE_FILTER_KEY } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import { dateRangeFilterValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import * as AppName from "dashboard/constants/applications/names";
import {
  azureIterationSupportableReports,
  azureLeadTimeIssueReports,
  issueManagementReports,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_AZURE_REPORTS,
  JENKINS_REPORTS,
  jiraAzureScmAllLeadTimeReports,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_MAX_XAXIS_SUPPORTED_REPORTS,
  leadTimeReports,
  PAGERDUTY_REPORT,
  scmCicdReportTypes,
  scmEnhancedReports,
  TESTRAILS_REPORTS
} from "dashboard/constants/applications/names";
import {
  AZURE_ISSUE_CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_STACK_FLAG,
  SPRINT_FILTER_META_KEY,
  valuesToFilters
} from "dashboard/constants/constants";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import {
  ALLOW_KEY_FOR_STACKS,
  FILTER_KEY_MAPPING,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  RANGE_FILTER_CHOICE
} from "dashboard/constants/filter-key.mapping";
import { VALUE_SORT_KEY, WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { FileReports, ReportsApplicationType, scmTableWidgets } from "dashboard/constants/helper";
import { mapPartialStringFilters } from "dashboard/constants/mapPartialFilters.helper";
import {
  IssueVisualizationTypes,
  SCMReworkVisualizationTypes,
  SCMVisualizationTypes
} from "dashboard/constants/typeConstants";
import { buildMetricOptions, getOptionKey } from "dashboard/graph-filters/components/bullseye-filters/options.constant";
import {
  allTimeFilterKeys,
  CustomTimeBasedTypes,
  getValueFromTimeRange,
  rangeMap
} from "dashboard/graph-filters/components/helper";
import {
  allowWidgetDataSorting,
  getWidgetDataSortingSortValueNonTimeBased,
  transformAzureWidgetQueryForCustomFields
} from "dashboard/helpers/helper";
import {
  cloneDeep,
  forEach,
  get,
  isArray,
  isNull,
  isNumber,
  isUndefined,
  reduce,
  set,
  uniq,
  uniqBy,
  unset
} from "lodash";
import moment from "moment";
import {
  getTruthyValues,
  mapStringNumberKeysToNumber,
  sanitizeObject,
  sanitizeObjectCompletely,
  trimStringKeys
} from "utils/commonUtils";
import { transformFiltersZendesk } from "utils/dashboardFilterUtils";
import { GITHUB_APPLICATIONS } from "utils/reportListUtils";
import { getGroupByRootFolderKey } from "../../../configurable-dashboard/helpers/helper";
import { getDashboardTimeRange, isValidRelativeTime } from "../../../custom-hooks/helpers/statHelperFunctions";
import {
  jiraBacklogKeyMapping,
  jiraResolutionTimeReports,
  timePeriodTointervalMapping
} from "../../../dashboard/graph-filters/components/Constants";
import {
  APPLICATIONS_SUPPORTING_OU_FILTERS,
  JIRA_LEAD_TIME_REPORTS
} from "../../../dashboard/pages/dashboard-drill-down-preview/helper-constants";
import { ChartType } from "../chart-container/ChartType";
import { OUSupportedFiltersByApplication } from "../../../configurations/pages/Organization/Constants";
import { widgetDataSortingOptionKeys } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { leadtimeOUDesignationQueryBuilder } from "dashboard/helpers/widget-build-query/leadtimePayloadQuery";
import { pagerdutyValuesToFilters } from "dashboard/constants/applications/pagerduty/pagerduty-time-to-resolve/constant";
import { AcrossIsAzureIterationProps, AZURE_ITERATION_SORTING_VALUES } from "./helper-constant";
import { getWorkItemsType } from "dashboard/pages/dashboard-drill-down-preview/helper";
import { capitalizeFirstLetter, valueToTitle } from "utils/stringUtils";
import { getDateRangeEpochToString } from "utils/dateUtils";
import { METRIC_VALUE_KEY_MAPPING } from "dashboard/reports/azure/issues-report/constant";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { CodeVolumeVsDeployementIntervalMapping } from "constants/dashboard";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import { FilterTypes } from "constants/FilterTypes";

export interface OU_EXCLUSION_CONFIG_TYPES {
  prefixValue?: string | undefined;
  remove_normal_prefix?: boolean;
  remove_exclude_prefix?: boolean;
  remove_partial_prefix?: boolean;
}

export const getNewChartProps = (reportData: any, type: string) => {
  const updatedKeys = reduce(
    (reportData as any)?.data || [],
    (acc: any, next: any) => {
      return {
        ...acc,
        ...next
      };
    },
    {}
  );

  delete updatedKeys.count;
  delete updatedKeys.name;

  const keyAllowedForCurStacks = getWidgetConstant(type, ALLOW_KEY_FOR_STACKS);
  let ignoreKeys = ["toolTip", "id", "additional_key"];
  if (!!keyAllowedForCurStacks) {
    ignoreKeys.push("key");
  }

  const newChartProps = Object.keys(updatedKeys)
    .filter(key => !ignoreKeys.includes(key))
    .map(key => {
      return {
        name: key,
        dataKey: key
      };
    });

  return newChartProps || [];
};

export const getProps = (
  type: string,
  existingProps: any,
  reportData: any,
  filters: any,
  metaData: any,
  dashboardMetaData?: any
) => {
  if (!reportData) {
    return existingProps;
  }

  switch (type) {
    case "sprint_metrics_percentage_trend":
    case "azure_sprint_metrics_percentage_trend":
      const graphType = filters?.filter?.visualization || "stacked_area";
      if (graphType === "stacked_area") {
        return { ...existingProps, stackedArea: true };
      } else if (graphType === "line") {
        return { ...existingProps, lineProps: existingProps?.areaProps };
      }
      return { ...existingProps, stackedArea: filters?.filter?.stackedArea ?? false };
    case "jenkins_job_config_change_counts":
    case AppName.ZENDESK_TICKETS_REPORT:
    case "cicd_scm_jobs_count_report":
    case "cicd_pipeline_jobs_count_report":
    case "pagerduty_hotspot_report":
    case "pagerduty_ack_trend":
    case "pagerduty_after_hours":
    case "testrails_tests_report":
    case "testrails_tests_estimate_report":
    case "testrails_tests_estimate_forecast_report":
    case "pagerduty_release_incidents":
    case "tickets_report":
    case AppName.MICROSOFT_ISSUES_REPORT_NAME:
    case ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT:
    case "levelops_assessment_count_report":
    case "levelops_workitem_count_report":
    case "sonarqube_issues_report":
    case "praetorian_issues_report":
    case "ncc_group_vulnerability_report":
    case "snyk_vulnerability_report":
    case "azure_pipeline_jobs_duration_report":
    case "azure_pipeline_jobs_runs_report":
    case "sprint_impact_estimated_ticket_report":
    case "azure_sprint_impact_estimated_ticket_report":
    case "cicd_jobs_count_report":
    case "scm_issues_time_across_stages_report":
      if (
        (filters.stacks && filters.stacks.length) ||
        [
          "pagerduty_hotspot_report",
          "pagerduty_ack_trend",
          "pagerduty_after_hours",
          "pagerduty_release_incidents",
          "sprint_impact_estimated_ticket_report",
          "azure_sprint_impact_estimated_ticket_report"
        ].includes(type)
      ) {
        const newChartProps = getNewChartProps(reportData, type);
        let stacked = true;

        if (type === "pagerduty_hotspot_report" && (!filters.stacks || filters.stacks.length === 0)) {
          stacked = false;
        }

        const legendProps = get(existingProps, ["chartProps", "legendProps"], undefined);
        if (legendProps) {
          existingProps = {
            ...existingProps,
            legendProps: legendProps
          };
        }

        if (["tickets_report", ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(type)) {
          // TODO: It Should be refactor and move to reports helper file due to testing effort not doing it now
          const metric = filters?.filter?.metric || "tickets";
          const visualization = get(filters, ["filter", "visualization"], '"bar_chart"');
          const isPercent = visualization === IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART;
          const isLineChart = visualization === IssueVisualizationTypes.LINE_CHART;
          const isBarvisualization = visualization === IssueVisualizationTypes.BAR_CHART;
          const percentIncludeFn = (data: any, all_data: any) => {
            if (data.value) {
              const item = data?.payload || {};
              const allKeys = Object.keys(item).filter((key: any) => !["id", "key", "name", "toolTip"].includes(key));
              if (!isPercent) {
                const total = allKeys.reduce((acc: any, next: any) => {
                  return acc + item[next];
                }, 0);
                return `${data.value} (${((data.value / (total || 1)) * 100).toFixed(2)} %)`;
              }
              const record = all_data.find((_data: any) => _data?.key === item?.key);
              const data_key = METRIC_VALUE_KEY_MAPPING[metric || "total_tickets"];
              const total_count = Math.max(
                (record?.stacks || []).reduce((acc: number, record_data: any) => {
                  return acc + (record_data?.[data_key] ?? 0);
                }, 0),
                record?.[data_key]
              );
              if (record) {
                return `${Math.round((data.value * total_count) / 100)} (${data.value.toFixed(2)}%)`;
              }
              return data.value;
            }
            return isUndefined(data?.value) || isNull(data?.value) || isNaN(data?.value) ? "" : data.value;
          };
          const totalCountTransformFn = (data: any, meta_data: any) => {
            const item = meta_data?.payload || {};
            const data_key = METRIC_VALUE_KEY_MAPPING[metric || "total_tickets"];
            const record = meta_data?.all_data.find((_data: any) => _data?.key === item?.key);
            const total_count = Math.max(
              (record?.stacks || []).reduce((acc: number, record_data: any) => {
                return acc + (record_data?.[data_key] ?? 0);
              }, 0),
              record?.[data_key]
            );
            if (record) {
              return isPercent ? `${total_count} (${Math.round(data)} %)` : `${data} (100%)`;
            }
            return isPercent ? `${Math.round(data)} %` : data;
          };
          if (metric === "story_point") {
            return {
              ...existingProps,
              unit: "Story Points",
              stacked,
              barProps: [...newChartProps],
              lineProps: [...newChartProps],
              transformFn: isPercent ? (data: any) => data.toFixed(2) + " %" : undefined,
              percentIncludeFn: isBarvisualization || isPercent || isLineChart ? percentIncludeFn : undefined,
              all_data: (reportData as any)?.all_data || [],
              totalCountTransformFn: isPercent || isBarvisualization ? totalCountTransformFn : undefined
            };
          }
          if (metric === "effort") {
            return {
              ...existingProps,
              unit: "Effort",
              stacked,
              barProps: [...(existingProps?.barProps || []), ...newChartProps],
              lineProps: [...(existingProps?.barProps || []), ...newChartProps],
              transformFn: isPercent ? (data: any) => data.toFixed(2) + " %" : undefined,
              percentIncludeFn: isBarvisualization || isPercent || isLineChart ? percentIncludeFn : undefined,
              all_data: (reportData as any)?.all_data || [],
              totalCountTransformFn: isPercent || isBarvisualization ? totalCountTransformFn : undefined
            };
          }
          return {
            ...existingProps,
            stacked,
            barProps: [...newChartProps],
            lineProps: [...newChartProps],
            transformFn: isPercent ? (data: any) => data.toFixed(2) + " %" : undefined,
            percentIncludeFn: isBarvisualization || isPercent || isLineChart ? percentIncludeFn : undefined,
            all_data: (reportData as any)?.all_data || [],
            totalCountTransformFn: isPercent || isBarvisualization || isLineChart ? totalCountTransformFn : undefined
          };
        }

        if (["sprint_impact_estimated_ticket_report", "azure_sprint_impact_estimated_ticket_report"].includes(type)) {
          return {
            ...existingProps,
            stacked,
            barProps: [...(existingProps?.barProps || []), ...newChartProps]
          };
        }

        return { ...existingProps, stacked, barProps: [...newChartProps] };
      }

      if (["tickets_report", ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(type)) {
        // TODO: It Should be refactor and move to reports helper file due to testing effort not doing it now
        const metric = filters?.filter?.metric || "tickets";
        const visualization = get(filters, ["filter", "visualization"], '"bar_chart"');
        const isPercent = visualization === IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART;
        if (metric === "story_point") {
          return {
            ...existingProps,
            unit: "Story Points",
            barProps: [
              {
                name: "total_story_points",
                dataKey: "total_story_points",
                unit: "Story Points"
              }
            ],
            interval: filters?.interval || "day",
            transformFn: isPercent ? (data: any) => data.toFixed(2) + " %" : undefined
          };
        }
        if (metric === "effort") {
          return {
            ...existingProps,
            unit: "Effort",
            barProps: [
              {
                name: "total_effort",
                dataKey: "total_effort",
                unit: "Effort"
              }
            ],
            interval: filters?.interval || "day",
            transformFn: isPercent ? (data: any) => data.toFixed(2) + " %" : undefined
          };
        }
        return {
          ...existingProps,
          interval: filters?.interval || "day",
          transformFn: isPercent ? (data: any) => data.toFixed(2) + " %" : undefined
        };
      }

      return existingProps;
    case "assignee_time_report":
      return { ...existingProps };
    case "github_commits_report":
      return { ...existingProps, interval: filters?.interval || "day" };
    case AppName.BULLSEYE_REPORTS.BULLSEYE_BRANCH_COVERAGE_REPORT:
    case AppName.BULLSEYE_REPORTS.BULLSEYE_DECISION_COVERAGE_REPORT:
    case AppName.BULLSEYE_REPORTS.BULLSEYE_FUNCTION_COVERAGE_REPORT:
      if (filters?.filter?.metric) {
        if (isArray(filters?.filter?.metric)) {
          const metricArray = filters?.filter?.metric;
          let isPersent = false;
          let isNumber = false;
          forEach(metricArray, (item: string) => {
            if (item.includes("percentage")) isPersent = true;
            else isNumber = true;
          });
          const unit = isPersent && isNumber ? "Metrics" : isPersent ? "Percentage (%)" : "Count";
          const newBarProps = metricArray.map((item: any) => ({
            name: item,
            dataKey: item
          }));
          return {
            ...existingProps,
            unit,
            barProps: newBarProps,
            stacked: !!filters?.filter?.stacked_metrics
          };
        }
        return {
          ...existingProps,
          unit: filters?.filter?.metric?.includes("percentage") ? "Percentage (%)" : "Count",
          barProps: [{ ...existingProps.barProps[0], dataKey: filters?.filter?.metric }]
        };
      }
      return existingProps;
    case AppName.BULLSEYE_REPORTS.BULLSEYE_BRANCH_COVERAGE_TREND_REPORT:
    case AppName.BULLSEYE_REPORTS.BULLSEYE_DECISION_COVERAGE_TREND_REPORT:
    case AppName.BULLSEYE_REPORTS.BULLSEYE_FUNCTION_COVERAGE_TREND_REPORT:
      const metric = filters?.filter?.metric;
      const isStacked = filters?.filter?.stacked_metrics;
      return {
        ...(existingProps || {}),
        unit: isStacked
          ? "Metrics"
          : metric === undefined || metric?.includes("percentage")
          ? "Percentage (%)"
          : "Count"
      };
    case "sonarqube_code_complexity_report":
    case "sonarqube_code_complexity_trend_report":
      if (filters.filter && filters.filter.metrics) {
        const metricValue = filters?.filter?.metrics?.[0] || "";
        return {
          ...existingProps,
          unit: metricValue === "complexity" ? "Cyclomatic Complexity" : "Cognitive Complexity"
        };
      }
      return existingProps;
    case "resolution_time_report":
    case ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT:
    case "scm_issues_time_resolution_report":
      if (filters?.filter?.metric) {
        if (isArray(filters?.filter?.metric)) {
          const metricArray = filters?.filter?.metric;
          const newBarProps = metricArray.map((item: any) => {
            return {
              name: item,
              dataKey: item,
              unit: item.includes("tickets") ? "Tickets" : "Days"
            };
          });
          return {
            ...existingProps,
            unit: "Days",
            barProps: newBarProps,
            interval: filters?.interval || "day"
          };
        }
        return {
          ...existingProps,
          unit: filters?.filter?.metric?.includes("tickets") ? "Tickets" : "Days",
          interval: filters?.interval || "day",
          barProps: [{ ...existingProps.barProps[0], dataKey: filters?.filter?.metric }]
        };
      }
      return { ...existingProps, interval: filters?.interval || "day" };
    case "jira_time_across_stages":
    case ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES:
      if (filters.across && filters.across !== "none") {
        const updatedKeys = reduce(
          (reportData as any)?.data || [],
          (acc: any, next: any) => {
            return {
              ...acc,
              ...next
            };
          },
          {}
        );
        delete updatedKeys.count;
        delete updatedKeys.name;
        const newChartProps = Object.keys(updatedKeys)
          .filter(key => !["toolTip", "id", "key"].includes(key))
          .map(key => {
            return {
              name: key,
              dataKey: key
            };
          });

        return { ...existingProps, stacked: true, barProps: [...newChartProps] };
      }
      return existingProps;
    case JENKINS_REPORTS.SCM_CODING_DAYS_REPORT: {
      const metrics = get(filters, ["filter", "metrics"], "avg_coding_day_week");
      let newChartProps: any[] = [];
      switch (metrics) {
        case "avg_coding_day_week":
          newChartProps = [
            {
              name: "Average Coding days per week",
              dataKey: "avg_coding_day_week"
            }
          ];
          break;
        case "median_coding_day_week":
          newChartProps = [
            {
              name: "Median Coding days per week",
              dataKey: "median_coding_day_week"
            }
          ];
          break;
        case "avg_coding_day_biweekly":
          newChartProps = [
            {
              name: "Average Coding days per two weeks",
              dataKey: "avg_coding_day_biweekly"
            }
          ];
          break;
        case "median_coding_day_biweekly":
          newChartProps = [
            {
              name: "Median Coding days per two weeks",
              dataKey: "median_coding_day_biweekly"
            }
          ];
          break;
        case "avg_coding_day_month":
          newChartProps = [
            {
              name: "Average Coding days per month",
              dataKey: "avg_coding_day_month"
            }
          ];
          break;
        case "median_coding_day_month":
          newChartProps = [
            {
              name: "Median Coding days per month",
              dataKey: "median_coding_day_month"
            }
          ];
          break;
      }
      return { ...existingProps, barProps: newChartProps };
    }
    case JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT: {
      const visualization = get(metaData, "visualization", SCMVisualizationTypes.CIRCLE_CHART);
      const metrics = get(filters, ["filter", "metrics"], "average_author_response_time");
      let stacked = visualization === SCMVisualizationTypes.STACKED_AREA_CHART;
      let newChartProps: any[] = [];

      if (metrics.includes("author")) {
        if (metrics.includes("average")) {
          newChartProps = [
            {
              name: "Average Author Response Time",
              dataKey: "average_author_response_time"
            }
          ];
        } else {
          newChartProps = [
            {
              name: "Median Author Response Time",
              dataKey: "median_author_response_time"
            }
          ];
        }
      } else {
        if (metrics.includes("average")) {
          newChartProps = [
            {
              name: "Average Reviewer Response Time",
              dataKey: "average_reviewer_response_time"
            }
          ];
        } else {
          newChartProps = [
            {
              name: "Median Reviewer Response Time",
              dataKey: "median_reviewer_response_time"
            }
          ];
        }
      }

      if (filters.stacks && filters.stacks.length) {
        stacked = true;
        newChartProps = getNewChartProps(reportData, type);
      }

      return { ...existingProps, stacked, barProps: newChartProps };
    }
    case "scm_rework_report": {
      const visualization = get(metaData, "visualization", SCMReworkVisualizationTypes.STACKED_BAR_CHART);
      const isPercent = visualization === SCMReworkVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART;
      const updatedData = reduce(
        reportData?.data || [],
        (acc, next) => {
          return {
            ...acc,
            ...next
          };
        },
        {}
      );
      const barProps = Object.keys(updatedData)
        .filter(key => !["key", "name"].includes(key))
        .map(key => {
          return {
            name: key,
            dataKey: key
          };
        });
      return {
        ...existingProps,
        unit: isPercent ? "Percentage of LOC" : "Lines of Code",
        barProps,
        interval: filters?.interval || "day",
        transformFn: isPercent ? (data: string) => data + " %" : undefined
      };
    }
    case JENKINS_REPORTS.SCM_PRS_REPORT: {
      const visualization = get(metaData, "visualization", SCMVisualizationTypes.CIRCLE_CHART);
      const metrics = get(metaData, "metrics", "num_of_prs");
      let stacked = visualization === SCMVisualizationTypes.STACKED_AREA_CHART;
      const unit = metrics === "num_of_prs" ? "PRs" : "Filtered PRs (%)";

      let newChartProps: any[] = [];

      if (metrics === "num_of_prs") {
        newChartProps = [
          {
            key: "num_of_prs",
            dataKey: "num_of_prs"
          }
        ];
      } else {
        newChartProps = [
          {
            key: "filtered_prs_percentage",
            dataKey: "filtered_prs_percentage",
            unit: "%",
            transformer: (data: string) => data + " %"
          }
        ];
      }

      if (filters.stacks && filters.stacks.length) {
        stacked = true;
        newChartProps = getNewChartProps(reportData, type);
      }

      return { ...existingProps, stacked, unit, barProps: newChartProps };
    }

    case JENKINS_REPORTS.SCM_PRS_SINGLE_STAT: {
      const metrics = get(metaData, "metrics", "num_of_prs");
      const unit = metrics === "num_of_prs" ? "PRs" : "%";
      return { ...existingProps, unit };
    }
    case PAGERDUTY_REPORT.RESPONSE_REPORTS: {
      const isStacked = !!filters?.stacks && !!filters?.stacks.length;
      if (isStacked) {
        const updatedKeys = reduce(
          (reportData as any)?.data || [],
          (acc: any, next: any) => {
            return {
              ...acc,
              ...next
            };
          },
          {}
        );
        const newBarProps = Object.keys(updatedKeys)
          .filter(key => !["name", "key", "toolTip", "id", "median", "mean", "min", "max", "count"].includes(key))
          .map(key => {
            return {
              name: key,
              dataKey: key
            };
          });
        return { ...existingProps, stacked: true, barProps: newBarProps };
      }
      return existingProps;
    }
    case JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT:
    case ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT: {
      const metric = get(filters, ["filter", "metric"], "mean");
      let barProps = [...(existingProps?.barProps || [])];
      const stacked = !!(filters?.stacks && (filters?.stacks || []).length);
      const updatedData = reduce(
        reportData?.data || [],
        (acc, next) => {
          return {
            ...acc,
            ...next
          };
        },
        {}
      );

      barProps = Object.keys(updatedData)
        .filter(key => !["key", "name", "additional_key"].includes(key))
        .map(key => ({
          name: key,
          dataKey: key,
          stackId: stacked ? key.split(" - ")?.[0] : key
        }));

      return {
        ...existingProps,
        unit: metric === "total_tickets" ? "Tickets" : "Count",
        barProps: barProps,
        stacked: stacked
      };
    }
    case AppName.SCM_DORA_REPORTS.DEPLOYMENT_FREQUENCY:
      let timeRange = "";
      const dateRange = get(dashboardMetaData, "dashboard_time_range_filter", "last_30_days");
      const dashboard_time = get(metaData, "dashBoard_time_keys", {});
      const use_dashboard_time = Object.keys(dashboard_time).find((item: string) => item.includes("merged_at"));
      if (use_dashboard_time && typeof dateRange === "string") {
        timeRange = capitalizeFirstLetter(valueToTitle(dateRange));
      } else {
        timeRange = getDateRangeEpochToString(filters?.filter?.pr_merged_at);
      }
      return {
        ...existingProps,
        timeRange
      };

    default:
      return existingProps;
  }
};

export const getBacklogChartProps = (existingProps: any, metadata: any, filters: any, reportData: any) => {
  const chartType = get(metadata, ["graphType"], "bar_chart");
  const leftYAxis = get(metadata, "leftYAxis", "total_tickets");
  const rightYAxis = get(metadata, "rightYAxis", "median");
  const isStacked = !!filters?.stacks && !!filters?.stacks.length;
  const unit = leftYAxis === "total_tickets" ? "Tickets" : "Story Points";
  switch (chartType) {
    case "bar_chart":
      let newBarProps: any[] = [];
      let metrics: any[] = [];
      if (isStacked) {
        metrics = [rightYAxis];
        const updatedKeys = reduce(
          (reportData as any)?.data || [],
          (acc: any, next: any) => {
            return {
              ...acc,
              ...next
            };
          },
          {}
        );
        delete updatedKeys.count;
        delete updatedKeys.name;
        newBarProps = Object.keys(updatedKeys)
          .filter(
            key => !["toolTip", "id", "median", "p90", "mean", "total_tickets", "total_story_points"].includes(key)
          )
          .map(key => {
            return {
              name: key,
              dataKey: key
            };
          });
      } else {
        metrics = [leftYAxis, rightYAxis];
        const defaultProps = metrics.map((item: any) => {
          const mappedKey = get(jiraBacklogKeyMapping, [item], "");
          return {
            name: mappedKey,
            dataKey: item
          };
        });
        newBarProps = [...defaultProps, ...newBarProps];
      }

      const props = {
        ...existingProps,
        stacked: isStacked,
        barProps: newBarProps,
        bypassTitleTransform: true,
        unit
      };
      return { ...props };
    case "line_chart":
      return {
        ...existingProps,
        chartProps: existingProps.chartProps,
        unit
      };
    default:
      return existingProps;
  }
};

export const convertChildKeysToSiblingKeys = (inputObject: any, parentKey: string, siblingKeys: Array<string>) => {
  const parent = get(inputObject, [parentKey], undefined);
  if (!parent) {
    return inputObject;
  }

  let transformedData: { [x: string]: any } = {
    ...inputObject
  };

  siblingKeys.forEach((childKey: string) => {
    const child = get(parent, [childKey], undefined);

    if (child !== undefined && child !== null) {
      unset(transformedData, [parentKey, childKey]);

      transformedData = {
        ...transformedData,
        [childKey]: child
      };
    }
  });
  return transformedData;
};

export const mergeFilters = (
  _constFilters: any,
  _widgetFilters: any,
  _hiddenFilters: any,
  sanitize: boolean = true
) => {
  const constKeys = Object.keys(_constFilters);
  const widgetKeys = Object.keys(_widgetFilters);
  const hiddenKeys = Object.keys(_hiddenFilters);

  const allKeys = uniq([...constKeys, ...widgetKeys, ...hiddenKeys]);

  let filters = {};
  allKeys
    .filter((key: string) => key !== "exclude")
    .map((key: string) => {
      if (
        Array.isArray(_constFilters[key]) ||
        Array.isArray(_widgetFilters[key]) ||
        Array.isArray(_hiddenFilters[key])
      ) {
        filters = {
          ...filters,
          [key]: uniq([...(_constFilters[key] || []), ...(_widgetFilters[key] || []), ...(_hiddenFilters[key] || [])])
        };
      } else {
        filters = {
          ...filters,
          [key]: getTruthyValues([_widgetFilters[key], _constFilters[key], _hiddenFilters[key]])[0]
        };
      }
    });

  return sanitize ? trimStringKeys(filters) : filters;
};

// constFilters comes from [application].application.js file ex. jira.application.js
// widgetFilters comes from widget object
// hiddenFilters comes from [application].application.js file ex. testrails.application.js

export const combineAllFilters = (constFilters: any = {}, widgetFilters: any = {}, hiddenFilters: any = {}) => {
  const allKeys = uniq([
    ...Object.keys(constFilters || {}),
    ...Object.keys(widgetFilters || {}),
    ...Object.keys(hiddenFilters || {})
  ]);

  if (allKeys.includes("exclude")) {
    return {
      ...mergeFilters(constFilters, widgetFilters, hiddenFilters),
      exclude: {
        ...mergeFilters(constFilters.exclude || {}, widgetFilters.exclude || {}, hiddenFilters.exclude || {}, false)
      }
    };
  } else {
    return {
      ...mergeFilters(constFilters, widgetFilters, hiddenFilters)
    };
  }
};

export const bullseyeStackedMetricsData = (reportType: any, data: any) => {
  const stackedKeys = buildMetricOptions(getOptionKey(reportType) as any).map(item => item.value);
  return {
    data: (data || []).map((item: any) => {
      const filteredKeys = Object.keys(item || {}).filter(key => stackedKeys.includes(key));
      let _data: any = {};
      filteredKeys.forEach(key => (_data[key] = item[key]));
      _data = mapStringNumberKeysToNumber(_data);
      return { ..._data, name: item.name };
    })
  };
};

export const trimPartialStringFilters = (filters: any) => {
  const partialMatch = get(filters, ["partial_match"], undefined);
  if (partialMatch !== undefined) {
    let updatedPartialMatch: any = {};
    forEach(Object.keys(partialMatch), key => {
      updatedPartialMatch[key] = trimStringKeys(partialMatch[key]);
    });
    filters["partial_match"] = updatedPartialMatch;
    return filters;
  }
  return filters;
};

export const removeEmptyCustomFields = (filter: any, filter_key: any) => {
  const customFields = Object.keys(filter[filter_key]).reduce((acc: any, next: any) => {
    if (
      filter[filter_key][next] &&
      ((next && filter[filter_key][next].length) ||
        (typeof filter[filter_key][next] === "object" && Object.keys(filter[filter_key][next] || {}).length))
    ) {
      return { ...acc, [next]: filter[filter_key][next] };
    } else {
      unset(filter, [filter_key, next]);
      return acc;
    }
  }, {});
  return customFields;
};
export const removeFiltersWithEmptyValues = (
  filters: any,
  category: string = "default",
  metaData: any = {},
  removeMetaData: boolean = true
) => {
  const filtersKeys = Object.keys(filters);
  let tempFilters = cloneDeep(filters);
  filtersKeys.forEach((filter_key: string) => {
    switch (filter_key) {
      case "metadata":
        if (removeMetaData) {
          unset(tempFilters, "metadata");
        }
        break;
      case "custom_fields":
        const customFields = removeEmptyCustomFields(filters, filter_key);
        tempFilters = {
          ...tempFilters,
          custom_fields: customFields
        };
        break;

      case "exclude":
        Object.keys(filters[filter_key]).forEach((item: any) => {
          switch (item) {
            case "custom_fields":
              const customFields = removeEmptyCustomFields(filters[filter_key], item);
              tempFilters = {
                ...tempFilters,
                exclude: {
                  ...tempFilters.exclude,
                  custom_fields: customFields
                }
              };
              break;
            default:
              if (
                (Array.isArray(filters[filter_key][item]) && filters[filter_key][item].length === 0) ||
                Object.keys(filters[filter_key][item] || {})?.length === 0
              ) {
                unset(tempFilters, [filter_key, item]);
              }
              break;
          }
        });
        break;

      case "partial_match":
        const partialfilters = Object.keys(filters[filter_key]).reduce((acc: any, next: any) => {
          if (Object.keys(filters[filter_key][next]).length) {
            return { ...acc, [next]: filters[filter_key][next] };
          } else {
            return acc;
          }
        }, {});
        tempFilters = {
          ...tempFilters,
          partial_match: partialfilters
        };
        break;
      case "parent_story_points":
      case "story_points":
      case "score_range":
        if (!filters[filter_key]["$gt"] && !filters[filter_key]["$lt"]) {
          unset(tempFilters, [filter_key]);
        }
        break;
      case "issue_updated_at":
      case "issue_created_at":
      case "zendesk_created_at":
      case "salesforce_created_at":
      case "salesforce_updated_at":
      case "disclosure_range":
      case "publication_range":
      case "score_range":
      case "ingested_at":
      case "created_at":
      case "committed_at":
      case "end_time":
        if (!filters[filter_key]?.["$gt"]?.length || !filters[filter_key]?.["$lt"]?.length) {
          unset(tempFilters, [filter_key]);
          if (category !== "default" && Object.keys(metaData || {}).length) {
            unset(metaData, [category, "range_filter_choice", filter_key]);
          } else if (Object.keys(filters?.metadata || {}).length) {
            unset(filters?.metadata, ["range_filter_choice", filter_key]);
          }
        }
        break;
      default:
        if (
          (Array.isArray(filters[filter_key]) && filters[filter_key].length === 0) ||
          (typeof filters[filter_key] === "object" && Object.keys(filters[filter_key] || {})?.length === 0)
        ) {
          unset(tempFilters, [filter_key]);
        }
    }
  });
  return tempFilters;
};

export const removeGlobalEmptyValues = (filters: any) => {
  const filtersKeys = Object.keys(filters);
  let globalFilters = { ...filters };
  filtersKeys.forEach((filter_key: any) => {
    const categoryFilters = removeFiltersWithEmptyValues(filters[filter_key], filter_key, filters?.metadata);
    globalFilters = {
      ...globalFilters,
      [filter_key]: categoryFilters
    };
  });
  return globalFilters;
};

export const transformFiltersForCustomFieldsInStacks = (filters: any) => {
  let newFilters = filters;
  if (Array.isArray(newFilters.stacks)) {
    let already_inserted = false;
    newFilters = {
      ...filters,
      stacks: newFilters.stacks.reduce((sum: any[], value: string) => {
        let insert_me;
        if (typeof value === "string" && value.includes(CUSTOM_FIELD_PREFIX)) {
          if (!already_inserted) {
            insert_me = CUSTOM_FIELD_STACK_FLAG;
            already_inserted = true;
          }
        } else {
          insert_me = value;
        }

        const newSum = insert_me ? [...sum, insert_me] : sum;
        return newSum;
      }, [])
    };
  }

  return newFilters;
};

export const getMetrics = (metaData: any, defaultValue = ["median", "total_tickets"]) => {
  let metrics = get(metaData, ["metrics"], defaultValue);
  if (metrics.length === 0) {
    metrics = defaultValue;
  }
  return metrics;
};

export const backlogTrendReportChartType = (metaData: any) => {
  const metrics = getMetrics(metaData);
  if (metrics.length === 1) {
    if (metrics.includes("total_tickets") || metrics.includes("total_story_points")) {
      return ChartType.LINE;
    }
  } else {
    if (metrics.includes("total_tickets") || metrics.includes("total_story_points")) {
      return ChartType.COMPOSITE;
    }
  }
  return ChartType.BAR;
};

export const resolutionTimeReportChartType = (filter: any) => {
  const metrics = filter?.metric || [];
  if (metrics.length === 1) {
    if (metrics.includes("number_of_tickets_closed")) {
      return ChartType.LINE;
    }
  } else {
    if (metrics.includes("number_of_tickets_closed")) {
      return ChartType.COMPOSITE;
    }
  }
  return ChartType.BAR;
};

export const addSprintNames = (finalFilters: any, metadata: any) => {
  let sprintFilter = {};
  let sprintFilterKey = metadata?.[SPRINT_FILTER_META_KEY];

  const _filters = finalFilters.filter;
  const finalFilterKeys = Object.keys(_filters || {});

  if (sprintFilterKey) {
    if (finalFilterKeys.includes(sprintFilterKey)) {
      sprintFilter = {
        [FilterTypes.SPRINT_NAMES]: _filters[sprintFilterKey]
      };
    }

    if (finalFilterKeys.includes("exclude") && Object.keys(_filters.exlude || {}).includes(sprintFilterKey)) {
      sprintFilter = {
        [FilterTypes.SPRINT_NAMES]: _filters.exclude[sprintFilterKey]
      };
    }

    if (
      finalFilterKeys.includes("custom_fields") &&
      Object.keys(_filters.custom_fields || {}).includes(sprintFilterKey)
    ) {
      sprintFilter = {
        [FilterTypes.SPRINT_NAMES]: _filters.custom_fields[sprintFilterKey]
      };
    }

    if (
      finalFilterKeys.includes("partial_match") &&
      Object.keys(_filters.partial_match || {}).includes(sprintFilterKey)
    ) {
      finalFilters = {
        ...finalFilters,
        filter: {
          ..._filters,
          partial_match: {
            ...(_filters.partial_match || {}),
            sprint_name: _filters.partial_match[sprintFilterKey]
          }
        }
      };
    }

    finalFilters = {
      ...finalFilters,
      filter: {
        ...(finalFilters.filter || {}),
        ...sprintFilter
      }
    };

    return finalFilters;
  }
  return finalFilters;
};

export const getWidgetUri = (reportType: string, uri: string, filters: any, metadata: any) => {
  let newUri = uri;
  const uriMappings = get(widgetConstants, [reportType, "METRIC_URI_MAPPING"], undefined);
  const groupByKey = getGroupByRootFolderKey(reportType);
  if (
    ["jira_zendesk_files", "jira_salesforce_files", "scm_jira_files_report", "scm_files_report"].includes(uri) &&
    get(metadata, [groupByKey], undefined) &&
    get(widgetConstants, [reportType, "rootFolderURI"], undefined)
  ) {
    newUri = get(widgetConstants, [reportType, "rootFolderURI"], "");
  }

  if (
    [
      JiraReports.JIRA_TICKETS_REPORT,
      ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
      ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT
    ].includes(reportType as any) &&
    filters?.metric === "story_point"
  ) {
    newUri = get(widgetConstants, [reportType, "storyPointUri"], "");
  }

  if (
    [JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_SINGLE_STAT].includes(
      reportType as any
    ) &&
    (filters?.metrics || "").includes("reviewer")
  ) {
    newUri = get(widgetConstants, [reportType, "reviewerUri"], "");
  }

  if (reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS && filters?.metric === "acknowledge") {
    newUri = get(widgetConstants, [reportType, "acknowledgeUri"], "");
  }

  if (uriMappings) {
    newUri = uriMappings[filters?.metric] || newUri;
  }
  return newUri;
};

export const getSupportedApplications = (reportType: string) => {
  const application = get(widgetConstants, [reportType, "application"], undefined);
  if (["lead_time_by_stage_report", "lead_time_by_type_report", "lead_time_trend_report"].includes(reportType)) {
    return [IntegrationTypes.JIRA];
  } else if (reportType === "azure_lead_time_single_stat") {
    return [IntegrationTypes.AZURE, IntegrationTypes.GITHUB];
  } else if (["lead_time_single_stat", "scm_jira_files_report"].includes(reportType)) {
    return [IntegrationTypes.JIRA, IntegrationTypes.GITHUB];
  } else if (application === IntegrationTypes.JENKINSGITHUB) {
    return [IntegrationTypes.JENKINS];
  } else {
    return [application];
  }
};

export const getAllowedKeys = (reportType: string) => {
  const application = get(widgetConstants, [reportType, "application"], undefined);
  let allowedFilters = get(OUSupportedFiltersByApplication, [application, "values"], []);

  if (application === IntegrationTypes.GITHUB) {
    allowedFilters = get(OUSupportedFiltersByApplication, application, []).reduce((acc: string[], next: any) => {
      return [...acc, ...(next?.values || [])];
    }, []);
  } else if (application === IntegrationTypes.AZURE) {
    allowedFilters = get(OUSupportedFiltersByApplication, application, []).reduce((acc: string[], next: any) => {
      return [...acc, ...(next?.values || [])];
    }, []);
    allowedFilters = [...allowedFilters, "code_area", "teams", "workitem_sprint_full_names"];
  } else if (application === IntegrationTypes.JENKINSGITHUB) {
    allowedFilters = get(OUSupportedFiltersByApplication, [IntegrationTypes.JENKINS, "values"], []);
  } else if (application === IntegrationTypes.GITHUBJIRA) {
    const githubFilters = get(OUSupportedFiltersByApplication, IntegrationTypes.GITHUB, []).reduce(
      (acc: string[], next: any) => {
        return [...acc, ...(next?.values || [])];
      },
      []
    );
    allowedFilters = [...get(OUSupportedFiltersByApplication, [IntegrationTypes.JIRA, "values"], []), ...githubFilters];
  } else if (application === IntegrationTypes.PAGERDUTY) {
    const supportValues = get(OUSupportedFiltersByApplication, [application, "values"], []).map((val: string) => {
      return get(pagerdutyValuesToFilters, [val], val);
    });
    allowedFilters = [...supportValues];
  }

  return [...allowedFilters, ...allowedFilters.map((filter: string) => get(valuesToFilters, filter, filter))];
};

// Note: not being used anywhere
export const getOUExclusionKeys = (
  filters: any,
  reportType: string,
  supportedCustomFields?: any,
  config?: OU_EXCLUSION_CONFIG_TYPES,
  drilldownFilterValue?: string
) => {
  const allowedFilters = getAllowedKeys(reportType);

  const excludeFilterKeys = Object.keys(filters?.exclude || {}).map((key: string) => {
    if (config && config?.remove_exclude_prefix) {
      return key.replace(config?.prefixValue as string, "");
    }
    return key;
  });
  const partialMatchFilterKeys = Object.keys(filters?.partial_match || {}).map((key: string) => {
    if (key?.includes("workitem_")) {
      const newKey = supportedCustomFields?.find(
        (customField: any) => customField?.field_key === key.replace("workitem_", "")
      )?.field_key;
      if (newKey) {
        return newKey;
      }
    }
    if (config && config?.remove_partial_prefix) {
      return key.replace(config?.prefixValue as string, "");
    }
    return key;
  });
  const missingFieldsFiltersKeys = Object.keys(filters?.missing_fields || {});
  const normalFilterKeys = Object.keys(filters || {}).map((key: string) => {
    if (config && config?.remove_normal_prefix) {
      return key.replace(config?.prefixValue as string, "");
    }
    return key;
  });
  const customFieldKeys = Object.keys(filters?.custom_fields || {});
  const excludeCustomFieldKeys = Object.keys(filters?.exclude?.custom_fields || {});
  const excludeWorkitemCustomFieldKeys = Object.keys(filters?.exclude?.workitem_custom_fields || {});
  const workitemCustomFieldKeys = Object.keys(filters?.workitem_custom_fields || {});
  const workitemAttributesKeys = Object.keys(filters?.workitem_attributes || {})
    .map((key: string) => {
      if (filters?.workitem_attributes?.[key]?.length) {
        return key;
      }
      return undefined;
    })
    .filter((key: any) => !!key);
  const allExclusionKeys = [
    ...normalFilterKeys,
    ...excludeFilterKeys,
    ...partialMatchFilterKeys,
    ...missingFieldsFiltersKeys,
    ...excludeCustomFieldKeys,
    ...customFieldKeys,
    ...excludeWorkitemCustomFieldKeys,
    ...workitemCustomFieldKeys,
    ...workitemAttributesKeys,
    drilldownFilterValue
  ].filter((key: string | undefined) => !!key);
  const filteredKeys = allExclusionKeys.filter((key: string) => {
    const customKey = supportedCustomFields?.find(
      (customField: any) => customField?.field_key === key.replace("workitem_", "")
    )?.field_key;

    if (customKey) {
      return true;
    }
    return allowedFilters.includes(key);
  });

  return uniq(filteredKeys) || [];
};

// it return true if across is azure iteration else false
export const acrossIsAzureIteration = (props: AcrossIsAzureIterationProps) => {
  const { across, application, reportType } = props;
  return (
    application === IntegrationTypes.AZURE &&
    across === AppName.SPRINT &&
    !Object.values(AppName.AZURE_SPRINT_REPORTS).includes(reportType as AppName.AZURE_SPRINT_REPORTS)
  );
};

export const widgetApiFilters = (payload: any) => {
  const {
    widgetFilters,
    filters,
    hiddenFilters,
    globalFilters,
    reportType,
    contextFilters,
    updateTimeFilters,
    widgetId,
    widgetMetaData,
    uri,
    application,
    jiraOrFilters,
    maxRecords,
    filterKey,
    updatedUri,
    excludeStatusState,
    supportedCustomFields,
    dashboardMetaData,
    scmGlobalSettings,
    availableIntegrations,
    dashboardOuIdsRef,
    queryParamOU,
    customFieldRecords,
    workflowProfile,
    tempWidgetInterval
  } = payload;
  const combinedFilters = combineAllFilters(widgetFilters || {}, cloneDeep(filters), hiddenFilters || {});

  let finalFilters: { [x: string]: any } = {
    filter: {
      ...combinedFilters,
      ...globalFilters
    }
  };
  const reportCustomFieldKey = get(widgetConstants, [reportType || "", "custom_fields"], undefined);
  const mapFiltersForWidgetApi = get(widgetConstants, [reportType || "", "mapFiltersForWidgetApi"], undefined);
  const excludeStageValues = get(widgetMetaData, ["hide_stages"], undefined);
  if (mapFiltersForWidgetApi) {
    let excludeFilters = mapFiltersForWidgetApi(finalFilters, excludeStageValues);
    finalFilters = {
      ...finalFilters,
      ...excludeFilters
    };
  }

  if (JENKINS_AZURE_REPORTS.includes(reportType)) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...finalFilters.filter,
        cicd_integration_ids: finalFilters.filter?.integration_ids
      },
      cicd_integration_ids: finalFilters.filter?.integration_ids
    };
  }

  if (reportType && reportType.includes("levelops")) {
    unset(finalFilters, ["filter", "product_id"]);
    unset(finalFilters, ["filter", "integration_ids"]);
  }

  if (reportType === "praetorian_issues_report") {
    let filters = finalFilters.filter || {};
    const priorities = filters.priorities;
    if (priorities) {
      delete filters.priorities;
      finalFilters = {
        ...finalFilters,
        filter: {
          ...filters,
          priority: [...priorities]
        }
      };
    }
  }

  const contextFilter = (contextFilters as any)[widgetId];
  if (reportType === "cicd_pipeline_jobs_count_report" && contextFilter?.localFilters) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...finalFilters.filter,
        parent_cicd_job_ids: (contextFilter.localFilters as any).parent_cicd_job_ids
      }
    };
  }

  if (reportType === JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT) {
    let paginationFilter = contextFilter ? contextFilter : { page: 0, page_size: 10 };
    finalFilters = {
      ...finalFilters,
      ...paginationFilter
    };
  }

  if (
    [
      FileReports.JIRA_SALESFORCE_FILES_REPORT,
      FileReports.JIRA_ZENDESK_FILES_REPORT,
      FileReports.SCM_FILES_REPORT,
      FileReports.SCM_JIRA_FILES_REPORT
    ].includes(reportType as any)
  ) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...finalFilters.filter,
        [filterKey]: get(finalFilters, ["filter", filterKey], "")
      }
    };
  }

  if (AppName.LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT.includes(reportType)) {
    set(finalFilters, ["filter", "calculateSingleState"], true);
  }

  if (["jira", "azure_devops"].includes(application) && AppName.ALL_VELOCITY_PROFILE_REPORTS.includes(reportType)) {
    let workItemsType = getWorkItemsType(application);
    if (workItemsType) {
      set(finalFilters, ["filter", "work_items_type"], workItemsType);
    }
  }

  finalFilters = convertChildKeysToSiblingKeys(finalFilters, "filter", [
    "across",
    "stacks",
    "sort",
    "interval",
    "filter_across_values"
  ]);

  finalFilters = transformFiltersForCustomFieldsInStacks(finalFilters);

  if (
    reportType &&
    reportType.includes("levelops") &&
    finalFilters.filter?.reporters &&
    finalFilters.filter?.reporters.length > 0
  ) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...(finalFilters.filter || {}),
        reporters: get(finalFilters.filter, ["reporters"], []).map((reporter: any) => {
          const result = typeof reporter === "string" ? reporter : reporter.label;
          return result;
        })
      }
    };
  }

  finalFilters = mapPartialStringFilters(finalFilters, reportType);

  if (updateTimeFilters) {
    finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, widgetMetaData, reportType, uri);
  }

  if (finalFilters.hasOwnProperty("sort") && Array.isArray(finalFilters.sort)) {
    finalFilters["sort"] = uniqBy(finalFilters.sort, "id");
  }

  if (application === ReportsApplicationType.ZENDESK) {
    finalFilters = customFieldFiltersSanitize(finalFilters, true);
    const { zendeskCustomFields } = transformFiltersZendesk(finalFilters?.filter);
    if (Object.keys(zendeskCustomFields || {}).length > 0) {
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          custom_fields: {
            ...(zendeskCustomFields || {})
          }
        }
      };
    }
  }

  if (application === ReportsApplicationType.JIRA_ZENDESK) {
    const { jiraCustomFields, zendeskCustomFields } = transformFiltersZendesk(finalFilters?.filter);
    const customFields = get(finalFilters, ["filter", "custom_fields"], {});
    if (Object.keys(customFields).length > 0) {
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          custom_fields: {
            ...(jiraCustomFields || {}),
            ...(zendeskCustomFields || {})
          }
        }
      };
    }
  }

  if (application === ReportsApplicationType.BULLSEYE) {
    if ("name" in finalFilters.filter) {
      // For Bullseye name filter, the payload key needs to be 'names'
      delete Object.assign(finalFilters.filter, { names: finalFilters.filter["name"] })["name"];
    }
  }

  // this check is added to fix the bug in old reports that are still sending trend in across
  // @ts-ignore
  if (scmCicdReportTypes.includes(reportType) && finalFilters.across === "trend") {
    finalFilters["across"] = "job_end";
  }

  // to add across_limit in jira applications
  if (
    (application === ReportsApplicationType.JIRA && JIRA_MAX_XAXIS_SUPPORTED_REPORTS.includes(reportType)) ||
    [
      ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
      ISSUE_MANAGEMENT_REPORTS.FIRST_ASSIGNEE_REPORT,
      TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT,
      TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_REPORT,
      TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT
    ].includes(reportType)
  ) {
    finalFilters = {
      ...finalFilters,
      across_limit: maxRecords || 20
    };
  }

  if (["jira", "githubjira"].includes(application) && Object.keys(jiraOrFilters || {}).length > 0) {
    let key = application === IntegrationTypes.JIRA ? "or" : "jira_or";
    const jiraOrFilterKey = get(widgetConstants, [reportType, "jira_or_filter_key"]);
    if (jiraOrFilterKey) {
      key = jiraOrFilterKey;
    }
    finalFilters = {
      ...finalFilters,
      filter: {
        ...(finalFilters.filter || {}),
        [key]: removeFiltersWithEmptyValues(jiraOrFilters)
      }
    };
  }

  if (scmTableWidgets.includes(reportType) && contextFilter) {
    finalFilters = {
      ...finalFilters,
      sort: [contextFilter]
    };
  }

  // Adding  this check to remove stacks from filters in case where aggregation has time
  // field and stacks were already added (as it was enabled in UI earlier)
  if (reportType === "zendesk_tickets_report" && ["ticket_created"].includes(finalFilters.across)) {
    delete finalFilters.stacks;
  }

  if (Object.keys(finalFilters.filter || {}).includes("last_sprint") && finalFilters?.filter?.last_sprint) {
    finalFilters = addSprintNames(finalFilters, widgetMetaData);
  }

  if (
    [
      ...issueManagementReports,
      ...AppName.ALL_VELOCITY_PROFILE_REPORTS,
      TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT,
      TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT
    ].includes(reportType as any)
  ) {
    const customFields = get(finalFilters, ["filter", "custom_fields"], {});
    const excludeFields = get(finalFilters, ["filter", "exclude"], {});
    const excludeCustomFields = get(finalFilters, ["filter", "exclude", "custom_fields"], {});
    const partialFilterKey = getWidgetConstant(reportType, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
    let partialFields = get(finalFilters, ["filter", partialFilterKey], {});

    if (Object.keys(partialFields || {}).includes("jira_label")) {
      const jira_labels = partialFields?.jira_label || undefined;
      delete partialFields.jira_label;
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          [partialFilterKey]: {
            ...(partialFields || {}),
            jira_labels
          }
        }
      };
    }
    let custom_field_prefix = "jira_";
    let custom_field_key = reportCustomFieldKey || "jira_custom_fields";
    if ([...azureLeadTimeIssueReports, ...issueManagementReports].includes(reportType as any)) {
      custom_field_key = "workitem_custom_fields";
      custom_field_prefix = "workitem_";
    }
    if (
      [TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT, TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT].includes(
        reportType as any
      )
    ) {
      custom_field_key = "custom_fields";
      custom_field_prefix = "testrails_";
    }

    if (AppName.LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT.includes(reportType)) {
      custom_field_key = "custom_fields";
      custom_field_prefix = "";
    }
    if (Object.keys(partialFields).length > 0) {
      Object.keys(partialFields || {}).forEach(field => {
        if (field.includes(CUSTOM_FIELD_PREFIX)) {
          const val = partialFields[field];
          delete partialFields[field];
          partialFields = {
            ...partialFields,
            [`${custom_field_prefix}${field}`]: val
          };
        }
      });
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          [partialFilterKey]: {
            ...(partialFields || {})
          }
        }
      };
    }
    if (Object.keys(customFields).length > 0 || finalFilters.filter.hasOwnProperty("custom_fields")) {
      delete finalFilters.filter.custom_fields;
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          [custom_field_key]: {
            ...(customFields || {})
          }
        }
      };
    }
    if (Object.keys(excludeFields).length > 0 && excludeFields?.custom_fields) {
      delete finalFilters.filter.exclude.custom_fields;
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          exclude: {
            ...finalFilters.filter.exclude,
            [custom_field_key]: { ...excludeCustomFields }
          }
        }
      };
    }
  }

  // TODO: Can be removed later, this is added to handle the already added reports
  if (
    [JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT, JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT].includes(
      reportType
    ) &&
    !finalFilters.hasOwnProperty("sort")
  ) {
    finalFilters["sort"] = [{ id: "job_end", desc: false }];
  }

  finalFilters = sanitizeObject(finalFilters);
  finalFilters = {
    ...(finalFilters || {}),
    filter: sanitizeObject(finalFilters?.filter || {})
  };

  const sortingOption = get(finalFilters, ["filter", WIDGET_DATA_SORT_FILTER_KEY], "");

  if (allowWidgetDataSorting(reportType, finalFilters) && sortingOption) {
    let across: string = get(finalFilters, ["across"], "");
    let valueSortKey = getWidgetConstant(reportType, VALUE_SORT_KEY, "");
    if (across.includes(CUSTOM_FIELD_PREFIX) || across.includes(AZURE_ISSUE_CUSTOM_FIELD_PREFIX)) {
      across = "custom_field";
    }
    if (
      [JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(reportType) &&
      ["issue_management_story_point_report", "story_point_report"].includes(updatedUri)
    ) {
      valueSortKey = "story_points";
    }

    if (reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      valueSortKey = across;
    }
    if (reportType === JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT) {
      const metric = get(finalFilters, ["filter", "metrics"], "average_author_response_time");
      valueSortKey = metric.includes("average_") ? "mean_author_response_time" : "median_author_response_time";
    }
    if (allTimeFilterKeys.includes(across)) {
      finalFilters["sort"] = [{ id: across, desc: true }];
    } else {
      finalFilters["sort"] = getWidgetDataSortingSortValueNonTimeBased(across, sortingOption, valueSortKey);
    }
  }

  finalFilters.filter = sanitizeStages(excludeStatusState, reportType, finalFilters.filter);

  const azureCodeAreaValues: any = get(finalFilters, ["filter", "workitem_attributes", "code_area"], undefined);
  if (azureCodeAreaValues) {
    const newAzureCodeAreaValues = azureCodeAreaValues.map((value: any) => {
      if (typeof value === "object") {
        return `${value.child}`;
      } else {
        // This is just for backward compatibility with old version that had string values
        return value;
      }
    });
    let key = "workitem_attributes";
    unset(finalFilters, ["filter", "workitem_attributes", "code_area"]);
    finalFilters = {
      ...finalFilters,
      filter: {
        ...(finalFilters.filter || {}),
        [key]: {
          ...finalFilters.filter.workitem_attributes,
          ["code_area"]: newAzureCodeAreaValues
        }
      }
    };
  }

  const azureIterationValues: any = get(finalFilters, ["filter", "azure_iteration"], undefined);
  const excludeAzureIterationValues: any = get(finalFilters, ["filter", "exclude", "azure_iteration"], undefined);
  const partialAzureIterationValue: any = get(finalFilters, ["filter", "partial_match", "azure_iteration"], undefined);
  if (application === IntegrationTypes.AZURE && excludeAzureIterationValues) {
    const newExcludeAzureIterationValues = excludeAzureIterationValues.map((value: any) => {
      if (typeof value === "object") {
        return `${value.parent}\\${value.child}`;
      } else {
        // This is just for backward compatibility with old version that had string values
        return value;
      }
    });
    let key = "workitem_sprint_full_names";
    unset(finalFilters, ["filter", "exclude", "azure_iteration"]);
    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...(finalFilters?.filter || {}),
        exclude: {
          ...finalFilters.filter.exclude,
          [key]: newExcludeAzureIterationValues
        }
      }
    };
  }

  if (application === IntegrationTypes.AZURE && partialAzureIterationValue) {
    unset(finalFilters, ["filter", "partial_match", "azure_iteration"]);
    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...(finalFilters?.filter || {}),
        partial_match: {
          ...finalFilters.filter.partial_match,
          workitem_milestone_full_name: partialAzureIterationValue
        }
      }
    };
  }
  if (application === IntegrationTypes.AZURE && azureIterationValues) {
    const newAzureIterationValues = azureIterationValues.map((value: any) => {
      if (typeof value === "object") {
        return `${value.parent}\\${value.child}`;
      } else {
        // This is just for backward compatibility with old version that had string values
        return value;
      }
    });
    let key = "workitem_sprint_full_names";
    unset(finalFilters, ["filter", "azure_iteration"]);
    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...(finalFilters?.filter || {}),
        [key]: newAzureIterationValues
      }
    };
  }

  if (acrossIsAzureIteration({ reportType, across: get(finalFilters, ["across"], ""), application })) {
    let across = AppName.SPRINT,
      valueSortKey =
        widgetDataSortingOptionKeys.SPRINT_START_DATE_OLD_LATEST === sortingOption
          ? AZURE_ITERATION_SORTING_VALUES.MILESTONE_START_DATE
          : AZURE_ITERATION_SORTING_VALUES.MILESTONE_END_DATE;

    finalFilters["sort"] = getWidgetDataSortingSortValueNonTimeBased(
      across,
      widgetDataSortingOptionKeys.VALUE_LOW_HIGH,
      valueSortKey
    );
  }

  if (scmEnhancedReports.includes(reportType as any)) {
    // TODO update SCM widget filters with help of scmSettings
    const code_change_size_unit = get(widgetMetaData, "code_change_size_unit", scmGlobalSettings.code_change_size_unit);

    const code_change_small = get(widgetMetaData, "code_change_size_small", scmGlobalSettings.code_change_size_small);
    const code_change_medium = get(
      widgetMetaData,
      "code_change_size_medium",
      scmGlobalSettings?.code_change_size_medium
    );

    const comment_density_small = get(widgetMetaData, "comment_density_small", scmGlobalSettings.comment_density_small);
    const comment_density_medium = get(
      widgetMetaData,
      "comment_density_medium",
      scmGlobalSettings?.comment_density_medium
    );

    let newMappedFilters: any = {
      code_change_size_unit,
      code_change_size_config: {
        small: code_change_small?.toString(),
        medium: code_change_medium?.toString()
      }
    };
    if (!newMappedFilters?.code_change_size_config?.small) {
      delete newMappedFilters?.code_change_size_config?.small;
    }
    if (!newMappedFilters?.code_change_size_config?.medium) {
      delete newMappedFilters?.code_change_size_config?.medium;
    }

    if (!["github_commits_report", "github_commits_single_stat"].includes(reportType)) {
      newMappedFilters = {
        ...newMappedFilters,
        comment_density_size_config: {
          shallow: comment_density_small?.toString(),
          good: comment_density_medium?.toString()
        }
      };

      if (!newMappedFilters?.comment_density_size_config?.shallow) {
        delete newMappedFilters?.comment_density_size_config?.shallow;
      }
      if (!newMappedFilters?.comment_density_size_config?.good) {
        delete newMappedFilters?.comment_density_size_config?.good;
      }
    }

    // transform day to day_of_week for By Day of Week
    if (["github_commits_report"].includes(reportType)) {
      let interval = get(finalFilters, "interval");
      if (interval && interval === "day") {
        finalFilters = {
          ...(finalFilters || {}),
          interval: "day_of_week"
        };
      }
    }

    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...(finalFilters?.filter || {}),
        ...newMappedFilters
      }
    };
  }

  if (reportType === "scm_rework_report") {
    const interval: number = get(widgetMetaData, "legacy_update_interval_config", 30);
    const now = moment().startOf("day").unix();
    const prev = now - interval * 86400;
    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...finalFilters.filter,
        legacy_update_interval_config: prev.toString()
      }
    };
  }

  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...finalFilters.filter,
      ...updateTimeFiltersValue(dashboardMetaData, widgetMetaData, { ...finalFilters?.filter })
    }
  };

  // TODO: need to discuss
  if (isArray(supportedCustomFields)) {
    finalFilters = sanitizeCustomDateFilters(finalFilters, supportedCustomFields);
  }

  if (["sprint_goal"].includes(reportType)) {
    const state = get(finalFilters, ["filter", "state"], "");
    if (state === "active") {
      unset(finalFilters, ["filter", "completed_at"]);
    }
    const sprint_filter_val = get(finalFilters, ["filter", "sprint"], null);
    if (sprint_filter_val !== null) {
      const partialFilterKey = getWidgetConstant(reportType, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
      unset(finalFilters, ["filter", "sprint"]);
      finalFilters = {
        ...finalFilters,
        filter: {
          ...finalFilters.filter,
          [partialFilterKey]: {
            ...finalFilters.filter[partialFilterKey],
            ["sprint"]: { $contains: sprint_filter_val }
          }
        }
      };
    }
  }

  const filterKeyMapping = getWidgetConstant(reportType, FILTER_KEY_MAPPING, {});

  if (Object.keys(filterKeyMapping).length) {
    Object.keys(filterKeyMapping).forEach(key => {
      const val = get(finalFilters, ["filter", key], null);
      if (val !== null) {
        unset(finalFilters, ["filter", key]);
        finalFilters = {
          ...finalFilters,
          filter: {
            ...finalFilters.filter,
            [filterKeyMapping[key]]: val
          }
        };
      }
      const partialFilterKey = getWidgetConstant(reportType, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
      const partial_val = get(finalFilters, ["filter", partialFilterKey, key], null);
      if (partial_val !== null) {
        unset(finalFilters, ["filter", partialFilterKey, key]);
        finalFilters = {
          ...finalFilters,
          filter: {
            ...finalFilters.filter,
            [partialFilterKey]: {
              ...finalFilters.filter[partialFilterKey],
              [filterKeyMapping[key]]: partial_val
            }
          }
        };
      }
      const exclude_val = get(finalFilters, ["filter", "exclude", key], null);
      if (exclude_val !== null) {
        unset(finalFilters, ["filter", "exclude", key]);
        finalFilters = {
          ...(finalFilters || {}),
          filter: {
            ...(finalFilters?.filter || {}),
            exclude: {
              ...finalFilters.filter.exclude,
              [filterKeyMapping[key]]: exclude_val
            }
          }
        };
      }
    });
  }

  if ("code_volume_vs_deployment_report" === reportType) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...(finalFilters.filter || {}),
        deploy_job: {
          ...(finalFilters?.filter?.deploy_job || {}),
          cicd_integration_ids: finalFilters?.filter?.integration_ids || []
        },
        build_job: {
          ...(finalFilters?.filter?.build_job || {}),
          cicd_integration_ids: finalFilters?.filter?.integration_ids || []
        }
      },
      interval: get(CodeVolumeVsDeployementIntervalMapping, [finalFilters.interval], finalFilters.interval)
    };

    delete finalFilters.filter.integration_ids;
  }

  if (
    application === IntegrationTypes.JIRA &&
    finalFilters?.filter?.sprint_states &&
    JIRA_LEAD_TIME_REPORTS.includes(reportType)
  ) {
    finalFilters = {
      ...finalFilters,
      filter: { ...finalFilters.filter, [ACTIVE_SPRINT_TYPE_FILTER_KEY]: finalFilters?.filter?.sprint_states }
    };
    unset(finalFilters, ["filter", "sprint_states"]);
  }

  if (
    application === IntegrationTypes.JIRA &&
    finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY] &&
    !JIRA_LEAD_TIME_REPORTS.includes(reportType)
  ) {
    if (reportType === "sprint_goal") {
      finalFilters = {
        ...finalFilters,
        filter: {
          ...finalFilters.filter,
          state: finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY][0]
        }
      };
    } else {
      finalFilters = {
        ...finalFilters,
        filter: { ...finalFilters.filter, sprint_states: finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY] }
      };
    }

    unset(finalFilters, ["filter", ACTIVE_SPRINT_TYPE_FILTER_KEY]);
  }

  // handling Jira reports
  if (
    [
      JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_SINGLE_STAT_REPORT,
      JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT
    ].includes(reportType)
  ) {
    let filterKey = "issue_created_at"; // used by resolution time single stat report

    if (finalFilters.across) {
      filterKey = `${finalFilters.across}_at`;
    }

    finalFilters = updateIssueAndResolutionStatFilters(finalFilters, filterKey, widgetMetaData, dashboardMetaData);
  }

  // handling Azure reports
  if (
    [
      ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_SINGLE_STAT_REPORT,
      ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT
    ].includes(reportType)
  ) {
    let filterKey = "workitem_created_at";

    if (finalFilters.across) {
      filterKey = `${finalFilters.across}`;
    }

    finalFilters = updateIssueAndResolutionStatFilters(finalFilters, filterKey, widgetMetaData, dashboardMetaData);
  }

  if (JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT === reportType) {
    let filterKey = "start_time";
    finalFilters = updateIssueAndResolutionStatFilters(finalFilters, filterKey, widgetMetaData, dashboardMetaData);
  }

  const ou_ids = queryParamOU ? [queryParamOU] : get(dashboardMetaData, "ou_ids", []);

  if (
    ou_ids.length &&
    (APPLICATIONS_SUPPORTING_OU_FILTERS.includes(application) ||
      [PAGERDUTY_REPORT.RESPONSE_REPORTS].includes(reportType))
  ) {
    let combinedOUFilters = {
      ...get(dashboardMetaData, "ou_user_filter_designation", {}),
      ...sanitizeObject(get(widgetMetaData, "ou_user_filter_designation", {}))
    };

    const supportedApplications = getSupportedApplications(reportType);
    Object.keys(combinedOUFilters).forEach((key: string) => {
      if (!supportedApplications.includes(key)) {
        delete combinedOUFilters?.[key];
      }
    });

    if (["jira", "azure_devops", "githubjira"].includes(application)) {
      let sprint: string | undefined = "";

      if (azureIterationSupportableReports.includes(reportType)) {
        sprint = "sprint_report";
      } else {
        const sprintCustomField = supportedCustomFields.find((item: any) =>
          (item.name || "").toLowerCase().includes("sprint")
        );
        if (sprintCustomField) {
          sprint = sprintCustomField?.field_key;
        }
      }

      combinedOUFilters = {
        ...combinedOUFilters,
        sprint: [sprint]
      };
    }
    if (
      combinedOUFilters?.hasOwnProperty("sprint") &&
      (!combinedOUFilters?.sprint || !combinedOUFilters?.sprint?.[0])
    ) {
      delete combinedOUFilters?.sprint;
    }

    finalFilters = {
      ...finalFilters,
      ou_ids,
      ou_user_filter_designation: Object.keys(combinedOUFilters).length ? combinedOUFilters : undefined
    };
  }

  const effort_investment_profile = get(dashboardMetaData, ["effort_investment_profile"], false);
  if (
    [
      jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
      jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT,
      jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT,
      jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT,
      ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT,
      ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT,
      ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
      ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT
    ].includes(reportType) &&
    effort_investment_profile
  ) {
    const effort_investment_profile_filter = get(dashboardMetaData, ["effort_investment_profile_filter"]);
    finalFilters = {
      ...finalFilters,
      filter: { ...finalFilters.filter, ticket_categorization_scheme: effort_investment_profile_filter },
      across_limit: 500
    };
  }

  const removeNoLongerSupportedFilter = get(
    widgetConstants,
    [reportType, AppName.NO_LONGER_SUPPORTED_FILTER],
    undefined
  );

  if (removeNoLongerSupportedFilter) {
    finalFilters = {
      ...finalFilters,
      filter: { ...removeNoLongerSupportedFilter(finalFilters.filter) }
    };
  }

  // key for custom fields stacks is stacks itself not custom_stacks (jira)
  // this is the check for fixing the same
  if (issueManagementReports.includes(reportType)) {
    const customStacks = get(finalFilters, ["filter", "custom_stacks"], []);
    const stacks = get(finalFilters, ["stacks"], []).filter((st: string) => st !== CUSTOM_FIELD_STACK_FLAG);
    if (customStacks.length) {
      set(finalFilters, "stacks", customStacks);
      unset(finalFilters, ["filter", "custom_stacks"]);
    } else if (!stacks.length) {
      unset(finalFilters, ["filter", "custom_stacks"]);
      unset(finalFilters, ["stacks"]);
    }
  }

  // Getting all available applications.
  // If dashboard doesn't have any integrations to it, availableApplications try to fetch all the possible integrations
  const availableApplications: string[] = availableIntegrations.map((integration: any) => integration?.application);
  const allowedIntegrations = get(finalFilters, ["filter", "integration_ids"], []);
  // currently filtering integration ids only for lead time reports based on issue management system.
  if (
    jiraAzureScmAllLeadTimeReports.includes(reportType) &&
    availableApplications.includes(IntegrationTypes.JIRA) &&
    availableApplications.includes(IntegrationTypes.AZURE) &&
    allowedIntegrations.length > 0
  ) {
    let newIntegrationIds: string[] = [];
    forEach(availableIntegrations, integration => {
      let valid = false;
      if (application === IntegrationTypes.GITHUBJIRA) {
        valid = [IntegrationTypes.GITHUB, IntegrationTypes.JIRA].includes(integration?.application);
      } else if (application == IntegrationTypes.GITHUB) {
        // azure_devops is also supported by github widgets
        valid = [IntegrationTypes.GITHUB, IntegrationTypes.AZURE, ...GITHUB_APPLICATIONS].includes(
          integration?.application
        );
      } else {
        valid = integration?.application === application;
      }
      if (valid) {
        newIntegrationIds.push(integration?.id);
      }
    });
    finalFilters = {
      ...finalFilters,
      filter: {
        ...get(finalFilters, ["filter"], {}),
        integration_ids: newIntegrationIds
      }
    };
  }

  finalFilters = leadtimeOUDesignationQueryBuilder(finalFilters, reportType, availableIntegrations);

  // It would be great if we don't write any filter transform logic beyond this
  const defaultSort = getWidgetConstant(reportType, "defaultSort");
  const getGraphFilters = getWidgetConstant(reportType, AppName.GET_GRAPH_FILTERS);

  if (getGraphFilters) {
    finalFilters = getGraphFilters({
      finalFilters,
      contextFilter,
      defaultSort,
      ou_ids,
      dashboardOuIdsRef,
      showDashboardOrg: !!dashboardMetaData?.show_org_unit_selection,
      availableIntegrations: availableIntegrations,
      workflowProfile: workflowProfile,
      dashboardMetaData: dashboardMetaData,
      reportType: reportType,
      widgetMetaData: widgetMetaData,
      integrationIds: allowedIntegrations
    });
  }

  if (!AppName.ALL_VELOCITY_PROFILE_REPORTS.includes(reportType as any)) {
    delete finalFilters?.filter?.velocity_config_id;
  } else if (widgetMetaData?.hasOwnProperty("apply_ou_on_velocity_report")) {
    finalFilters = {
      ...finalFilters,
      apply_ou_on_velocity_report: widgetMetaData.apply_ou_on_velocity_report
    };
  }

  /** for issues report if sprint as custom field is across then need to change it to sprint. */
  const transformFinalFilters = getWidgetConstant(reportType, ["transformFinalFilters"], null);
  if (transformFinalFilters) {
    finalFilters = transformFinalFilters(finalFilters, supportedCustomFields);
  }

  // Appending the widget id to the payload
  if (!!widgetId) {
    finalFilters = {
      ...finalFilters,
      widget_id: widgetId.replace("-preview", "")
    };
  }

  if (application === IntegrationTypes.AZURE && customFieldRecords) {
    finalFilters = transformAzureWidgetQueryForCustomFields(finalFilters, customFieldRecords);
  }

  const widgetFiltersTransformer = get(widgetConstants, [reportType, "widget_filter_transform"]);
  if (widgetFiltersTransformer) {
    finalFilters = widgetFiltersTransformer(finalFilters);
  }
  if (tempWidgetInterval) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...finalFilters?.filter,
        interval: tempWidgetInterval
      }
    };
  }
  return sanitizeObjectCompletely(finalFilters);
};

const PARTIAL_MATCH_PREFIXS = ["jira_", "workitem_"];

export const sanitizeCustomDateFilters = (filters: any, supportedCustomFields: any[]) => {
  // get all custom_filter keys
  Object.keys(filters.filter)
    .filter(key => key.includes("custom_fields"))
    .forEach(key => {
      const customFilter = filters.filter[key];
      Object.keys(customFilter || {}).forEach(_key => {
        Object.keys(filters.filter.partial_match || {}).forEach(_partialKey => {
          if (_partialKey.includes(_key)) {
            delete filters.filter.partial_match[_partialKey];
          }
        });

        if (Object.keys(filters.filter?.exclude?.[key] || {})?.includes(_key)) {
          delete filters.filter.exclude[key][_key];
        }
      });
    });

  const fieldKeys = (supportedCustomFields || [])?.map((item: any) => item.field_key) || [];

  // custom fields
  Object.keys(filters.filter)
    .filter(key => key.includes("custom_fields"))
    .forEach(key => {
      const customFilter = filters.filter[key];
      Object.keys(customFilter || {}).forEach(_key => {
        if (!fieldKeys.includes(_key)) {
          delete filters?.filter?.[key]?.[_key];
        }
      });
    });

  Object.keys(filters?.filter?.exclude || {})
    .filter(key => key.includes("custom_fields"))
    .forEach(key => {
      const customFilter = filters.filter?.exclude?.[key];
      Object.keys(customFilter || {}).forEach(_key => {
        if (!fieldKeys.includes(_key)) {
          delete filters?.filter?.exclude?.[key]?.[_key];
        }
      });
    });

  const dateTimeFields =
    (supportedCustomFields || [])?.filter((item: any) => CustomTimeBasedTypes.includes(item.field_type)) || [];

  Object.keys(filters?.filter?.partial_match || {}).forEach(key => {
    // removing jira or workitem from the starting of the key
    let modifiedKey = key;
    const index = PARTIAL_MATCH_PREFIXS.findIndex((prefix: string) => modifiedKey.startsWith(prefix));
    if (index !== -1) {
      modifiedKey = modifiedKey.replace(PARTIAL_MATCH_PREFIXS[index], "");
    }
    if (key?.toLowerCase().includes("custom") && (!fieldKeys.includes(modifiedKey) || dateTimeFields.includes(key))) {
      delete filters?.filter?.partial_match?.[key];
    }
  });

  return filters;
};

export const sanitizeStages = (excludeStatusState: any, reportType: any, filter: any) => {
  let _filter = filter;

  if (jiraResolutionTimeReports.includes(reportType)) {
    const excludeStatus = get(excludeStatusState, ["data", "records", 0, "status"], []).map((item: any) => item.key);
    const existingStages = get(_filter, ["exclude", "stages"], []);

    if (existingStages.length > 0) {
      const updatedStages = existingStages.filter((stage: string) => !excludeStatus.includes(stage));
      _filter = {
        ..._filter,
        exclude: {
          ..._filter.exclude,
          stages: updatedStages
        }
      };
    }
  }

  return _filter;
};

export const updateTimeFiltersValue = (
  dashboardMetaData: any,
  widgetMetaData: any,
  filters: any,
  ignoreKeys = [""]
) => {
  const dateRange = get(dashboardMetaData, "dashboard_time_range_filter", "last_30_days");
  const dashboard_time = get(widgetMetaData, "dashBoard_time_keys", {});
  // filtering ignoreKeys for updation
  const dashboard_use_time_keys = Object.keys(dashboard_time).filter(
    (item: string) => !ignoreKeys.includes(dashboard_time[item]) && dashboard_time[item].use_dashboard_time
  );
  let newFilters = { ...(filters || {}) };
  dashboard_use_time_keys.forEach((key: string) => {
    if (key.includes("customfield")) {
      if (newFilters.custom_fields) {
        newFilters.custom_fields[key] = {
          $gt: dateRangeFilterValue(dateRange).$gt.toString(),
          $lt: dateRangeFilterValue(dateRange).$lt.toString()
        };
      } else {
        newFilters = {
          ...newFilters,
          custom_fields: {
            [key]: {
              $gt: dateRangeFilterValue(dateRange).$gt.toString(),
              $lt: dateRangeFilterValue(dateRange).$lt.toString()
            }
          }
        };
      }
    } else {
      newFilters[key] = {
        $gt: dateRangeFilterValue(dateRange).$gt.toString(),
        $lt: dateRangeFilterValue(dateRange).$lt.toString()
      };
    }
  });

  return newFilters;
};

const getInterval = (timePeriod: number, interval: any) => {
  return get(timePeriodTointervalMapping, [timePeriod], interval);
};

export const updateIssueAndResolutionStatFilters = (
  finalFilters: any,
  filterKey: string,
  widgetMeta: any,
  dashMeta: any
) => {
  const metaKey = get(rangeMap, filterKey, filterKey);

  let relativeTime = widgetMeta?.[RANGE_FILTER_CHOICE]?.[metaKey]?.relative || {};

  const useDashboardTime = widgetMeta?.dashBoard_time_keys?.[filterKey]?.use_dashboard_time || false;

  if (isDashboardTimerangeEnabled(dashMeta || {}) && useDashboardTime) {
    const range = getDashboardTimeRange(dashMeta, true);
    if (range) {
      finalFilters = {
        ...finalFilters,
        filter: {
          ...(finalFilters.filter || {}),
          [filterKey]: range
        }
      };
    } else {
      delete finalFilters.filter?.[filterKey];
    }
  } else {
    const isValidTime = isValidRelativeTime(relativeTime);
    if (Object.keys(relativeTime).length && isValidTime) {
      const gap = parseInt(relativeTime.last.num) + parseInt(relativeTime?.next?.num || "0");

      const value = getValueFromTimeRange(
        {
          ...(relativeTime || {}),
          last: {
            num: parseInt(relativeTime.last.num) + gap,
            unit: relativeTime.last.unit
          }
        },
        true
      );

      finalFilters = {
        ...finalFilters,
        filter: {
          ...(finalFilters.filter || {}),
          [filterKey]: value
        }
      };
    } else {
      delete finalFilters.filter?.[filterKey];
    }
  }

  return finalFilters;
};

export const mapWidgetMetadataForCompare = (widgetMetadata: any) => {
  const updated = { ...(widgetMetadata || {}) };
  // const ignoreKeys = [
  //   WIDGET_FILTER_TAB_ORDER,
  //   RANGE_FILTER_CHOICE,
  //   "disable_issue_management_system",
  //   "disable_support_system",
  //   "default_value",
  //   "metric",
  //   "metrics"
  // ];
  const ignoreKeys = [WIDGET_FILTER_TAB_ORDER, "drilldown_columns"];
  forEach(ignoreKeys, key => unset(updated, key));
  return updated;
};
