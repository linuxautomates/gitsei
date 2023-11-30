import moment from "moment";
import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { volumeChangeTransformer } from "../../../shared-resources/helpers/transformers/github-jenkin-volume-change-transformer";
import {
  cicdSCMJobCountTransformer,
  jenkinsJobConfigChangeTransform,
  jobsChangeVolumeTransform,
  seriesDataTransformer,
  statReportTransformer,
  timeDurationGenericTransform,
  trendReportTransformer
} from "../../../custom-hooks/helpers";
import { jenkinsPipelineJobsCountTransformer } from "../../../custom-hooks/helpers/helper";
import {
  jenkinsJobConfigSupportedFilters,
  jenkinsGithubJobSupportedFilters,
  jenkinsPipelineJobSupportedFilters,
  junitSupportedFilters,
  scmCicdSupportedFilters,
  CodeVolVsDeployemntSupportedFilters
} from "../supported-filters.constant";
import {
  ChartContainerType,
  transformSCMCommitToCICDJobLeadTimeSingleStatReportPrevQuery,
  transformCICDJobReport
} from "../../helpers/helper";
import { jenkinsDrilldownTransformer } from "dashboard/helpers/drilldown-transformers/jenkinsDrilldownTransformer";
import {
  jenkinsJobCountDrilldown,
  junitTestDrilldown,
  jenkinsPipelineDrilldown,
  jenkinsGithubJobRunDrilldown,
  jenkinsJobCountStatDrilldown,
  jenkinsGithubJobRunStatDrilldown,
  scmCicdDrilldown,
  scmCicdStatDrilldown,
  jenkinsPipelineJobDrilldown,
  jenkinsCodeVsDeploymentDrilldown
} from "../drilldown.constants";
import {
  cicdJobCountStatDefaultQuery,
  cicdTrendDefaultQuery,
  jobCommitSingleStatDefaultQuery,
  scmCicdDefaultQuery,
  scmCicdVolumeStatDefaultQuery,
  shouldSliceFromEnd,
  statDefaultQuery,
  xAxisLabelTransform
} from "../helper";
import {
  jenkinsPartialFilterKeyMapping,
  PARTIAL_FILTER_MAPPING_KEY,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB,
  RANGE_FILTER_CHOICE,
  DEFAULT_METADATA,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  INCLUDE_ACROSS_OU_EXCLUSIONS,
  CSV_DRILLDOWN_TRANSFORMER
} from "../filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  FILTER_PARENT_AND_VALUE_KEY,
  GET_PARENT_AND_TYPE_KEY,
  METADATA_FILTERS_PREVIEW,
  scmCicdFilterOptionsMapping,
  SUPPORTED_FILTERS_WITH_INFO,
  VALUE_SORT_KEY,
  WIDGET_DATA_SORT_FILTER_KEY,
  WIDGET_FILTER_PREVIEW_COUNT,
  WIDGET_VALIDATION_FUNCTION
} from "../filter-name.mapping";
import {
  cicdJobCountTransformer,
  cicdTrendReportTransform,
  jenkinsConfigChangeCountTrendTransformer,
  scmCicdTrendTransformer
} from "../../../custom-hooks/helpers/trendReport.helper";
import {
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  PREV_REPORT_TRANSFORMER,
  FE_BASED_FILTERS,
  HIDE_CUSTOM_FIELDS,
  REPORT_FILTERS_CONFIG,
  TIME_FILTER_RANGE_CHOICE_MAPPER,
  DEPRECATED_MESSAGE
} from "./names";
import { jenkinsRangeChoiceMapping, jenkinsGithubRangeChoiceMapping } from "../timeFilterRangeChoiceMapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "../WidgetDataSortingFilter.constant";
import { CodeVolVsDeployemntTransformer } from "custom-hooks/helpers/seriesData.helper";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { get } from "lodash";
import { hasValue } from "dashboard/components/dashboard-header/helper";
import { toTitleCase } from "utils/stringUtils";
import { sCMCommitToCICDJobLeadTimeSingleStatReportTransformerWrapper } from "custom-hooks/helpers/issuesSingleStat.helper";
import { CICD_DEPRECATED_MESSAGE, show_value_on_bar } from "./constant";
import { cicdJobCountReportTooltipTransformer } from "../chartTooltipTransform/cicdJobCountReportTooltip.transformer";
import { JenkinsJobConfigChangeStatReportConfig } from "dashboard/reports/jenkins/jenkins-job-config-change-counts-stat-report/filters.config";
import { JenkinsJobConfigChangeCountsReportConfig } from "dashboard/reports/jenkins/jenkins-job-config-change-counts-report/filters.config";
import { JenkinsJobConfigChangeCountsTrendsReportConfig } from "dashboard/reports/jenkins/jenkins-job-config-change-counts-trend-report/filters.config";
import { JenkinsJobsCountSingleStatReportConfig } from "dashboard/reports/jenkins/jobs-count-single-stat-report/filters.config";
import { JenkinsJobsCommitsLeadSingleStatReportConfig } from "dashboard/reports/jenkins/jobs-commits-lead-single-stat-report/filters.config";
import { JenkinsJobsDurationSingleStatReportConfig } from "dashboard/reports/jenkins/jobs-duration-single-stat-report/filters.config";
import { JenkinsChangeVolumeSingleStatReportConfig } from "dashboard/reports/jenkins/jobs-change-volumes-single-stat-report/filters.config";
import { getFilterConfigCicdJobDuration } from "dashboard/reports/jenkins/cicd-scm-jobs-duration-report/filters.config";
import { getFilterConfigCicdPipelineJobDuration } from "dashboard/reports/jenkins/cicd-pipeline-jobs-duration-report/filters.config";
import { getFilterConfigCicdPipelineJobDurationTrends } from "dashboard/reports/jenkins/cicd-pipeline-jobs-duration-trend-report/filters.config";
import { getFilterConfigCicdJobCount } from "dashboard/reports/jenkins/cicd-jobs-count-report/filters.config";
import { JenkinsCICDPipelineJobsCountReportConfig } from "dashboard/reports/jenkins/cicd-pipeline-jobs-count-report/filters.config";
import { getFilterConfigCicdPipelineJobsCountTrends } from "dashboard/reports/jenkins/cicd-pipeline-jobs-count-trend-report/filters.config";
import { JenkinsJobsCountTrendsReportConfig } from "dashboard/reports/jenkins/jobs-count-trends-report/filters.config";
import { JenkinsJobsDurationTrendsReportConfig } from "dashboard/reports/jenkins/jobs-durations-trends-report/filters.config";
import { JenkinsChangeVolumeTrendsReportConfig } from "dashboard/reports/jenkins/jobs-change-volumes-trends-report/filters.config";
import { JenkinsJobRunsTestReportConfig } from "dashboard/reports/jenkins/job-runs-test-report/filters.config";
import { JenkinsJobRunsTestTrendReportConfig } from "dashboard/reports/jenkins/job-runs-test-trend-report/filters.config";
import { JenkinsJobRunsTestDurationReportConfig } from "dashboard/reports/jenkins/job-runs-test-duration-report/filters.config";
import { JenkinsJobRunsTestDurationTrendsReportConfig } from "dashboard/reports/jenkins/job-runs-test-duration-trend-report/filters.config";
import { JenkinsCodeVolVsDeployementReportConfig } from "dashboard/reports/jenkins/code-volume-vs-deployment-report/filters.config";
import { JenkinsJobsCommitLeadsTrendsReportConfig } from "dashboard/reports/jenkins/jenkins-cicd-job-lead-time/filters.config";
import { cicdCsvDrilldownDataTransformer } from "../../helpers/csv-transformers/cicdCsvTransformer";
import { IntegrationTypes } from "constants/IntegrationTypes";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const job_end_date = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Job End Date",
  BE_key: "end_time",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

