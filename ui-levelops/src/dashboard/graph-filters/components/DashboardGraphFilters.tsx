import { Checkbox, Form, Icon, Input, Tooltip } from "antd";
import { CheckboxChangeEvent } from "antd/lib/checkbox";
import * as AppName from "dashboard/constants/applications/names";
import {
  AZURE_LEAD_TIME_ISSUE_REPORT,
  AZURE_SPRINT_REPORTS,
  azureLeadTimeIssueReports,
  azureSprintReports,
  COMBINED_JIRA_SPRINT_REPORT,
  fileTypeFilterReports,
  INFO_MESSAGES,
  ISSUE_MANAGEMENT_REPORTS,
  issueManagementReports,
  JENKINS_REPORTS,
  jenkinsTrendReports,
  JIRA_SPRINT_REPORTS,
  jiraManagementTicketReport,
  jiraSprintMetricOptions,
  LEAD_TIME_ISSUE_REPORT,
  LEAD_TIME_REPORTS,
  leadTimeIssueReports,
  leadTimeReports,
  scmCicdAzureReports,
  scmEnhancedReports,
  STACKS_FILTER_STATUS,
  supportReports,
  JIRA_MANAGEMENT_TICKET_REPORT,
  HIDE_ISSUE_MANAGEMENT_SYSTEM_DROPDOWN,
  JIRA_SPRINT_DISTRIBUTION_REPORTS
} from "dashboard/constants/applications/names";
import {
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_FE_BASED_FILETRS_WITH_STATUS_OPTIONS,
  DefaultKeyTypes,
  DISABLE_XAXIS,
  extraReportWithTicketCategorizationFilter,
  IGNORE_SUPPORTED_FILTERS_KEYS,
  KEYS_TO_UNSET_WHEN_BA_WITH_COMMIT_COUNT,
  MAX_RECORDS_LABEL,
  MAX_RECORDS_OPTIONS_KEY,
  SHOW_MAX_RECORDS_INSIDE_TAB,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import {
  allBAReports,
  EffortUnitType,
  jiraAzureBADynamicSupportedFiltersReports,
  jiraBADynamicSupportedFiltersReports,
  jiraBAReportTypes
} from "dashboard/constants/enums/jira-ba-reports.enum";
import { JiraReports, JiraStatReports } from "dashboard/constants/enums/jira-reports.enum";
import { scmTableReportType } from "dashboard/constants/enums/scm-reports.enum";
import { STACKS_SHOW_TAB } from "dashboard/constants/filter-key.mapping";
import { GET_PARENT_AND_TYPE_KEY } from "dashboard/constants/filter-name.mapping";
import { FileReports, ReportsApplicationType, ZendeskStacksReportsKey } from "dashboard/constants/helper";
import { FEBasedFilterMap } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { useFilterClassnames } from "dashboard/graph-filters/components/utils/useFilterClassnames";
import { allowWidgetDataSorting } from "dashboard/helpers/helper";
import { capitalize, cloneDeep, debounce, forEach, get, map, uniq, unset, upperCase, values } from "lodash";
import React, { useCallback, useContext, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { acrossIsAzureIteration } from "shared-resources/containers/widget-api-wrapper/helper";
import { unsetKeysFromObject } from "utils/commonUtils";
import { v1 as uuid } from "uuid";
import {
  AgnosticTimeRangeFilter,
  APIFilters,
  CustomTimeRangeFilters,
  IdealRangeFilter,
  JenkinsGithubParameters,
  JenkinsJobTimeFilter,
  JiraExcludeStatusFilter,
  JiraHygieneFilters,
  JiraIssueTimeFilters,
  LeadTimeFilters,
  ModifiedApiFilters,
  NccGroupFilters,
  PraetorianFilters,
  SprintDoneStatusSelect,
  StatFilter,
  StatSprintMetricFilter,
  StatTimeRangeFilters,
  SynkFilters,
  VelocityConfigFilter,
  ZendeskCreatedInFilter
} from ".";
import {
  getAcrossValue,
  getFilterValue,
  getGroupByRootFolderKey,
  getMetadataValue,
  isExcludeVal
} from "../../../configurable-dashboard/helpers/helper";
import { WIDGET_CONFIGURATION_KEYS } from "../../../constants/widgets";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  AntCheckboxGroup,
  AntInput,
  AntSelect,
  AntText,
  AntTooltip,
  CustomFormItemLabel,
  CustomSelect,
  InputRangeFilter
} from "../../../shared-resources/components";
import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import {
  ADDITIONAL_KEY_FILTERS,
  bullseyeJobReports,
  CUSTOM_FIELD_PREFIX,
  CUSTOM_STACK_FILTER_NAME_MAPPING,
  jenkinsJobReports,
  TESTRAILS_CUSTOM_FIELD_PREFIX,
  valuesToFilters
} from "../../constants/constants";
import widgetConstants from "../../constants/widgetConstants";
import {
  BAHygieneTypes,
  hygieneTypes,
  scmIssueHygieneTypes,
  zendeskSalesForceHygieneTypes
} from "../../constants/hygiene.constants";
import { SCMReworkVisualizationTypes, SCMVisualizationTypes } from "../../constants/typeConstants";
import { WidgetTabsContext } from "../../pages/context";
import FEBasedFiltersContainer from "../containers/FEBasedFilters/FEBasedFilters.container";
import AzureTeamsFilter from "./AzureTeamsFilter";
import BullseyeMetricFilter from "./bullseye-filters/BullseyeMetricFilter";
import {
  APPLY_OU_ON_VELOCITY_OPTIONS,
  azureIntervalReport,
  backlogTrendReportOptions,
  cicdIntervalOptions,
  codeVolVsDeploymentMetricsOptions,
  defaultMaxEntriesOptions,
  ITEM_TEST_ID,
  jiraResolutionTimeMetricsOptions,
  jiraTimeAcrossStagesMetricOptions,
  lastFileUpdateIntervalOptions,
  leadTimeMetricOptions,
  LEGACY_CODE_INFO,
  MODIFIED_API_FILTERS_REPORT,
  scmCodeChangeOptions,
  scmCodingMetricsOptions,
  scmCodingSingleStatMetricsOptions,
  scmCommentDensityOptions,
  scmOtherCriteriaOptions,
  scmPRsResponseTimeMetricsOptions,
  scmResolutionTimeMetricsOptions,
  scmReworkVisualizationOptions,
  scmVisualizationOptions,
  sonarQubeCodeComplexityWigets,
  sonarQubeMetricsOptions,
  sprintImpactIntervalOptions,
  sprintVisualizationOptions,
  ticketReportMetricOptions
} from "./Constants";
import { issueManagementSystemOptions, singleStatIssueManagementSystemOptions } from "./DashboardFiltersConstants";
import "./DashboardGraphFilters.style.scss";
import DependencyAnalysisFilter from "./DependencyAnalysisFIlter";
import {
  backlogLeftYAxisOptions,
  backlogRightYAxisOptions,
  jiraTicketSingleStatMetricsOptions,
  leadTimeCalculationOptions,
  scmCommitterMetricOptions,
  scmMetricOptions,
  SPRINT_GRACE_INFO,
  SPRINT_GRACE_OPTIONS,
  SPRINT_GRACE_OPTIONS_REPORTS,
  supportSystemOptions
} from "./helper";
import { ModulePathFilter } from "./index";
import SCMCodeFilters from "./SCMCodeFilters/SCMCodeFilters";
import { getSortedFilterOptions, stringSortingComparator } from "./sort.helper";
import { IDEAL_RANGE_FILTER_KEY, sprintStatReports } from "./sprintFilters.constant";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";
import TimeAcrossStagesFilterComponent from "./TimeAcrossStagesFilterComponent";
import TimeRangeComponent from "./TimeRangeComponent";
import WidgetDataSortFilter from "./WidgetDataSortFilter";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { IssueManagementOptions } from "constants/issueManagementOptions";

interface DashboardGraphFiltersProps {
  data: Array<any>;
  customData?: Array<any>;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  onMaxRecordsSelection?: (value: any) => void;
  onExcludeChange?: (key: string, value: boolean) => void;
  onPartialChange: (key: string, value: any) => void;
  maxRecords?: any;
  acrossOptions?: Array<any>;
  filters: any;
  application: string;
  reportType: string;
  applicationUse: boolean;
  partialFilterError?: any;
  onCustomFilterValueChange?: ((value: any, type?: any) => void) | undefined;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  metaData?: any;
  hasNext?: boolean;
  sprintData?: Array<any>;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  integrationIds: string[];
  fieldTypeList?: { key: string; type: string; name: string }[];
  onModifieldFilterValueChange?: (payload: any) => void;
  isMultiTimeSeriesReport?: boolean;
  isCompositeChild?: boolean;
  dashboardMetaData?: any;
  scmGlobalSettings?: any;
  onSingleStatTypeFilterChange?: (value: string, removeKey: string) => void;
}

const jobNameExcludeTypes = ["cicd_scm_jobs_count_report", "cicd_pipeline_jobs_count_report"];

const DashboardGraphFiltersComponent: React.FC<DashboardGraphFiltersProps> = (props: DashboardGraphFiltersProps) => {
  const {
    data,
    onFilterValueChange,
    onMetadataChange,
    onExcludeChange,
    onPartialChange,
    onTimeRangeTypeChange,
    filters,
    application,
    applicationUse,
    reportType,
    customData,
    onTimeFilterValueChange,
    metaData,
    sprintData,
    handleLastSprintChange,
    fieldTypeList = [],
    onModifieldFilterValueChange,
    isMultiTimeSeriesReport,
    dashboardMetaData,
    scmGlobalSettings,
    onSingleStatTypeFilterChange
  } = props;
  const { isVisibleOnTab } = useContext(WidgetTabsContext);

  const debouncedOnFilterValueChange = useCallback(debounce(onFilterValueChange, 300), [onFilterValueChange]);
  const [activePopKey, setActivePopKey] = useState<string | undefined>();

  const excludeStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "jira_filter_values",
    method: "list",
    uuid: "exclude_status"
  });

  const { innerClassName, outerClassName } = useFilterClassnames({
    activePopKey: !!activePopKey,
    applicationUse: props.applicationUse
  });

  const integrationsList = useSelector(selectedDashboardIntegrations);

  const getWidgetConstant = useCallback(
    (key: any, defaultValue = undefined) => get(widgetConstants, [reportType, key], defaultValue),
    [reportType]
  );

  const isStacksDisabled = useMemo(() => {
    const getStackStatus = getWidgetConstant(STACKS_FILTER_STATUS, undefined);
    if (getStackStatus) {
      return getStackStatus(filters);
    }
  }, [filters]);

  const doneStatusFilter = getWidgetConstant("doneStatusFilter", undefined);

  const stackMessage = useMemo(() => {
    const messages = getWidgetConstant(INFO_MESSAGES, "");
    if (messages && Object.keys(messages).length > 0) {
      return get(messages, "stacks_disabled", "");
    }
  }, [filters]);
  const getHygieneOptions = () => {
    const supportedReports = [
      "github_issues_report",
      "github_issues_report_trends",
      "scm_issues_time_resolution_report",
      "github_issues_count_single_stat",
      "github_issues_first_reponse_report",
      "github_issues_first_response_report_trends",
      "github_issues_first_response_count_single_stat"
    ];
    let types = ["zendesk", "salesforce"].includes(application)
      ? zendeskSalesForceHygieneTypes
      : supportedReports.includes(reportType)
      ? scmIssueHygieneTypes
      : hygieneTypes;

    if (allBAReports.includes(reportType as any)) {
      types = BAHygieneTypes;
    }

    const hygieneOptions = types.map((item: any) => ({
      label: item.replace(/_/g, " "),
      value: item
    }));
    hygieneOptions.sort(stringSortingComparator());
    return hygieneOptions;
  };

  const showOUFilters =
    !!(dashboardMetaData?.ou_ids || []).length &&
    ["jira", "jenkins", "github", "azure_devops", "jenkinsgithub", "githubjira"].includes(application);

  const getSortOptions = () => {
    let sortOptions = getWidgetConstant("sort_options").map((item: any) => ({
      label: capitalize(item.replace(/_/g, " ")),
      value: item
    }));
    sortOptions.sort(stringSortingComparator());
    return sortOptions;
  };

  const getMapSortValue = (value: any) => {
    return [{ id: value, order: "-1" }];
  };

  const getSortFilterValue = () => {
    return filters["sort"] ? filters["sort"][0].id : "";
  };

  const shouldFilterAcrossValue = useMemo(() => {
    const across = getAcrossValue(filters, reportType);

    return Object.keys(filters || {}).includes(get(valuesToFilters, [across], across));
  }, [filters, reportType]);

  const getStacksOptions = () => {
    let filterJobName = false;
    let filterAcross = false;
    if (jobNameExcludeTypes.includes(reportType) && filters.across === "qualified_job_name") {
      filterJobName = true;
    }
    if (
      [
        "praetorian_issues_report",
        "ncc_group_vulnerability_report",
        "snyk_vulnerability_report",
        AppName.MICROSOFT_ISSUES_REPORT_NAME
      ].includes(reportType)
    ) {
      filterAcross = true;
    }

    let widgetStackOptions = ((widgetConstants as any)[reportType]?.["stack_filters"] || [])
      .filter((f: string) => {
        let valid_across = true;
        let valid_job_name = true;

        if (filterAcross) {
          valid_across = f !== filters.across;
        }

        if (filterJobName) {
          valid_job_name = !["job_name", "qualified_job_name"].includes(f);
        }

        const keep = valid_across && valid_job_name;
        return keep;
      })
      .map((item: any) => {
        if (application === IntegrationTypes.JIRA || scmCicdAzureReports.includes(reportType as any)) {
          const mapping = getWidgetConstant("filterOptionMap");
          return {
            label: get(mapping, [item], item),
            value: item
          };
        }
        return {
          label: capitalize(item.replace(/_/g, " ")),
          value: item
        };
      });

    const customStackOptions = (customData || []).map((field: any) => {
      return {
        label: capitalize((field?.name).replace(/_/g, " ")),
        value: field?.key
      };
    });

    if (
      (application === ReportsApplicationType.ZENDESK &&
        getWidgetConstant(ZendeskStacksReportsKey.ZENDESK_STACKED_KEY)) ||
      [ReportsApplicationType.JIRA, ReportsApplicationType.AZURE_DEVOPS].includes(application as ReportsApplicationType)
    ) {
      widgetStackOptions = [...widgetStackOptions, ...customStackOptions];
    }

    (widgetStackOptions || []).sort(stringSortingComparator());
    return widgetStackOptions;
  };

  const getJiraTimeAcrossStagesOptions = useMemo(() => {
    const filterKey = leadTimeReports.includes(reportType as any) ? "jira_status" : "status";
    const jiraStatuses = data ? data.filter((item: any) => Object.keys(item)[0] === filterKey)[0] : [];

    return jiraStatuses?.[filterKey] || [];
  }, [data, reportType]);

  const getJiraResolutionTimeAcrossStagesOptions = useMemo(() => {
    const excludeStatus = get(excludeStatusState, ["data", "records", 0, "status"], []).map((item: any) => item.key);
    const statusObject = data
      ? data.filter(
          (item: { [filterType: string]: { [key: string]: string }[] }) => Object.keys(item)[0] === "status"
        )[0]
      : [];
    if (statusObject && Object.keys(statusObject).length > 0) {
      let list = statusObject.status;
      if (list) {
        list = list.filter((item: { [key: string]: string }) => !excludeStatus.includes(item.key));
      } else {
        list = [];
      }
      return list;
    }
    return [];
  }, [data, reportType, excludeStatusState]);

  const getAzureTimeAcrossStagesOptions = useMemo(() => {
    const filterKey = "workitem_status";
    const jiraStatuses = data ? data.filter((item: any) => Object.keys(item)[0] === filterKey)[0] : [];
    return jiraStatuses?.[filterKey] || [];
  }, [data, reportType]);

  const getScmTimeAcrossStagesOptions = useMemo(() => {
    if (!data || !data.length) {
      return [];
    }
    let newData = data.filter((item: any) => Object.keys(item)[0] === "column")?.[0] || {};
    newData = (newData?.["column"] || []).map((item: any) => ({ key: item.key, label: item.key, value: item.key }));
    return newData;
  }, [data]);

  const getRightFiltersData = () => {
    return data ? data.filter(item => Object.keys(item)[0] === "label") : [];
  };

  const getRestFiltersData = () => {
    let filterKeys = ["label"];
    const ignoredFilterKeys: string[] = getWidgetConstant(IGNORE_SUPPORTED_FILTERS_KEYS, []);
    filterKeys = [...filterKeys, ...ignoredFilterKeys];

    let filteredData = cloneDeep(data ? data.filter(item => !filterKeys.includes(Object.keys(item || {})[0])) : []);

    filteredData = filteredData.map(item => {
      let key = Object.keys(item)[0];
      let _item = item[key];
      if (Array.isArray(_item)) {
        _item = _item.map(options => {
          if (options.hasOwnProperty("cicd_job_id") && key !== "job_normalized_full_name") {
            return {
              key: options["cicd_job_id"],
              value: options["key"]
            };
          } else if (options.hasOwnProperty("additional_key") && ADDITIONAL_KEY_FILTERS.includes(key)) {
            return {
              key: options["key"],
              value: options["additional_key"]
            };
          }
          if (key === "file_type" && fileTypeFilterReports.includes(reportType as any)) {
            return {
              key: options["key"],
              value: upperCase(options["key"]) === "NA" ? "N/A" : upperCase(options["key"])
            };
          }
          if (
            [...issueManagementReports, ...azureLeadTimeIssueReports].includes(reportType as any) &&
            key === "workitem_priority"
          ) {
            const _key = options["key"];
            return {
              value: staticPriorties.hasOwnProperty(_key) ? (staticPriorties as any)[_key] : _key,
              key: options["additional_key"] || _key
            };
          }
          return options;
        });
      }
      if (key === "column") {
        key = "current_column";
      }
      item[key] = _item;
      return item;
    });

    if (["scm_jira_files_report"].includes(reportType)) {
      filteredData = filteredData.map((item: any) => {
        let key = Object.keys(item || {})[0];
        if (key.includes("repo_id")) {
          key = "scm_file_repo_id";
        }
        if (key.includes("issue_type")) {
          key = "jira_issue_type";
        }
        return { [key]: item[Object.keys(item || {})[0]] };
      });
    }

    if ([...jenkinsJobReports].includes(reportType)) {
      filteredData = filteredData.map((item: any) => {
        let key = Object.keys(item || {})[0];
        if (key === "job_normalized_full_name") {
          key = "jenkins_job_path";
        }
        return { [key]: item[Object.keys(item || {})[0]] };
      });
    }

    if (bullseyeJobReports.includes(reportType)) {
      filteredData = filteredData.map((item: any) => {
        let key = Object.keys(item || {})[0];
        if (key === "job_normalized_full_name") {
          key = "jenkins_job_path";
        }
        return { [key]: item[Object.keys(item || {})[0]] };
      });
    }

    if (["scm_issues_time_across_stages_report"].includes(reportType)) {
      filteredData = filteredData.map((item: any) => {
        let key = Object.keys(item || {})[0];
        if (key === "column") {
          key = "current_column";
        }
        return { [key]: item[Object.keys(item || {})[0]] };
      });
    }

    if (AppName.coverityReports.includes(reportType as any)) {
      filteredData = filteredData.map((item: any) => {
        let key = Object.keys(item || {})[0];
        let options: any = Object.values(item)[0];
        if (["first_detected_stream", "last_detected_stream"].includes(key)) {
          return { [key]: options.map((option: any) => ({ ...option, key: option.additional_key })) };
        }
        return item;
      });
    }

    return filteredData;
  };

  const onSwitchValueChange = useCallback((key: string, value: boolean) => onExcludeChange?.(key, value), [filters]);

  const handleStackFilterChange = useCallback(
    (value: string) => {
      let extraMetaData: any = {};
      if ((value || "").includes(CUSTOM_FIELD_PREFIX) || (value || "").includes(TESTRAILS_CUSTOM_FIELD_PREFIX)) {
        const customData = props.customData;
        const mappedStackCustomField = (customData || []).find((fields: any) => fields?.key === (value || ""));
        if (mappedStackCustomField) {
          extraMetaData = {
            [CUSTOM_STACK_FILTER_NAME_MAPPING]: mappedStackCustomField.name || ""
          };
        }
      } else {
        extraMetaData = {
          [CUSTOM_STACK_FILTER_NAME_MAPPING]: ""
        };
      }
      onFilterValueChange(value ? [value] : undefined, "stacks", undefined, extraMetaData);
    },
    [props.customData, onFilterValueChange]
  );

  const supportExcludeFilters = useMemo(
    () => get(widgetConstants, [reportType, "supportExcludeFilters"], false),
    [reportType]
  );

  const supportPartialStringFilters = useMemo(
    () => get(widgetConstants, [reportType, "supportPartialStringFilters"], false),
    [reportType]
  );

  const onFilterChange = (value: any, type?: any, exclude?: boolean) => {
    if (reportType === "scm_jira_files_report" && ["issue_type"].includes(type)) {
      return onFilterValueChange(value, `jira_${type}`, exclude);
    }
    if (reportType === "scm_jira_files_report" && ["repo_id"].includes(type)) {
      return onFilterValueChange(value, `scm_file_${type}`, exclude);
    }

    if (
      ["resolution_time_report", ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(reportType) &&
      type === "metric"
    ) {
      return onFilterValueChange(
        value.length ? value : ["median_resolution_time", "number_of_tickets_closed"],
        type,
        exclude
      );
    }
    if (reportType === "scm_issues_time_resolution_report" && type === "metric") {
      return onFilterValueChange(
        value.length ? value : ["median_resolution_time", "number_of_tickets_closed"],
        type,
        exclude
      );
    }
    return onFilterValueChange(value, type, exclude);
  };

  const _modifiedFilterValueChange = (value: any, type?: any, exclude?: boolean) => {
    const getParentAndTypeKey = getWidgetConstant(GET_PARENT_AND_TYPE_KEY, undefined);
    const { parentKey, _type } = getParentAndTypeKey(type);
    const payload = {
      parentKey: parentKey,
      value: value,
      type: _type
    };
    return onModifieldFilterValueChange?.(payload);
  };

  const stackFilterShowTab = getWidgetConstant(STACKS_SHOW_TAB) || WIDGET_CONFIGURATION_KEYS.METRICS;

  const getSprintDoneStatus = useMemo(() => {
    const index = props.data.findIndex(item => Object.keys(item)[0] === "status");
    return index != -1 ? props.data[index]["status"] : [];
  }, [data]);

  const getAPIFilters = () => {
    if (reportType === "scm_jira_files_report" && filters) {
      const filtersData = Object.keys(filters).reduce((acc: any, next: any) => {
        if (next === "repo_ids") {
          delete filters[next];
          return { ...acc };
        }
        return { ...acc, [next]: filters[next] };
      }, {});
      return filtersData;
    }

    return filters;
  };

  const baURIUnit = get(
    filters,
    [TICKET_CATEGORIZATION_UNIT_FILTER_KEY],
    getWidgetConstant(reportType, DefaultKeyTypes.DEFAULT_EFFORT_UNIT)
  );

  const isBAWithCommitCount =
    jiraAzureBADynamicSupportedFiltersReports.includes(reportType as any) &&
    baURIUnit &&
    [EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(baURIUnit);

  const isJiraBAWithoutCommitCount =
    jiraBADynamicSupportedFiltersReports.includes(reportType as any) &&
    !!baURIUnit &&
    ![EffortUnitType.COMMIT_COUNT].includes(baURIUnit);

  const transformCustomData = useMemo(() => {
    return map(customData, (item: any) => {
      const dataKey = `${item.key}@${item.name}`;
      return {
        [dataKey]: item.values || []
      };
    });
  }, [customData]);

  const filterFEBasedFilters = useCallback(
    (filtersConfig: FEBasedFilterMap) => {
      if (isMultiTimeSeriesReport) {
        unset(filtersConfig, "visualization");
        return filtersConfig;
      }

      if (isBAWithCommitCount) {
        let nFiltersConfig = cloneDeep(filtersConfig);
        nFiltersConfig = unsetKeysFromObject(KEYS_TO_UNSET_WHEN_BA_WITH_COMMIT_COUNT, nFiltersConfig);
        return nFiltersConfig;
      }

      if (isJiraBAWithoutCommitCount) {
        let nFiltersConfig = cloneDeep(filtersConfig);
        const baAttributionSelected = get(filters, [BA_EFFORT_ATTRIBUTION_BE_KEY], undefined);
        const dontShowFilterAssigneeByStatus = baAttributionSelected !== "current_and_previous_assignees";
        const dontShowInProgressStatusFilter = baURIUnit !== EffortUnitType.TICKET_TIME_SPENT;

        const statusFilter: { [x: string]: { key: string }[] } = (data ?? []).find(
          item => Object.keys(item ?? { key: [] })[0] === "status"
        );
        const statusFilterValues: { key: string }[] = Object.values(statusFilter ?? { key: [] })[0];
        if (statusFilterValues.length) {
          let statusFilterOptions = statusFilterValues.map(value => ({ label: value?.key, value: value?.key }));
          forEach(BA_FE_BASED_FILETRS_WITH_STATUS_OPTIONS, key => {
            nFiltersConfig = {
              ...(nFiltersConfig ?? {}),
              [key]: {
                ...get(nFiltersConfig, [key], {}),
                options: statusFilterOptions
              }
            } as FEBasedFilterMap;
          });
        }

        if (dontShowFilterAssigneeByStatus) {
          unset(nFiltersConfig, "filterAssigneeByStatusFilter");
        }

        if (dontShowInProgressStatusFilter) {
          unset(nFiltersConfig, "baInProgressStatusFilter");
        }

        return nFiltersConfig;
      }

      return filtersConfig;
    },
    [isMultiTimeSeriesReport, isBAWithCommitCount, isJiraBAWithoutCommitCount, filters, data, baURIUnit]
  );

  const dynamicSCMMetricOptions = useMemo(() => {
    const applications = uniq(map(integrationsList, (rec: any) => rec?.application));
    const allOptions = [...scmMetricOptions, { value: "num_workitems", label: "Number of Workitems" }];
    if (applications.includes(IssueManagementOptions.JIRA) && applications.includes(IssueManagementOptions.AZURE)) {
      return allOptions;
    }
    if (applications.includes(IssueManagementOptions.JIRA)) {
      return allOptions.filter((item: { label: string; value: string }) => item.value !== "num_workitems");
    }
    if (applications.includes(IssueManagementOptions.AZURE)) {
      return allOptions.filter((item: { label: string; value: string }) => item.value !== "num_jira_issues");
    }
    return allOptions.filter(
      (item: { label: string; value: string }) => !["num_jira_issues", "num_workitems"].includes(item.value)
    );
  }, [integrationsList]);

  const renderSettings = () => {
    if (!isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS)) {
      return null;
    }
    return (
      <>
        {JiraReports.JIRA_TIME_ACROSS_STAGES === reportType && (
          <JiraExcludeStatusFilter
            reportType={reportType}
            filters={filters}
            data={getJiraTimeAcrossStagesOptions}
            onFilterValueChange={onFilterValueChange}
          />
        )}
        {ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES === reportType && (
          <JiraExcludeStatusFilter
            reportType={reportType}
            filters={filters}
            data={getAzureTimeAcrossStagesOptions}
            onFilterValueChange={onFilterValueChange}
          />
        )}
      </>
    );
  };
  // Getting default value for issue management system in SETTINGS tab.
  let settingsImsDefaultValue = getMetadataValue(metaData, "default_value", "");
  if (settingsImsDefaultValue) {
    settingsImsDefaultValue = settingsImsDefaultValue.includes("azure_devops")
      ? "azure_devops"
      : reportType === LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT
      ? "githubjira"
      : "jira";
  }

  return (
    <div data-testid="dashboard-graph-filters-component" className={outerClassName}>
      <div className={innerClassName}>
        {(isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          !MODIFIED_API_FILTERS_REPORT.includes(reportType) && (
            <APIFilters
              data={getRestFiltersData()}
              filters={getAPIFilters()}
              supportExcludeFilters={supportExcludeFilters}
              supportPartialStringFilters={supportPartialStringFilters}
              handlePartialValueChange={onPartialChange}
              handleFilterValueChange={onFilterChange}
              handleSwitchValueChange={onSwitchValueChange}
              partialFilterError={props.partialFilterError}
              hasNext={props.hasNext}
              reportType={props.reportType}
              activePopkey={activePopKey}
              handleActivePopkey={key => setActivePopKey(key)}
              handleLastSprintChange={handleLastSprintChange}
            />
          )}
        {(isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          MODIFIED_API_FILTERS_REPORT.includes(reportType) && (
            <ModifiedApiFilters
              data={getRestFiltersData()}
              filters={getAPIFilters()}
              supportExcludeFilters={supportExcludeFilters}
              supportPartialStringFilters={supportPartialStringFilters}
              handlePartialValueChange={onPartialChange}
              handleFilterValueChange={_modifiedFilterValueChange}
              handleSwitchValueChange={onSwitchValueChange}
              partialFilterError={props.partialFilterError}
              hasNext={props.hasNext}
              reportType={props.reportType}
              activePopkey={activePopKey}
              handleActivePopkey={key => setActivePopKey(key)}
              handleLastSprintChange={handleLastSprintChange}
            />
          )}
        {customData && (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
          <APIFilters
            data={transformCustomData}
            filters={filters}
            supportExcludeFilters={supportExcludeFilters}
            supportPartialStringFilters={supportPartialStringFilters}
            handlePartialValueChange={onPartialChange}
            handleFilterValueChange={onFilterChange}
            handleSwitchValueChange={onSwitchValueChange}
            partialFilterError={props.partialFilterError}
            hasNext={props.hasNext}
            reportType={props.reportType}
            isCustom={true}
            activePopkey={activePopKey}
            handleActivePopkey={key => setActivePopKey(key)}
            handleLastSprintChange={handleLastSprintChange}
            fieldTypeList={fieldTypeList}
          />
        )}
        {sprintData &&
          !isBAWithCommitCount &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <APIFilters
              data={sprintData}
              filters={filters}
              supportExcludeFilters={supportExcludeFilters}
              supportPartialStringFilters={supportPartialStringFilters}
              handlePartialValueChange={onPartialChange}
              handleFilterValueChange={onFilterChange}
              handleSwitchValueChange={onSwitchValueChange}
              partialFilterError={props.partialFilterError}
              hasNext={props.hasNext}
              reportType={props.reportType}
              isCustom={true}
              activePopkey={activePopKey}
              handleActivePopkey={key => setActivePopKey(key)}
              handleLastSprintChange={handleLastSprintChange}
            />
          )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
          <CustomTimeRangeFilters
            data={transformCustomData}
            fieldTypeList={fieldTypeList}
            filters={filters}
            metaData={props.metaData}
            onMetadataChange={onMetadataChange}
            dashboardMetaData={dashboardMetaData}
            reportType={reportType}
            onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
            application={application}
            onFilterValueChange={(value: any, type?: any, rangeType?: string, isCustom?: boolean) => {
              onTimeFilterValueChange?.(value, type, rangeType, isCustom);
            }}
          />
        )}
        {(isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          (["jira", "zendesk", "salesforce"].includes(application) ||
            ["github_issues_report", "scm_issues_time_resolution_report"].includes(reportType)) &&
          !["sprint_goal"].includes(reportType) &&
          !isBAWithCommitCount && (
            <Form.Item
              key={`${ITEM_TEST_ID}-hygiene`}
              data-filterselectornamekey={`${ITEM_TEST_ID}-hygiene`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-hygiene`}
              className={"custom-form-item"}
              label={
                <CustomFormItemLabel
                  label={"Hygiene"}
                  withSwitch={{
                    showSwitchText: application === IntegrationTypes.JIRA,
                    showSwitch: application === IntegrationTypes.JIRA,
                    switchValue: isExcludeVal(filters, "hygiene_types"),
                    onSwitchValueChange: value => onSwitchValueChange("hygiene_types", value)
                  }}
                />
              }>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-hygiene_dropdown`}
                showArrow={true}
                value={getFilterValue(filters, "hygiene_types")}
                mode="multiple"
                options={getHygieneOptions()}
                onChange={(value: any, options: any) =>
                  onFilterValueChange(value, "hygiene_types", isExcludeVal(filters, "hygiene_types"))
                }
              />
            </Form.Item>
          )}
        {(isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          [...issueManagementReports].includes(reportType as any) &&
          !isBAWithCommitCount && (
            <Form.Item
              key={`${ITEM_TEST_ID}-workitem-hygiene`}
              data-filterselectornamekey={`${ITEM_TEST_ID}-workitem-hygiene`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-workitem-hygiene`}
              className={"custom-form-item"}
              label={
                <CustomFormItemLabel
                  label={"Workitem Hygiene"}
                  withSwitch={{
                    showSwitchText: application === IntegrationTypes.JIRA,
                    showSwitch: application === IntegrationTypes.JIRA,
                    switchValue: isExcludeVal(filters, "workitem_hygiene_types"),
                    onSwitchValueChange: value => onSwitchValueChange("workitem_hygiene_types", value)
                  }}
                />
              }>
              <AntSelect
                showArrow={true}
                dropdownTestingKey={`${ITEM_TEST_ID}-workitem-hygiene_dropdown`}
                value={getFilterValue(filters, "workitem_hygiene_types")}
                mode="multiple"
                options={getHygieneOptions()}
                onChange={(value: any, options: any) =>
                  onFilterValueChange(value, "workitem_hygiene_types", isExcludeVal(filters, "workitem_hygiene_types"))
                }
              />
            </Form.Item>
          )}
        {reportType === JiraReports.JIRA_TICKETS_REPORT && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
          <DependencyAnalysisFilter
            filters={filters}
            onFilterValueChange={onFilterValueChange}
            onSwitchValueChange={onSwitchValueChange}
          />
        )}
        {[...issueManagementReports, ...azureLeadTimeIssueReports].includes(reportType as any) &&
          !isBAWithCommitCount &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
            <AzureTeamsFilter filters={filters} onFilterValueChange={onFilterValueChange} />
          )}
        {(([
          "jenkins",
          "jenkinsgithub",
          "jira",
          "testrails",
          "sonarqube",
          "praetorian",
          "nccgroup",
          "snyk",
          "azure_devops",
          AppName.MICROSOFT_APPLICATION_NAME
        ].includes(application) &&
          (widgetConstants as any)[reportType]["stack_filters"] &&
          (widgetConstants as any)[reportType]["stack_filters"].length &&
          (!["issue_created", "issue_updated", "issue_resolved"].includes(filters.across) ||
            [JiraReports.JIRA_TICKETS_REPORT].includes(reportType as any))) ||
          (application === ReportsApplicationType.ZENDESK &&
            getWidgetConstant(ZendeskStacksReportsKey.ZENDESK_STACKED_KEY) &&
            !["ticket_created"].includes(filters.across))) &&
          isVisibleOnTab(stackFilterShowTab) && (
            <Form.Item
              key={"common_stacks_filter"}
              label={"Stacks"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-stacks`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-stacks`}>
              <Tooltip title={isStacksDisabled ? stackMessage : ""}>
                <AntSelect
                  disabled={isStacksDisabled}
                  dropdownTestingKey={`${ITEM_TEST_ID}-stacks_dropdown`}
                  showArrow={true}
                  value={getFilterValue(filters, "stacks")}
                  options={getStacksOptions()}
                  // mode={"multiple"}
                  allowClear={true}
                  onChange={handleStackFilterChange}
                />
              </Tooltip>
            </Form.Item>
          )}

        {["jira"].includes(application) &&
          !["sprint_goal"].includes(reportType) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          !isBAWithCommitCount && (
            <>
              <InputRangeFilter
                value={getFilterValue(filters, "story_points")}
                label={"Story Points"}
                onChange={(value: any) => onFilterValueChange(value, "story_points")}
              />
              <InputRangeFilter
                value={getFilterValue(filters, "parent_story_points")}
                label={"Parent Story Points"}
                onChange={(value: any) => onFilterValueChange(value, "parent_story_points")}
              />
            </>
          )}
        {[...issueManagementReports, ...Object.values(AZURE_LEAD_TIME_ISSUE_REPORT)].includes(reportType as any) &&
          !isBAWithCommitCount &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <>
              <InputRangeFilter
                value={getFilterValue(filters, "workitem_story_points")}
                label={"Azure story points"}
                onChange={(value: any) => onFilterValueChange(value, "workitem_story_points")}
              />
              {azureSprintReports.includes(reportType as any) && (
                <InputRangeFilter
                  value={getFilterValue(filters, "workitem_parent_story_points")}
                  label={"Workitem Parent Story Points"}
                  onChange={(value: any) => onFilterValueChange(value, "workitem_parent_story_points")}
                />
              )}
            </>
          )}
        {application === IntegrationTypes.BULLSEYE &&
          ![
            AppName.BULLSEYE_REPORTS.BULLSEYE_CODE_COVERAGE_REPORT,
            AppName.BULLSEYE_REPORTS.BULLSEYE_CODE_COVERAGE_TREND_REPORT
          ].includes(props.reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) || applicationUse) && (
            <BullseyeMetricFilter reportType={reportType} filters={filters} onFilterChange={onFilterValueChange} />
          )}
        {[
          FileReports.JIRA_SALESFORCE_FILES_REPORT,
          FileReports.JIRA_ZENDESK_FILES_REPORT,
          FileReports.SCM_FILES_REPORT,
          FileReports.SCM_JIRA_FILES_REPORT
        ].includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          get(props.metaData, [getGroupByRootFolderKey(reportType)], false) && (
            <ModulePathFilter
              uri={get(widgetConstants, [reportType, "rootFolderURI"], "")}
              value={FileReports.SCM_JIRA_FILES_REPORT === reportType ? filters?.scm_module : filters?.module || ""}
              integrationIds={filters?.integration_ids}
              onChange={value =>
                onFilterValueChange(value, FileReports.SCM_JIRA_FILES_REPORT === reportType ? "scm_module" : "module")
              }
            />
          )}
        {sonarQubeCodeComplexityWigets.includes(reportType) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <InputRangeFilter
              value={getFilterValue(filters, "complexity_score")}
              label={"Complexity Score"}
              onChange={(value: any) => onFilterValueChange(value, "complexity_score")}
            />
          )}
      </div>
      <div>
        {!props.applicationUse &&
          getWidgetConstant("xaxis") &&
          isVisibleOnTab(getWidgetConstant(SHOW_MAX_RECORDS_INSIDE_TAB, WIDGET_CONFIGURATION_KEYS.AGGREGATIONS)) && (
            <>
              {!getWidgetConstant(DISABLE_XAXIS) && (
                <Form.Item
                  label={"X-Axis"}
                  required
                  data-filterselectornamekey={`${ITEM_TEST_ID}-xaxis`}
                  data-filtervaluesnamekey={`${ITEM_TEST_ID}-xaxis`}>
                  <AntSelect
                    dropdownTestingKey={`${ITEM_TEST_ID}-xaxis_dropdown`}
                    options={props.acrossOptions}
                    value={isMultiTimeSeriesReport ? filters?.across : getAcrossValue(filters, reportType)}
                    onSelect={(value: any) => props.onAggregationAcrossSelection?.(value)}
                  />
                </Form.Item>
              )}
              {reportType === "scm_issues_time_across_stages_report" &&
                getAcrossValue(filters, reportType) !== "column" && (
                  <Form.Item label={"Stacks"} className={"custom-universal-filter-item"}>
                    <Checkbox
                      checked={get(filters, ["stacks", "0"], undefined) === "column"}
                      disabled={getAcrossValue(filters, reportType) === "column"}
                      onChange={(e: any) => onFilterValueChange(e.target.checked ? ["column"] : [], "stacks")}>
                      Stack by time in historical status
                    </Checkbox>
                  </Form.Item>
                )}
              {!(getWidgetConstant("show_max") === false) &&
                ![JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(
                  reportType as any
                ) && (
                  <Form.Item
                    label={getWidgetConstant(MAX_RECORDS_LABEL) || "Max X-Axis Entries"}
                    data-filterselectornamekey={`${ITEM_TEST_ID}-max-records`}
                    data-filtervaluesnamekey={`${ITEM_TEST_ID}-max-records`}>
                    <AntSelect
                      dropdownTestingKey={`${ITEM_TEST_ID}-max-records_dropdown`}
                      options={getWidgetConstant(MAX_RECORDS_OPTIONS_KEY) || defaultMaxEntriesOptions}
                      defaultValue={props.maxRecords}
                      onSelect={(value: any) => props.onMaxRecordsSelection?.(value)}
                    />
                  </Form.Item>
                )}
            </>
          )}

        {!props.applicationUse &&
          [JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <>
              <Form.Item
                label={getWidgetConstant(MAX_RECORDS_LABEL) || "Max X-Axis Entries"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-max-xaxis-entries`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-max-xaxis-entries`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-max-xaxis-entries_dropdown`}
                  options={getWidgetConstant(MAX_RECORDS_OPTIONS_KEY) || defaultMaxEntriesOptions}
                  defaultValue={props.maxRecords}
                  onSelect={(value: any) => props.onMaxRecordsSelection?.(value)}
                />
              </Form.Item>
              <Form.Item className={"custom-universal-filter-item"} label={"X-Axis Labels"}>
                <Checkbox
                  checked={get(filters, ["filter_across_values"], true)}
                  disabled={!shouldFilterAcrossValue}
                  onChange={(e: any) => onFilterValueChange(e.target.checked, "filter_across_values")}>
                  Display only filtered values
                </Checkbox>
              </Form.Item>
            </>
          )}
        {[
          FileReports.JIRA_SALESFORCE_FILES_REPORT,
          FileReports.JIRA_ZENDESK_FILES_REPORT,
          FileReports.SCM_FILES_REPORT,
          FileReports.SCM_JIRA_FILES_REPORT
        ].includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <Form.Item>
              <Checkbox
                checked={get(props.metaData, [getGroupByRootFolderKey(reportType)], true)}
                onChange={(e: CheckboxChangeEvent) => onFilterValueChange(e.target.checked, "metadata")}
                className="text-uppercase"
                style={{
                  fontSize: "12px",
                  fontWeight: 700,
                  color: "#575757"
                }}>
                <AntText style={{ marginRight: "4px" }}>{"Group By Modules"}</AntText>
                <AntTooltip title="Root folders are the top most folders in a file system">
                  <Icon type="info-circle" />
                </AntTooltip>
              </Checkbox>
            </Form.Item>
          )}
        {[ChartType.STATS, ChartType.GRAPH_STAT, ChartType.EFFORT_INVESTMENT_STAT].includes(
          getWidgetConstant("chart_type")
        ) &&
          ![
            jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT,
            ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
            LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
            AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT,
            JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT,
            AppName.SCM_DORA_REPORTS.LEAD_TIME_FOR_CHNAGE
          ].includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <>
              <StatFilter
                chartType={getWidgetConstant("chart_type")}
                application={application}
                filters={filters}
                reportType={reportType}
                onFilterValueChange={onFilterValueChange}
              />
            </>
          )}
        {application === IntegrationTypes.GITHUB &&
          reportType === "scm_files_report" &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <Form.Item
              label={"Sort"}
              key={`${ITEM_TEST_ID}-sort`}
              data-filterselectornamekey={`${ITEM_TEST_ID}-sort`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-sort`}>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-sort_dropdown`}
                showArrow={true}
                value={getSortFilterValue()}
                options={getSortOptions()}
                mode={"single"}
                onChange={(value: any, options: any) => onFilterValueChange(getMapSortValue(value), "sort")}
              />
            </Form.Item>
          )}
        {sonarQubeCodeComplexityWigets.includes(reportType) && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
          <Form.Item
            key="sonarqube_code_complexity_metrics"
            label={"Metric"}
            data-filterselectornamekey={`${ITEM_TEST_ID}-code-complexity-metric`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-code-complexity-metric`}>
            <AntSelect
              dropdownTestingKey={`${ITEM_TEST_ID}-code-complexity-metric_dropdown`}
              showArrow={true}
              value={getFilterValue(filters, "metrics")}
              options={sonarQubeMetricsOptions.sort(stringSortingComparator("label"))}
              mode={"single"}
              onChange={(value: any, options: any) => onFilterValueChange([value], "metrics")}
            />
          </Form.Item>
        )}
        {["code_volume_vs_deployment_report"].includes(reportType) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <Form.Item key="code_volume_vs_deployment_metrics" label={"Metric"}>
              <AntSelect
                showArrow={true}
                value={getMetadataValue(metaData, "metrics", "line_count")}
                options={codeVolVsDeploymentMetricsOptions.sort(stringSortingComparator("label"))}
                mode={"single"}
                onChange={(value: any, options: any) => onMetadataChange?.(value, "metrics")}
              />
            </Form.Item>
          )}
        {renderSettings()}
        {["jira_backlog_trend_report", "azure_backlog_trend_report"].includes(reportType) && ( // Removed condition to make Sample Interval and Minimum age visible for filters tab
          <>
            {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
              <Form.Item
                key="left-y-axis"
                label={"Left Y-Axis"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-left-y-axis`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-left-y-axis`}>
                <AntSelect
                  showArrow={true}
                  dropdownTestingKey={`${ITEM_TEST_ID}-left-y-axis_dropdown`}
                  defaultValue={"total_tickets"}
                  value={getMetadataValue(metaData, "leftYAxis", "total_tickets")}
                  options={backlogLeftYAxisOptions.sort(stringSortingComparator("label"))}
                  mode={"single"}
                  onChange={(value: any, options: any) => onMetadataChange?.(value, "leftYAxis")}
                />
              </Form.Item>
            )}
            {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
              <Form.Item
                key="right-y-axis"
                label={"Right Y-Axis"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-right-y-axis`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-right-y-axis`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-right-y-axis_dropdown`}
                  showArrow={true}
                  defaultValue={"median"}
                  value={getMetadataValue(metaData, "rightYAxis", "median")}
                  options={backlogRightYAxisOptions.sort(stringSortingComparator("label"))}
                  mode={"single"}
                  onChange={(value: any, options: any) => onMetadataChange?.(value, "rightYAxis")}
                />
              </Form.Item>
            )}
            {(isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
              <>
                {!isMultiTimeSeriesReport && (
                  <Form.Item key="jira_backlog_trend_report" label={"Sample Interval"}>
                    <AntSelect
                      showArrow={true}
                      value={get(filters, ["interval"], "week")}
                      options={backlogTrendReportOptions.sort(stringSortingComparator("label"))}
                      mode={"single"}
                      onChange={(value: any, options: any) => onFilterValueChange(value, "interval")}
                    />
                  </Form.Item>
                )}

                {![ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT].includes(reportType as ISSUE_MANAGEMENT_REPORTS) && (
                  <Form.Item>
                    <InputRangeFilter
                      value={getFilterValue(filters, "age")}
                      label={"Minimum Age"}
                      onChange={(value: any) => onFilterValueChange(value, "age")}
                    />
                  </Form.Item>
                )}
              </>
            )}
          </>
        )}
        {["resolution_time_report", ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(reportType) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <>
              <Form.Item
                key="resolution_time_metrics"
                label={"Metric"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-resolution-metric`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-resolution-metric`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-resolution-metric_dropdown`}
                  showArrow={true}
                  value={filters?.metric}
                  options={jiraResolutionTimeMetricsOptions.sort(stringSortingComparator("label"))}
                  mode={filters?.graph_type === ChartType.CIRCLE ? "single" : "multiple"}
                  onChange={(value: any, options: any) => onFilterChange(value, "metric")}
                />
              </Form.Item>
            </>
          )}
        {reportType === "scm_issues_time_resolution_report" && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
          <>
            <Form.Item
              key="resolution_time_metrics"
              label={"Metric"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-scm-resolution-metric`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-scm-resolution-metric`}>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-scm-resolution-metric_dropdown`}
                showArrow={true}
                value={filters?.metric}
                options={scmResolutionTimeMetricsOptions.sort(stringSortingComparator("label"))}
                mode={"multiple"}
                onChange={(value: any, options: any) => onFilterChange(value, "metric")}
              />
            </Form.Item>
          </>
        )}
        {[...Object.values(JIRA_SPRINT_REPORTS), ...Object.values(AZURE_SPRINT_REPORTS)].includes(reportType as any) &&
          !sprintStatReports.includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <>
              <Form.Item
                key="jira_sprint_reports"
                label={"Metric"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-jira-sprint-metric`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-jira-sprint-metric`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-jira-sprint-metric_dropdown`}
                  showArrow={true}
                  value={filters?.metric}
                  options={getSortedFilterOptions((jiraSprintMetricOptions as any)[reportType.replace("azure_", "")])}
                  mode={
                    [
                      "sprint_metrics_percentage_trend",
                      "sprint_metrics_trend",
                      "azure_sprint_metrics_percentage_trend",
                      "azure_sprint_metrics_trend"
                    ].includes(reportType as any)
                      ? "multiple"
                      : "single"
                  }
                  onChange={(value: any, options: any) => onFilterChange(value, "metric")}
                />
              </Form.Item>
            </>
          )}
        {[JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <>
              <Form.Item
                key="ticket_report"
                label={"Metric"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-tickets-report-metric`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-tickets-report-metric`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-tickets-report-metric_dropdown`}
                  showArrow={true}
                  value={filters?.metric}
                  options={ticketReportMetricOptions.sort(stringSortingComparator("label"))}
                  mode="single"
                  onChange={(value: any, options: any) => onFilterChange(value, "metric")}
                />
              </Form.Item>
            </>
          )}
        {["scm_repos_report", scmTableReportType.SCM_FILE_TYPES_REPORT].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <Form.Item
              key="scm_metrics"
              label={"Metrics"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-scm-metric`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-scm-metric`}>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-scm-metric_dropdown`}
                showArrow={true}
                value={getMetadataValue(metaData, "metrics", ["num_changes", "num_commits", "num_prs"])}
                options={dynamicSCMMetricOptions}
                mode={"multiple"}
                onChange={(value: any, options: any) => onMetadataChange?.(value, "metrics")}
              />
            </Form.Item>
          )}
        {reportType === "scm_committers_report" && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
          <Form.Item
            key="scm_metrics"
            label={"Metrics"}
            data-filterselectornamekey={`${ITEM_TEST_ID}-scm-metric`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-scm-metric`}>
            <AntSelect
              dropdownTestingKey={`${ITEM_TEST_ID}-scm-metric_dropdown`}
              showArrow={true}
              value={getMetadataValue(metaData, "metrics", [
                "num_changes",
                "num_commits",
                "num_prs",
                "num_jira_issues"
              ])}
              options={scmCommitterMetricOptions}
              mode={"multiple"}
              onChange={(value: any, options: any) => onMetadataChange?.(value, "metrics")}
            />
          </Form.Item>
        )}
        {["tickets_counts_stat"].includes(reportType as any) && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
          <Form.Item
            key="ticket_issues_metrics"
            label={"Metrics"}
            data-filterselectornamekey={`${ITEM_TEST_ID}-tickets-issue-metric`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-tickets-issue-metric`}>
            <AntSelect
              showArrow={true}
              dropdownTestingKey={`${ITEM_TEST_ID}-tickets-issue-metric_dropdown`}
              value={getMetadataValue(metaData, "metrics", ["total_tickets"])}
              options={jiraTicketSingleStatMetricsOptions}
              mode={"single"}
              onChange={(value: any, options: any) => onMetadataChange?.(value, "metrics")}
            />
          </Form.Item>
        )}
        {[
          JiraReports.JIRA_TIME_ACROSS_STAGES,
          "scm_issues_time_across_stages_report",
          ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES
        ].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <>
              <Form.Item
                key="jira_metrics"
                label={"Metric"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-jira-metric`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-jira-metric`}>
                <AntSelect
                  showArrow={true}
                  dropdownTestingKey={`${ITEM_TEST_ID}-jira-metric_dropdown`}
                  value={filters?.metric}
                  options={jiraTimeAcrossStagesMetricOptions.sort(stringSortingComparator("label"))}
                  mode={"single"}
                  onChange={(value: any, options: any) => onFilterValueChange(value, "metric")}
                />
              </Form.Item>
            </>
          )}
        {[...leadTimeReports, ...azureLeadTimeIssueReports].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <>
              {[
                LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
                AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT
              ].includes(reportType as any) && (
                <Form.Item
                  key="lead_time_calculation"
                  label={"Metrics"}
                  data-filterselectornamekey={`${ITEM_TEST_ID}-lead-time-metric`}
                  data-filtervaluesnamekey={`${ITEM_TEST_ID}-lead-time-metric`}>
                  <AntSelect
                    showArrow={true}
                    dropdownTestingKey={`${ITEM_TEST_ID}-lead-time-metric_dropdown`}
                    value={filters?.calculation}
                    options={leadTimeCalculationOptions.sort(stringSortingComparator("label"))}
                    mode={"single"}
                    onChange={(value: any, options: any) => onFilterValueChange(value, "calculation")}
                  />
                </Form.Item>
              )}
              <Form.Item
                key="stage_duration"
                label={"Stage Duration"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-stage-duration`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-stage-duration`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-stage-duration_dropdown`}
                  showArrow={true}
                  value={getMetadataValue(metaData, "metrics", "mean")}
                  options={leadTimeMetricOptions.sort(stringSortingComparator("label"))}
                  mode={"single"}
                  onChange={(value: any, options: any) => onMetadataChange?.(value, "metrics")}
                />
              </Form.Item>
            </>
          )}
        {[
          JiraReports.RESOLUTION_TIME_REPORT,
          JiraReports.RESOLUTION_TIME_REPORT_TRENDS,
          JiraStatReports.RESOLUTION_TIME_COUNTS_STAT
        ].includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <JiraExcludeStatusFilter
              reportType={reportType}
              filters={filters}
              data={getJiraResolutionTimeAcrossStagesOptions}
              onFilterValueChange={onFilterValueChange}
              createOption={false}
            />
          )}
        {[
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_TREND_REPORT,
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_SINGLE_STAT_REPORT
        ].includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <JiraExcludeStatusFilter
              reportType={reportType}
              filters={filters}
              data={getAzureTimeAcrossStagesOptions}
              onFilterValueChange={onFilterValueChange}
            />
          )}
        {[...leadTimeReports, ...azureLeadTimeIssueReports].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <>
              <VelocityConfigFilter
                filters={filters}
                reportType={reportType}
                metaData={props.metaData}
                onFilterValueChange={onFilterValueChange}
                onMetadataChange={onMetadataChange}
              />
              <Form.Item
                key="apply_ou_on_velocity_report"
                label={"Apply Filters"}
                data-filterselectornamekey={`${ITEM_TEST_ID}-apply-ou-filter`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-apply-ou-filter`}>
                <AntSelect
                  dropdownTestingKey={`${ITEM_TEST_ID}-apply-ou-filter_dropdown`}
                  showArrow={true}
                  value={
                    metaData?.hasOwnProperty("apply_ou_on_velocity_report")
                      ? metaData.apply_ou_on_velocity_report
                      : true
                  }
                  options={APPLY_OU_ON_VELOCITY_OPTIONS}
                  mode={"single"}
                  onChange={(value: any, options: any) => onMetadataChange?.(value, "apply_ou_on_velocity_report")}
                />
              </Form.Item>
            </>
          )}
        {jenkinsTrendReports.includes(props.reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <Form.Item
              key="sample_interval"
              label={"Sample Interval"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-sample-interval`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-sample-interval`}>
              <AntSelect
                showArrow
                dropdownTestingKey={`${ITEM_TEST_ID}-sample-interval_dropdown`}
                value={get(props.filters, ["interval"], "month")}
                options={cicdIntervalOptions}
                mode="single"
                onChange={(value: any, options: any) => props.onFilterValueChange(value, "interval")}
              />
            </Form.Item>
          )}
        {[JIRA_SPRINT_REPORTS.SPRINT_IMPACT, AZURE_SPRINT_REPORTS.SPRINT_IMPACT].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <Form.Item
              key="sample_interval"
              label={"Sample Interval"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-sprint-sample-interval`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-sprint-sample-interval`}>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-sprint-sample-interval_dropdown`}
                showArrow={true}
                value={get(filters, ["across"], "week")}
                options={(reportType.includes("azure_") ? azureIntervalReport : sprintImpactIntervalOptions).sort(
                  stringSortingComparator("label")
                )}
                mode={"single"}
                onChange={(value: any, options: any) => props.onAggregationAcrossSelection?.(value)}
              />
            </Form.Item>
          )}
        {[
          JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
          AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND
        ].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <Form.Item
              key="visualization"
              label={"Visualization"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-visualization`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-visualization`}>
              <AntSelect
                showArrow
                dropdownTestingKey={`${ITEM_TEST_ID}-visualization_dropdown`}
                value={get(filters, ["visualization"], "stacked_area")}
                options={sprintVisualizationOptions.sort(stringSortingComparator("label"))}
                mode="single"
                onChange={(value: any, options: any) => onFilterValueChange(value, "visualization")}
              />
            </Form.Item>
          )}
        {SPRINT_GRACE_OPTIONS_REPORTS.includes(reportType) && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <Form.Item
            key="sprint_creep_grace_period"
            data-filterselectornamekey={`${ITEM_TEST_ID}-sprint-creep-grace-period`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-sprint-creep-grace-period`}
            label={
              <CustomFormItemLabel
                label="Sprint Creep Grace Period"
                withInfo={{
                  showInfo: true,
                  description: SPRINT_GRACE_INFO
                }}
              />
            }>
            <AntSelect
              showArrow
              allowClear
              dropdownTestingKey={`${ITEM_TEST_ID}-sprint-creep-grace-period_dropdown`}
              value={get(filters, ["creep_buffer"], "")}
              options={SPRINT_GRACE_OPTIONS}
              mode="single"
              onChange={(value: any, options: any) => onFilterValueChange(value, "creep_buffer")}
            />
          </Form.Item>
        )}
        {(isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
          <APIFilters
            data={getRightFiltersData()}
            filters={filters}
            supportExcludeFilters={supportExcludeFilters}
            supportPartialStringFilters={supportPartialStringFilters}
            handlePartialValueChange={onPartialChange}
            handleFilterValueChange={onFilterValueChange}
            handleSwitchValueChange={onSwitchValueChange}
            partialFilterError={props.partialFilterError}
            hasNext={props.hasNext}
            reportType={props.reportType}
            activePopkey={activePopKey}
            handleActivePopkey={key => setActivePopKey(key)}
          />
        )}
        {application === IntegrationTypes.JIRA &&
          !["sprint_goal"].includes(reportType) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) &&
          !isBAWithCommitCount && (
            <>
              <Form.Item
                key={`${ITEM_TEST_ID}-epics`}
                data-filterselectornamekey={`${ITEM_TEST_ID}-epics`}
                data-filtervaluesnamekey={`${ITEM_TEST_ID}-epics`}
                label={"Epics"}>
                <CustomSelect
                  dataTestid="filter-list-element-select"
                  createOption
                  options={[]}
                  mode={"multiple"}
                  showArrow={false}
                  value={filters?.["epics"] || []}
                  truncateOptions
                  labelCase={"title_case"}
                  dropdownVisible
                  onChange={(value: string[]) => onFilterValueChange(value, "epic")}
                />
              </Form.Item>
            </>
          )}
        {["jenkinsgithub", "jenkins"].includes(application) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <JenkinsGithubParameters
              application={application}
              data={data}
              filters={filters}
              reportType={reportType}
              onFilterValueChange={onFilterValueChange}
            />
          )}
        {["jira", "azure_devops"].includes(application) &&
          reportType.includes("hygiene") &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <JiraHygieneFilters filters={filters} onFilterValueChange={debouncedOnFilterValueChange} />
          )}
        {["salesforce_top_customers_report", "zendesk_top_customers_report"].includes(reportType) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <TimeRangeComponent filterValueChange={onFilterValueChange} value={filters.age || {}} />
          )}
        {["zendesk_time_across_stages", "salesforce_time_across_stages"].includes(reportType) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <TimeAcrossStagesFilterComponent
              filters={filters}
              stateOptions={data ? data.filter((item: any) => Object.keys(item)[0] === "jira_status")[0] : []}
              onFilterValueChange={onFilterValueChange}
            />
          )}
        {["jira", "jirazendesk", "jirasalesforce"].includes(application) &&
          ![
            "sprint_goal",
            "tickets_counts_stat",
            JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_SINGLE_STAT_REPORT
          ].includes(reportType) &&
          !isBAWithCommitCount &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <JiraIssueTimeFilters
              filters={filters}
              metaData={props.metaData}
              reportType={reportType}
              onMetadataChange={onMetadataChange}
              dashboardMetaData={dashboardMetaData}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
              application={application}
              onFilterValueChange={(value: any, type?: any, rangeType?: string) => {
                onTimeFilterValueChange?.(value, type, rangeType);
              }}
            />
          )}
        {[
          JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT,
          ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
          JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_SINGLE_STAT_REPORT,
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_SINGLE_STAT_REPORT
        ].includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <StatTimeRangeFilters
              filters={filters}
              metaData={props.metaData}
              reportType={reportType}
              onMetadataChange={onMetadataChange}
              dashboardMetaData={dashboardMetaData}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
              onFilterValueChange={onFilterValueChange}
              onTimeFilterValueChange={onTimeFilterValueChange}
              onSingleStatTypeFilterChange={onSingleStatTypeFilterChange}
            />
          )}
        {[
          ...Object.values(JIRA_SPRINT_DISTRIBUTION_REPORTS),
          ...Object.values(JIRA_SPRINT_REPORTS),
          ...Object.values(AZURE_SPRINT_REPORTS)
        ].includes(reportType as any) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
            <Form.Item key={uuid()} label={"Last N Sprints"}>
              <AntInput
                type="number"
                value={filters.sprint_count}
                className="w-100"
                onChange={(e: number) => onFilterValueChange(e, "sprint_count")}
              />
            </Form.Item>
          )}
        {AppName.jenkinsEndTimeFilterReports.includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <JenkinsJobTimeFilter
              report={reportType}
              filters={filters}
              metaData={props.metaData}
              onMetadataChange={onMetadataChange}
              dashboardMetaData={dashboardMetaData}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
              application={application}
              onFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                onTimeFilterValueChange?.(value, type, rangeType)
              }
            />
          )}
        {["praetorian"].includes(application) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <PraetorianFilters
              application={application}
              filters={filters}
              metaData={props.metaData}
              report={reportType}
              onFilterValueChange={onFilterValueChange}
              dashboardMetaData={dashboardMetaData}
              onTimeFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                onTimeFilterValueChange?.(value, type, rangeType)
              }
              onMetadataChange={onMetadataChange}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
            />
          )}
        {["nccgroup"].includes(application) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <NccGroupFilters
              application={application}
              filters={filters}
              metaData={props.metaData}
              report={reportType}
              onFilterValueChange={onFilterValueChange}
              onTimeFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                onTimeFilterValueChange?.(value, type, rangeType)
              }
              onMetadataChange={onMetadataChange}
              dashboardMetaData={dashboardMetaData}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
            />
          )}
        {["snyk"].includes(application) && (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
          <SynkFilters
            application={application}
            filters={filters}
            metaData={props.metaData}
            report={reportType}
            onFilterValueChange={onFilterValueChange}
            onTimeFilterValueChange={(value: any, type?: any, rangeType?: string) =>
              onTimeFilterValueChange?.(value, type, rangeType)
            }
            onMetadataChange={onMetadataChange}
            dashboardMetaData={dashboardMetaData}
            onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
          />
        )}

        {reportType === JENKINS_REPORTS.SCM_CODING_DAYS_REPORT && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
          <Form.Item key="metrics" label={"Metrics"}>
            <AntSelect
              showArrow
              value={get(filters, "metrics", "avg_coding_day_week")}
              options={scmCodingMetricsOptions}
              mode="single"
              onChange={(value: any, options: any) => onFilterValueChange(value, "metrics")}
            />
          </Form.Item>
        )}
        {reportType === JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <Form.Item key="metrics" label={"Metrics"}>
              <AntSelect
                showArrow
                value={get(filters, "metrics", "average_coding_day")}
                options={scmCodingSingleStatMetricsOptions}
                mode="single"
                onChange={(value: any, options: any) => onFilterValueChange(value, "metrics")}
              />
            </Form.Item>
          )}

        {scmEnhancedReports.includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <>
              {!["github_commits_report", "github_commits_single_stat"].includes(reportType) && (
                <>
                  <InputRangeFilter
                    value={getFilterValue(filters, "num_reviewers")}
                    label={"Number Of Reviewers"}
                    onChange={(value: any) => onFilterValueChange(value, "num_reviewers")}
                  />
                  <InputRangeFilter
                    value={getFilterValue(filters, "num_approvers")}
                    label={"Number Of Approvers"}
                    onChange={(value: any) => onFilterValueChange(value, "num_approvers")}
                  />
                  <Form.Item key="approval_status" label={"Other Criteria"}>
                    <AntCheckboxGroup
                      className="criteria-group"
                      value={filters.approval_statuses}
                      options={scmOtherCriteriaOptions}
                      onChange={(e: number) => onFilterValueChange(e, "approval_statuses")}
                    />
                    <Checkbox
                      checked={get(filters, ["has_issue_keys"], "false") === "true"}
                      onChange={(e: any) => onFilterValueChange(e.target.checked ? "true" : "false", "has_issue_keys")}>
                      Has Linked Issues
                    </Checkbox>
                  </Form.Item>
                  <Form.Item key="comment_density" label={"PR Comment Density"}>
                    <AntSelect
                      showArrow
                      allowClear
                      value={filters?.comment_densities}
                      options={scmCommentDensityOptions}
                      mode="multiple"
                      onChange={(value: any, options: any) => onFilterValueChange(value, "comment_densities")}
                    />
                  </Form.Item>
                </>
              )}

              <Form.Item key="code_change_sizes" label={"Code Change Size"}>
                <AntSelect
                  showArrow
                  allowClear
                  value={filters?.code_change_sizes}
                  options={scmCodeChangeOptions}
                  mode="multiple"
                  onChange={(value: any, options: any) => onFilterValueChange(value, "code_change_sizes")}
                />
              </Form.Item>
            </>
          )}
        {[JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_SINGLE_STAT].includes(
          reportType as any
        ) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <Form.Item key="metrics" label={"Metrics"}>
              <AntSelect
                showArrow
                value={get(filters, "metrics", "average_author_response_time")}
                options={scmPRsResponseTimeMetricsOptions}
                mode="single"
                onChange={(value: any, options: any) => onFilterValueChange(value, "metrics")}
              />
            </Form.Item>
          )}
        {scmEnhancedReports.includes(reportType as any) && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <>
            {[JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_REPORT].includes(
              reportType as any
            ) && (
              <Form.Item key="visualization" label={"Visualization"}>
                <AntSelect
                  showArrow
                  value={get(props.metaData, "visualization", SCMVisualizationTypes.CIRCLE_CHART)}
                  options={scmVisualizationOptions}
                  mode="single"
                  onChange={(value: any, options: any) => onMetadataChange?.(value, "visualization")}
                />
              </Form.Item>
            )}
            <SCMCodeFilters
              scmGlobalSettings={scmGlobalSettings}
              report={reportType}
              metaData={props.metaData}
              onMetadataChange={onMetadataChange}
            />
          </>
        )}
        {leadTimeReports.includes(reportType as any) &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <LeadTimeFilters
              filters={filters}
              metaData={props.metaData}
              reportType={reportType}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
              application={application}
              onMetadataChange={onMetadataChange}
              dashboardMetaData={dashboardMetaData}
              onFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                onTimeFilterValueChange?.(value, type, rangeType)
              }
            />
          )}
        {getWidgetConstant("widgetSettingsTimeRangeFilterSchema") &&
          (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) || applicationUse) && (
            <AgnosticTimeRangeFilter
              timeRangeFiltersSchema={getWidgetConstant("widgetSettingsTimeRangeFilterSchema")}
              filters={filters}
              metaData={props.metaData}
              reportType={reportType}
              onRangeTypeChange={(key: string, value: any) => onTimeRangeTypeChange?.(key, value)}
              application={application}
              onMetadataChange={onMetadataChange}
              dashboardMetaData={dashboardMetaData}
              onFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                onTimeFilterValueChange?.(value, type, rangeType)
              }
            />
          )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && ["zendesk"].includes(application) && (
          <ZendeskCreatedInFilter
            filters={filters}
            reportType={reportType}
            metadata={metaData}
            onFilterValueChange={onTimeFilterValueChange}
            onMetadataChange={onMetadataChange}
            dashboardMetaData={dashboardMetaData}
            onTimeRangeTypeChange={onTimeRangeTypeChange}
          />
        )}
        {sprintStatReports.includes(reportType as any) && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <IdealRangeFilter
            idealFilters={get(metaData, [IDEAL_RANGE_FILTER_KEY], {})}
            onFilterValueChange={onMetadataChange}
          />
        )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && sprintStatReports.includes(reportType as any) && (
          <StatSprintMetricFilter
            value={get(filters, ["metric"], undefined)}
            onFilterValueChange={onFilterValueChange}
          />
        )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && supportReports.includes(reportType as any) && (
          <Form.Item
            key="support_system_select"
            label={"Issue Management System"}
            data-filterselectornamekey={`${ITEM_TEST_ID}-cross-agg-management`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-cross-agg-management`}>
            <AntSelect
              showArrow={true}
              defaultValue={"zendesk"}
              dropdownTestingKey={`${ITEM_TEST_ID}-cross-agg-management_dropdown`}
              disabled={getMetadataValue(metaData, "disable_support_system", false)}
              value={reportType.includes("salesforce") ? "salesforce" : "zendesk"}
              options={supportSystemOptions.sort(stringSortingComparator("label"))}
              mode={"single"}
              onChange={(value: any, options: any) => onMetadataChange?.(value, "support_system")}
            />
          </Form.Item>
        )}
        {reportType === "scm_issues_time_across_stages_report" &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <JiraExcludeStatusFilter
              reportType={reportType}
              filters={filters}
              data={getScmTimeAcrossStagesOptions}
              onFilterValueChange={onFilterValueChange}
            />
          )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) &&
          Object.values(COMBINED_JIRA_SPRINT_REPORT).includes(reportType as any) && (
            <SprintDoneStatusSelect
              onFilterValueChange={onFilterValueChange}
              value={
                doneStatusFilter
                  ? get(filters, doneStatusFilter?.valueKey, [])
                  : get(filters, ["additional_done_statuses"], [])
              }
              valueKey={doneStatusFilter ? doneStatusFilter?.valueKey : "additional_done_statuses"}
              apiOptions={getSprintDoneStatus}
              integrationIds={props.integrationIds}
            />
          )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) &&
          [
            ...leadTimeIssueReports,
            ...azureLeadTimeIssueReports,
            ...issueManagementReports,
            ...jiraManagementTicketReport
          ].includes(reportType as any) &&
          !(widgetConstants as any)[reportType][HIDE_ISSUE_MANAGEMENT_SYSTEM_DROPDOWN] && (
            <Form.Item
              key="support_system_select"
              label={"Issue Management System"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-issue-management`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-issue-management`}>
              <AntSelect
                showArrow={true}
                defaultValue={settingsImsDefaultValue}
                dropdownTestingKey={`${ITEM_TEST_ID}-issue-management_dropdown`}
                disabled={
                  getMetadataValue(metaData, "disable_issue_management_system", false) || settingsImsDefaultValue === ""
                }
                value={
                  application.includes("azure_devops")
                    ? "azure_devops"
                    : reportType === LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT
                    ? "githubjira"
                    : "jira"
                }
                options={(reportType === LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT
                  ? singleStatIssueManagementSystemOptions
                  : issueManagementSystemOptions
                ).sort(stringSortingComparator("label"))}
                mode={"single"}
                onChange={(value: any, options: any) => onMetadataChange?.(value, "issue_management_system")}
              />
            </Form.Item>
          )}

        {reportType === "scm_rework_report" && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <>
            <Form.Item key="visualization" label={"Visualization"}>
              <AntSelect
                showArrow
                value={get(props.metaData, "visualization", SCMReworkVisualizationTypes.STACKED_BAR_CHART)}
                options={scmReworkVisualizationOptions}
                mode="single"
                onChange={(value: any, options: any) => onMetadataChange?.(value, "visualization")}
              />
            </Form.Item>
            <Form.Item
              key="legacy_code"
              label={
                <CustomFormItemLabel
                  label="Legacy Code"
                  withInfo={{
                    showInfo: true,
                    description: LEGACY_CODE_INFO
                  }}
                />
              }>
              <Form.Item key="legacy_update_interval_config" label={"Last File Update Timestamp"}>
                <AntSelect
                  showArrow
                  allowClear
                  value={get(metaData, ["legacy_update_interval_config"], 30)}
                  options={lastFileUpdateIntervalOptions}
                  mode="single"
                  onChange={(value: any, options: any) => onMetadataChange?.(value, "legacy_update_interval_config")}
                />
              </Form.Item>
            </Form.Item>
          </>
        )}
        {isBAWithCommitCount && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
          <TimeRangeAbsoluteRelativeWrapperComponent
            key={`Committed_At`}
            label={"Committed In"}
            filterKey={"committed_at"}
            metaData={metaData}
            filters={filters}
            onFilterValueChange={(data: any, key: string) => {
              onTimeFilterValueChange?.(data, key);
            }}
            onTypeChange={(key: string, value: { type: string; relative: any }) => onTimeRangeTypeChange?.(key, value)}
            onMetadataChange={onMetadataChange}
            dashboardMetaData={props.dashboardMetaData}
          />
        )}
        <FEBasedFiltersContainer
          filters={filters}
          metadata={metaData}
          report={reportType}
          onFilterValueChange={onFilterValueChange}
          onTimeRangeTypeChange={onTimeRangeTypeChange}
          onTimeFilterValueChange={onTimeFilterValueChange}
          filterFEBasedFilters={filterFEBasedFilters}
          onMetadataChange={onMetadataChange}
          dashboardMetaData={dashboardMetaData}
        />
        {allowWidgetDataSorting(reportType, filters) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) &&
          !props.isMultiTimeSeriesReport &&
          !props.isCompositeChild && (
            <WidgetDataSortFilter
              filters={filters}
              onFilterValueChange={onFilterValueChange}
              acrossIsAzureIteration={acrossIsAzureIteration({
                reportType,
                application,
                across: get(filters, ["across"], "")
              })}
            />
          )}
      </div>
    </div>
  );
};

export default DashboardGraphFiltersComponent;