const job_start_date = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "JOB START DATE",
  BE_key: "start_time",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const codeVolVsDeploymentDefaultQuery = {
  interval: "week"
};

const interval = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Interval",
  BE_key: "interval",
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  defaultValue: "weekly",
  options: [
    { label: "Weekly", value: "week" },
    { label: "Bi-Weekly", value: "biweekly" },
    { label: "Monthly", value: "month" }
  ]
};

const onChartClickPayload = params => {
  const { data, across } = params;
  const _data = data?.activePayload?.[0]?.payload || {};
  let payload = {
    name: _data.name || data.activeLabel || ""
  };
  if (across === "qualified_job_name") {
    payload.value = {
      instance_name: _data.additional_key,
      job_name: _data.name
    };
  } else if (across === "trend") {
    payload.value = _data.key;
  } else {
    payload.value = _data.name || data.activeLabel;
  }
  return payload;
};

const commitToCICDLeadTimeSingleStatDefaultMeta = {
  [RANGE_FILTER_CHOICE]: {
    start_time: {
      type: "relative",
      relative: {
        last: {
          num: 1,
          unit: "days"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

const jenkinsApiBasedFilterKeyMapping = {
  cicd_job_ids: "cicd_job_id"
};

export const JenkinsDashboards = {
  jenkins_job_config_change_counts_stat: {
    name: "CICD Job Config Change Count Single Stat",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jenkins_job_config_change_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "Jobs"
    },
    default_query: statDefaultQuery,
    compareField: "count",
    drilldown: jenkinsJobCountStatDrilldown,
    supported_filters: jenkinsJobConfigSupportedFilters,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: JenkinsJobConfigChangeStatReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jenkins_job_config_change_counts: {
    name: "CICD Job Config Change Count Report",
    application: IntegrationTypes.JENKINS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jenkins_job_config_change_report",
    method: "list",
    defaultAcross: "cicd_user_id",
    filters: {},
    appendAcrossOptions: [{ label: "Instance Name", value: "instance_name" }],
    stack_filters: ["trend", "job_name", "cicd_user_id", "qualified_job_name"],
    xaxis: true,
    chart_props: {
      unit: "Jobs",
      chartProps: chartProps,
      barProps: [
        {
          name: "count",
          dataKey: "count"
        }
      ],
      stacked: false
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [VALUE_SORT_KEY]: "count",
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    supported_filters: jenkinsJobConfigSupportedFilters,
    drilldown: jenkinsJobCountDrilldown,
    shouldJsonParseXAxis: () => true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => jenkinsJobConfigChangeTransform(data),
    [REPORT_FILTERS_CONFIG]: JenkinsJobConfigChangeCountsReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jenkins_job_config_change_counts_trend: {
    name: "CICD Job Config Change Count Trend Report",
    application: IntegrationTypes.JENKINS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jenkins_job_config_change_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    composite: true,
    composite_transform: {
      count: "jenkins_jobs_config_count"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      ...cicdTrendDefaultQuery,
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    chart_props: {
      unit: "Jobs",
      chartProps: chartProps
    },
    supported_filters: jenkinsJobConfigSupportedFilters,
    drilldown: jenkinsJobCountDrilldown,
    onChartClickPayload,
    shouldJsonParseXAxis: () => true,
    transformFunction: data => jenkinsConfigChangeCountTrendTransformer(data),
    valuesToFilters: {
      trend: "job_config_changed_at"
    },
    [REPORT_FILTERS_CONFIG]: JenkinsJobConfigChangeCountsTrendsReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jobs_count_single_stat_report: {
    name: "CICD Jobs Count Single Stat",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_count_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "jobs"
    },
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    default_query: cicdJobCountStatDefaultQuery,
    drilldown: jenkinsGithubJobRunStatDrilldown,
    compareField: "count",
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    supported_filters: {
      ...jenkinsGithubJobSupportedFilters,
      values: [...jenkinsGithubJobSupportedFilters.values]
    },
    supportExcludeFilters: true,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: JenkinsJobsCountSingleStatReportConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [DEPRECATED_MESSAGE]: CICD_DEPRECATED_MESSAGE
  },
  jobs_commits_lead_single_stat_report: {
    name: "SCM Commit to CICD Job Lead Time Single Stat",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_commits_lead_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "days"
    },
    default_query: jobCommitSingleStatDefaultQuery,
    compareField: "sum",
    supported_filters: scmCicdSupportedFilters,
    supportExcludeFilters: true,
    drilldown: scmCicdStatDrilldown,
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    transformFunction: data => sCMCommitToCICDJobLeadTimeSingleStatReportTransformerWrapper(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [PREV_REPORT_TRANSFORMER]: data => transformSCMCommitToCICDJobLeadTimeSingleStatReportPrevQuery(data),
    [FE_BASED_FILTERS]: {
      job_start_date
    },
    [WIDGET_VALIDATION_FUNCTION]: payload => {
      const { query } = payload;
      const start_time = get(query, "start_time", undefined);
      const isAggType = query?.agg_type !== undefined;
      return start_time && isAggType ? true : false;
    },
    hasStatUnit: compareField => true,
    [DEFAULT_METADATA]: commitToCICDLeadTimeSingleStatDefaultMeta,
    [REPORT_FILTERS_CONFIG]: JenkinsJobsCommitsLeadSingleStatReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jobs_duration_single_stat_report: {
    name: "CICD Job Duration Single Stat",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_duration_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "hours"
    },
    default_query: statDefaultQuery,
    compareField: "sum",
    drilldown: jenkinsGithubJobRunStatDrilldown,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    supported_filters: {
      ...jenkinsGithubJobSupportedFilters,
      values: [...jenkinsGithubJobSupportedFilters.values, "job_normalized_full_name"]
    },
    supportExcludeFilters: true,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: JenkinsJobsDurationSingleStatReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jobs_change_volumes_single_stat_report: {
    name: "SCM Change Volume to CICD Jobs Single Stat",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_change_volume_report",
    method: "list",
    filters: {
      across: "trend",
      by: "lines"
    },
    xaxis: false,
    chart_props: {
      unit: "lines"
    },
    default_query: scmCicdVolumeStatDefaultQuery,
    compareField: "lines",
    supported_filters: scmCicdSupportedFilters,
    supportExcludeFilters: true,
    drilldown: scmCicdStatDrilldown,
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: JenkinsChangeVolumeSingleStatReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  cicd_scm_jobs_duration_report: {
    name: "CICD Job Duration Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_duration_report",
    defaultAcross: "cicd_user_id",
    appendAcrossOptions: [
      { label: "Instance Name", value: "instance_name" },
      { label: "Qualified Name", value: "job_normalized_full_name" },
      { label: "Project", value: "project_name" }
    ],
    defaultFilterKey: "median",
    method: "list",
    filters: {},
    chart_props: {
      unit: "Minutes",
      barProps: [
        {
          name: "Min duration of pipelines",
          dataKey: "min"
        },
        {
          name: "Median duration of pipelines",
          dataKey: "median"
        },
        {
          name: "Max duration of pipelines",
          dataKey: "max"
        }
      ],
      chartProps: chartProps
    },
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    supported_filters: jenkinsGithubJobSupportedFilters,
    supportExcludeFilters: true,
    xaxis: true,
    across: ["cicd_user_id", "job_name"],
    convertTo: "mins",
    drilldown: jenkinsGithubJobRunDrilldown,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsGithubRangeChoiceMapping,
    transformFunction: data => timeDurationGenericTransform(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [VALUE_SORT_KEY]: "duration",
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    [REPORT_FILTERS_CONFIG]: getFilterConfigCicdJobDuration,
    [HIDE_CUSTOM_FIELDS]: true,
    acrossFilterLabelMapping: scmCicdFilterOptionsMapping,
    [CSV_DRILLDOWN_TRANSFORMER]: cicdCsvDrilldownDataTransformer
  },
  cicd_pipeline_jobs_duration_report: {
    name: "CICD Pipeline Jobs Duration Report",
    application: IntegrationTypes.JENKINS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pipelines_jobs_duration_report",
    defaultFilterKey: "median",
    method: "list",
    appendAcrossOptions: [
      { label: "Instance Name", value: "instance_name" },
      { label: "Qualified Name", value: "job_normalized_full_name" }
    ],
    filters: {},
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    chart_props: {
      unit: "Minutes",
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      chartProps: chartProps
    },
    supported_filters: jenkinsPipelineJobSupportedFilters,
    xaxis: true,
    convertTo: "mins",
    drilldown: jenkinsPipelineJobDrilldown,
    across: ["cicd_job_id", "job_name", "qualified_job_name", "instance_name"],
    defaultAcross: "cicd_job_id",
    transformFunction: data => timeDurationGenericTransform(data),
    onChartClickPayload,
    shouldJsonParseXAxis: () => true,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsRangeChoiceMapping,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [VALUE_SORT_KEY]: "duration",
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    valuesToFilters: {
      qualified_job_name: "qualified_job_names",
      cicd_job_id: "job_names", // Not a mistake
      job_normalized_full_name: "job_normalized_full_names"
    },
    [REPORT_FILTERS_CONFIG]: getFilterConfigCicdPipelineJobDuration,
    [HIDE_CUSTOM_FIELDS]: true,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    acrossFilterLabelMapping: scmCicdFilterOptionsMapping,
    [CSV_DRILLDOWN_TRANSFORMER]: cicdCsvDrilldownDataTransformer
  },
  cicd_pipeline_jobs_duration_trend_report: {
    name: "CICD Pipeline Jobs Duration Trend Report",
    application: IntegrationTypes.JENKINS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pipelines_jobs_duration_report",
    method: "list",
    filters: {
      across: "trend"
    },
    composite: true,
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    chart_props: {
      unit: "Minutes",
      chartProps: chartProps
    },
    composite_transform: {
      count: "jenkins_pipeline_jobs_duration_counts"
    },
    supported_filters: jenkinsPipelineJobSupportedFilters,
    xaxis: false,
    convertTo: "mins",
    drilldown: jenkinsPipelineJobDrilldown,
    blockTimeFilterTransformation: params => {
      const { timeFilterName } = params;
      return ["start_time"].includes(timeFilterName);
    },
    onChartClickPayload,
    shouldJsonParseXAxis: () => true,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsRangeChoiceMapping,
    across: ["trends", "job_name", "qualified_job_name", "cicd_job_id", "instance_name"],
    xAxisLabelTransform: xAxisLabelTransform,
    transformFunction: data => cicdTrendReportTransform(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      ...cicdTrendDefaultQuery,
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    valuesToFilters: {
      job_normalized_full_name: "job_normalized_full_names"
    },
    [API_BASED_FILTER]: ["cicd_job_ids"],
    [FIELD_KEY_FOR_FILTERS]: jenkinsApiBasedFilterKeyMapping,
    key_for_filter_value: { cicd_job_ids: "key" },
    [REPORT_FILTERS_CONFIG]: getFilterConfigCicdPipelineJobDurationTrends,
    [HIDE_CUSTOM_FIELDS]: true,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    [CSV_DRILLDOWN_TRANSFORMER]: cicdCsvDrilldownDataTransformer
  },
  cicd_jobs_count_report: {
    name: "CICD Job Count Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_count_report",
    defaultAcross: "cicd_user_id",
    method: "list",
    filters: {},
    default_query: {
      end_time: {
        $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    appendAcrossOptions: [
      { label: "Instance Name", value: "instance_name" },
      { label: "Qualified Name", value: "job_normalized_full_name" },
      { label: "Project", value: "project_name" },
      { label: "Triage Rule", value: "triage_rule" }
    ],
    stack_filters: [
      "trend",
      "job_name",
      "cicd_user_id",
      "job_status",
      "qualified_job_name",
      "instance_name",
      "project_name",
      "triage_rule"
    ],
    chart_props: {
      unit: "Count",
      barProps: [
        {
          name: "Total number of jobs run",
          dataKey: "count"
        }
      ],
      chartProps: chartProps,
      stacked: false
    },
    supported_filters: {
      ...jenkinsGithubJobSupportedFilters,
      values: [...jenkinsGithubJobSupportedFilters.values, "triage_rule"]
    },
    supportExcludeFilters: true,
    xaxis: true,
    drilldown: jenkinsGithubJobRunDrilldown,
    across: ["cicd_user_id", "job_name", "job_status"],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsGithubRangeChoiceMapping,
    transformFunction: data => cicdSCMJobCountTransformer(data),
    valuesToFilters: {
      qualified_job_name: "qualified_job_names",
      job_normalized_full_name: "job_normalized_full_names"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [VALUE_SORT_KEY]: "count",
    [CHART_DATA_TRANSFORMERS]: {
      [CHART_TOOLTIP_RENDER_TRANSFORM]: cicdJobCountReportTooltipTransformer
    },
    [PREV_REPORT_TRANSFORMER]: data => transformCICDJobReport(data),
    [REPORT_FILTERS_CONFIG]: getFilterConfigCicdJobCount,
    [HIDE_CUSTOM_FIELDS]: true,
    acrossFilterLabelMapping: scmCicdFilterOptionsMapping,
    stackFilterLabelMapping: scmCicdFilterOptionsMapping,
    [CSV_DRILLDOWN_TRANSFORMER]: cicdCsvDrilldownDataTransformer
  },
  cicd_pipeline_jobs_count_report: {
    name: "CICD Pipeline Jobs Count Report",
    application: IntegrationTypes.JENKINS,
    chart_type: ChartType?.TREEMAP,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pipelines_jobs_count_report",
    method: "list",
    filters: {},
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    appendAcrossOptions: [
      { label: "Instance Name", value: "instance_name" },
      { label: "Jenkins Job Path", value: "job_normalized_full_name" }
    ],
    // stack_filters: ["trend", "job_name", "cicd_job_id", "cicd_user_id", "qualified_job_name", "job_status"],
    supported_filters: jenkinsPipelineJobSupportedFilters,
    xaxis: true,
    drilldown: {
      application: "jenkins_job_count_treemap"
    },
    across: ["trends", "job_name", "qualified_job_name", "cicd_job_id", "instance_name"],
    defaultAcross: "cicd_job_id",
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [VALUE_SORT_KEY]: "count",
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsRangeChoiceMapping,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    transformFunction: data => jenkinsPipelineJobsCountTransformer(data),
    [API_BASED_FILTER]: ["cicd_job_ids"],
    [FIELD_KEY_FOR_FILTERS]: jenkinsApiBasedFilterKeyMapping,
    key_for_filter_value: { cicd_job_ids: "key" },
    [REPORT_FILTERS_CONFIG]: JenkinsCICDPipelineJobsCountReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  cicd_pipeline_jobs_count_trend_report: {
    name: "CICD Pipeline Jobs Count Trend Report",
    application: IntegrationTypes.JENKINS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pipelines_jobs_count_report",
    method: "list",
    filters: {
      across: "trend"
    },
    composite: true,
    chart_props: {
      unit: "Count",
      chartProps: chartProps
    },
    composite_transform: {
      count: "jenkins_pipeline_job_counts"
    },
    [PARTIAL_FILTER_MAPPING_KEY]: jenkinsPartialFilterKeyMapping,
    supported_filters: jenkinsPipelineJobSupportedFilters,
    xaxis: false,
    across: ["trends", "job_name", "qualified_job_name", "cicd_job_id", "instance_name"],
    drilldown: jenkinsPipelineDrilldown,
    blockTimeFilterTransformation: params => {
      const { timeFilterName } = params;
      return ["start_time"].includes(timeFilterName);
    },
    onChartClickPayload,
    shouldJsonParseXAxis: () => true,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsRangeChoiceMapping,
    xAxisLabelTransform: xAxisLabelTransform,
    transformFunction: data => cicdTrendReportTransform(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsRangeChoiceMapping,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      ...cicdTrendDefaultQuery,
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    valuesToFilters: {
      trend: "start_time",
      job_normalized_full_name: "job_normalized_full_names"
    },
    [API_BASED_FILTER]: ["cicd_job_ids"],
    [FIELD_KEY_FOR_FILTERS]: jenkinsApiBasedFilterKeyMapping,
    key_for_filter_value: { cicd_job_ids: "key" },
    [REPORT_FILTERS_CONFIG]: getFilterConfigCicdPipelineJobsCountTrends,
    [HIDE_CUSTOM_FIELDS]: true,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    [CSV_DRILLDOWN_TRANSFORMER]: cicdCsvDrilldownDataTransformer
  },
  jobs_count_trends_report: {
    name: "CICD Job Count Trend Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_count_report",
    method: "list",
    filters: {
      across: "trend"
    },
    composite: true,
    chart_props: {
      unit: "Count",
      chartProps: chartProps
    },
    composite_transform: {
      count: "cicd_jobs_count"
    },
    supported_filters: jenkinsGithubJobSupportedFilters,
    supportExcludeFilters: true,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    xaxis: false,
    onChartClickPayload,
    shouldJsonParseXAxis: () => true,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsGithubRangeChoiceMapping,
    drilldown: { ...jenkinsGithubJobRunDrilldown, drilldownTransformFunction: jenkinsDrilldownTransformer },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      ...cicdTrendDefaultQuery,
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    xAxisLabelTransform: xAxisLabelTransform,
    transformFunction: data => cicdJobCountTransformer(data),
    valuesToFilters: {
      trend: "job_started_at",
      job_normalized_full_name: "job_normalized_full_names"
    },
    [REPORT_FILTERS_CONFIG]: JenkinsJobsCountTrendsReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jobs_commit_leads_trends_report: {
    name: "SCM Commit to CICD Job Lead Time Trend Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_commits_lead_report",
    method: "list",
    default_query: scmCicdDefaultQuery,
    filters: {
      across: "job_end"
    },
    chart_props: {
      barProps: [
        {
          name: "Median Initial Commit to Deployment time",
          dataKey: "median"
        },
        {
          name: "Number of commits deployed",
          dataKey: "count"
        }
      ],
      stacked: false,
      unit: "Minutes",
      chartProps: chartProps
    },
    supported_filters: scmCicdSupportedFilters,
    supportExcludeFilters: true,
    composite: true,
    convertTo: "mins",
    drilldown: scmCicdDrilldown,
    xAxisLabelTransform: xAxisLabelTransform,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsGithubRangeChoiceMapping,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    transformFunction: data => scmCicdTrendTransformer(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: JenkinsJobsCommitLeadsTrendsReportConfig
  },
  jobs_durations_trends_report: {
    name: "CICD Job Duration Trend Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_duration_report",
    method: "list",
    default_query: scmCicdDefaultQuery,
    filters: {
      across: "job_end"
    },
    chart_props: {
      unit: "Minutes",
      chartProps: chartProps
    },
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    supported_filters: jenkinsGithubJobSupportedFilters,
    supportExcludeFilters: true,
    composite: true,
    composite_transform: {
      min: "cicd_jobs_duration_min",
      median: "cicd_jobs_duration_median",
      max: "cicd_jobs_duration_max"
    },
    convertTo: "mins",
    drilldown: jenkinsGithubJobRunDrilldown,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsGithubRangeChoiceMapping,
    xAxisLabelTransform: xAxisLabelTransform,
    transformFunction: data => scmCicdTrendTransformer(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: JenkinsJobsDurationTrendsReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  jobs_change_volumes_trends_report: {
    name: "SCM Change Volume to CICD Job Trend Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jobs_change_volume_report",
    method: "list",
    default_query: scmCicdDefaultQuery,
    filters: {
      across: "job_end"
    },
    composite: true,
    chart_props: {
      areaProps: [
        {
          name: "lines_added_count",
          dataKey: "lines_added_count",
          transformer: volumeChangeTransformer
        },
        {
          name: "files_changed_count",
          dataKey: "files_changed_count"
        },
        {
          name: "lines_removed_count",
          dataKey: "lines_removed_count",
          transformer: volumeChangeTransformer
        }
      ],
      unit: "Lines of Code",
      //sortBy: "median",
      chartProps: chartProps
    },
    supported_filters: scmCicdSupportedFilters,
    supportExcludeFilters: true,
    shouldSliceFromEnd: shouldSliceFromEnd,
    xAxisLabelTransform: xAxisLabelTransform,
    drilldown: scmCicdDrilldown,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jenkinsGithubRangeChoiceMapping,
    [FILTER_NAME_MAPPING]: scmCicdFilterOptionsMapping,
    transformFunction: data => jobsChangeVolumeTransform(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: JenkinsChangeVolumeTrendsReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  job_runs_test_report: {
    name: "Junit Test Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_click_enable: true,
    uri: "jobs_run_tests_report",
    method: "list",
    composite: false,
    chart_props: {
      barProps: [
        {
          name: "total_tests",
          dataKey: "total_tests"
        }
      ],
      stacked: false,
      unit: "Tests",
      chartProps: chartProps
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [VALUE_SORT_KEY]: "count",
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    supported_filters: junitSupportedFilters,
    xaxis: true,
    defaultAcross: "test_suite",
    across: ["job_status", "job_name", "cicd_user_id", "test_status", "test_suite"],
    drilldown: junitTestDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => seriesDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: JenkinsJobRunsTestReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  job_runs_test_trend_report: {
    name: "Junit Test Trend Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_click_enable: true,
    uri: "jobs_run_tests_report",
    method: "list",
    filters: {
      across: "trend"
    },
    composite: false,
    chart_props: {
      barProps: [
        {
          name: "total_tests",
          dataKey: "total_tests"
        }
      ],
      stacked: false,
      unit: "Tests",
      chartProps: chartProps
    },
    supported_filters: junitSupportedFilters,
    xaxis: false,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    drilldown: junitTestDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: JenkinsJobRunsTestTrendReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  job_runs_test_duration_report: {
    name: "Junit Duration Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_click_enable: true,
    uri: "jobs_run_tests_duration_report",
    method: "list",
    composite: false,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [VALUE_SORT_KEY]: "duration",
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: true,
      unit: "Minutes",
      chartProps: chartProps
    },
    supported_filters: junitSupportedFilters,
    xaxis: true,
    defaultAcross: "job_name",
    across: ["job_status", "job_name", "cicd_user_id", "test_status", "test_suite"],
    drilldown: junitTestDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: JenkinsJobRunsTestDurationReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  job_runs_test_duration_trend_report: {
    name: "Junit Duration Trend Report",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_click_enable: true,
    uri: "jobs_run_tests_duration_report",
    method: "list",
    composite: false,
    filters: {
      across: "trend"
    },
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      unit: "Minutes",
      chartProps: chartProps
    },
    supported_filters: junitSupportedFilters,
    xaxis: false,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    drilldown: junitTestDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: JenkinsJobRunsTestDurationTrendsReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  code_volume_vs_deployment_report: {
    name: "Code Volume Vs Deployment",
    application: IntegrationTypes.JENKINSGITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_click_enable: true,
    uri: "code_vol_vs_deployment",
    method: "list",
    composite: false,
    chart_props: {
      barProps: [
        {
          name: "Number of deployments",
          dataKey: "number_of_deployment"
        },
        {
          name: "Volume of code changes",
          dataKey: "volume_of_code_change"
        }
      ],
      stacked: false,
      unit: "Line Count",
      chartProps: chartProps
    },
    supported_filters: CodeVolVsDeployemntSupportedFilters,
    xaxis: false,
    defaultAcross: "job_end",
    drilldown: junitTestDrilldown,
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    transformFunction: data => CodeVolVsDeployemntTransformer(data),
    [FE_BASED_FILTERS]: {
      job_end_date,
      interval,
      show_value_on_bar
    },
    // used to show red asterick in required supported filters
    requiredSupportedFilter: {
      build_job_name: true,
      deploy_job_name: true
    },
    default_query: codeVolVsDeploymentDefaultQuery,
    drilldown: jenkinsCodeVsDeploymentDrilldown,
    // when filter need to send in some parent key instead in the filters use GET_PARENT_AND_TYPE_KEY and FILTER_PARENT_AND_VALUE_KEY method
    [GET_PARENT_AND_TYPE_KEY]: type => {
      const parentKey = type.includes("deploy_") ? "deploy_job" : "build_job";
      const _type = type.substring(type.indexOf("_") + 1, type.length);
      return { parentKey, _type };
    },
    [FILTER_PARENT_AND_VALUE_KEY]: item => {
      const parentKey =
        Object.keys(item)[0].substring(0, Object.keys(item)[0].indexOf("_")) === "build" ? "build_job" : "deploy_job";
      const valueKey = Object.keys(item)[0].substring(
        Object.keys(item)[0].indexOf("_") + 1,
        Object.keys(item)[0].length
      );
      return { parentKey, valueKey };
    },
    [WIDGET_VALIDATION_FUNCTION]: payload => {
      const { query } = payload;
      const deployObject = get(query, "deploy_job", {});
      const buildObject = get(query, "build_job", {});
      const deploy_job_names = get(deployObject, ["job_names"], []);
      const build_job_names = get(buildObject, ["job_names"], []);
      const build_job_normalized_full_names = get(buildObject, ["job_normalized_full_names"], []);
      const deploy_job_normalized_full_names = get(deployObject, ["job_normalized_full_names"], []);
      return (
        (deploy_job_names.length || deploy_job_normalized_full_names.length) &&
        (build_job_names.length || build_job_normalized_full_names.length)
      );
    },
    [SHOW_AGGREGATIONS_TAB]: false,
    [WIDGET_FILTER_PREVIEW_COUNT]: filters => {
      let count = 0;
      const buildJobFilters = get(filters, ["build_job"], {});
      const deployJobFilters = get(filters, ["deploy_job"], {});
      const otherKeys = Object.keys(filters).filter(key => !["build_job", "deploy_job", "across"].includes(key));
      count = count + Object.keys(buildJobFilters).filter(key => hasValue(buildJobFilters[key])).length;
      count = count + Object.keys(deployJobFilters).filter(key => hasValue(deployJobFilters[key])).length;
      count = count + otherKeys.filter(key => hasValue(filters[key])).length;
      return count + 1; //+1 for metrics always present in metadata
    },
    [METADATA_FILTERS_PREVIEW]: metadata => {
      const metrics = get(metadata, ["metrics"], "line_count");
      const final_filters = [];
      final_filters.push({
        label: "Metrics",
        value: toTitleCase(metrics)
      });
      return final_filters;
    },
    [SUPPORTED_FILTERS_WITH_INFO]: {
      build_job_name: {
        info: "Either Choose BUILD JOB NAME or Build JOB NORMALIZED FULL NAME"
      },
      deploy_job_name: {
        info: "Either Choose DEPLOY JOB NAME or DEPLOY JOB NORMALIZED FULL NAME"
      },
      build_job_normalized_full_name: {
        info: "Either Choose BUILD JOB NAME or BUILD JOB NORMALIZED FULL NAME"
      },
      deploy_job_normalized_full_name: {
        info: "Either Choose DEPLOY JOB NAME or DEPLOY JOB NORMALIZED FULL NAME"
      }
    },
    weekStartsOnMonday: true,
    onChartClickPayload: param => {
      let timeStamp = get(param, ["data", "activePayload", 0, "payload", "key"], undefined);
      const label = get(param, ["data", "activeLabel"], undefined);
      return { id: timeStamp, name: label };
    },
    [INCLUDE_ACROSS_OU_EXCLUSIONS]: true,
    [REPORT_FILTERS_CONFIG]: JenkinsCodeVolVsDeployementReportConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
