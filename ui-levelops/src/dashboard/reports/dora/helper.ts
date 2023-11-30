import { TIME_INTERVAL_TYPES } from "constants/time.constants";
import { dateRangeFilterValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { valuesToFilters } from "dashboard/constants/constants";
import DrilldownToggle from "dashboard/pages/dashboard-drill-down-preview/components/DrilldownToggleComponent";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import {
  DORA_REPORT_TO_KEY_MAPPING
} from "dashboard/graph-filters/components/helper";
import { findIntegrationType } from "helper/integration.helper";
import { get } from "lodash";
import moment from "moment";
import { sanitizeFilterObject } from "utils/filtersUtils";
import { DateRange, getDateRangeEpochToString, getMomentFromInterval, valueToUtcUnixTime } from "utils/dateUtils";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { Integration } from "model/entities/Integration";
import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { ROLLBACK_KEY } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { Dict } from "types/dict";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";

export const getConditionalUri = (params: any) => {
  const integrationId = get(params.workspaceProfile, [DORA_REPORT_TO_KEY_MAPPING[params.reportType], "integration_id"]);
  const integrationObj = params?.integrationState?.find((item: any) => {
    return +item?.id === integrationId;
  });
  const integrationType = findIntegrationType(integrationObj || null);
  switch (integrationType) {
    case "SCM":
      return "github_prs_filter_values";
    case "CICD":
      return "jenkins_jobs_filter_values";
    case "IM":
      if (integrationObj?.application === IntegrationTypes.JIRA) {
        return "jira_filter_values";
      }
      if (integrationObj?.application === IntegrationTypes.AZURE) {
        return "issue_management_workitem_values";
      }
    default:
      return "jira_filter_values";
  }
};

export const getDrilldownTitle = (params: any) => {
  let dateRange = getDateRangeEpochToString(params?.xAxis?.time_range);
  let splitDateRange = dateRange?.split("-");
  if (splitDateRange[0]?.trim() === splitDateRange[1]?.trim()) {
    return splitDateRange[0];
  } else {
    return dateRange;
  }
};

export const onChartClickPayload = (params: any) => {
  let { date, interval, dashboardTimeRangeData, count } = params;
  let gt, lt;
  if (interval === TIME_INTERVAL_TYPES.WEEK) {
    gt = valueToUtcUnixTime(moment.utc(date));
    lt = valueToUtcUnixTime(moment.utc(date).add(6, "d").endOf("day"));
  } else {
    gt = valueToUtcUnixTime(getMomentFromInterval(date, interval, DateRange.FROM));
    lt = valueToUtcUnixTime(getMomentFromInterval(date, interval, DateRange.TO));
  }
  let finalLt = dashboardTimeRangeData?.$lt < lt ? dashboardTimeRangeData?.$lt : lt;
  let finalGt = dashboardTimeRangeData?.$gt > gt ? dashboardTimeRangeData?.$gt : gt;

  return {
    time_range: {
      $lt: finalLt,
      $gt: finalGt
    },
    count
  };
};

export const getHideFilterButton = () => {
  return true;
};

export const extractFilterKeys = (filter: any) => {
  if (!filter) return [];
  let normalKeys = Object.keys(filter).filter((item: any) => {
    return item !== "exclude" && item !== "partial_match";
  });

  const partialMatchObj = sanitizeFilterObject(filter["partial_match"] ?? {});
  // @ts-ignore
  const partialMatchObjkeys = Object.keys(partialMatchObj).map(partialKey => valuesToFilters[partialKey] || partialKey);

  const excludeObj = sanitizeFilterObject(filter["exclude"] ?? {});
  const excludeObjkeys = Object.keys(excludeObj);

  if (normalKeys.includes("custom_fields")) {
    const custom_fields = filter["custom_fields"];
    const customFieldsFilters = extractFilterKeys(custom_fields);
    normalKeys = normalKeys.filter(keys => keys !== "custom_fields");
    normalKeys = [...normalKeys, ...(customFieldsFilters || [])];
  }

  const allKeysToRemove = [...normalKeys, ...partialMatchObjkeys, ...excludeObjkeys];
  return allKeysToRemove;
};

export const getOUFilterKeys = (selectedOU: any) => {
  const sections = get(selectedOU, "sections", []);
  const filterKeys = sections.reduce((acc: string[], section: any) => {
    const integrations = get(section, "integrations", {});
    const integrationId = Object.keys(integrations)[0];
    const filters = get(integrations, [integrationId, "filters"], {});
    if (Object.keys(filters).length) {
      return [...acc, ...extractFilterKeys(filters)];
    }
    return acc;
  }, []);
  return filterKeys;
};

export const getDoraSingleStateValue = (params: any) => {
  const { isRelative, count, realValue, descStringValue } = params;
  if (isRelative) {
    return `${realValue} of ${count} ${descStringValue}`;
  } else {
    return `${count} ${descStringValue}`;
  }
};
export const getToggleComponent = () => {
  return DrilldownToggle;
};

export const onChangeHandler = (params: any) => {
  const { value, filters, setReload } = params;
  if (value) {
    return {
      ...filters,
      filter: {
        ...filters.filter,
        across: "velocity"
      }
    };
  } else {
    delete filters?.filter?.across;
    return { ...filters };
  }
};

export const getLastNextValue = (dashboardMetadata: {
  dashboard_time_range_filter: string | { $gt: string | number; $lt: string | number };
}) => {
  const dateRange = get(dashboardMetadata, ["dashboard_time_range_filter"], "last_30_days");
  const startOfToday = moment.utc().startOf("day").unix();
  const endOfToday = moment.utc().endOf("day").unix();
  const gt = parseInt(dateRangeFilterValue(dateRange).$gt.toString());
  const lt = parseInt(dateRangeFilterValue(dateRange).$lt.toString());
  const lastValue = Math.round((startOfToday - gt) / 86400);
  const nextValue = Math.round((lt - endOfToday) / 86400);
  return { lastValue, nextValue };
};

export const getMetadata = (lastValue: number, nextValue: number) => {
  const next = nextValue === 0 ? { unit: "today" } : { num: nextValue, unit: "days" };
  return {
    [RANGE_FILTER_CHOICE]: {
      time_range: {
        type: "relative",
        relative: {
          last: {
            num: lastValue,
            unit: "days"
          },
          next
        }
      }
    },
    dashBoard_time_keys: {
      time_range: { use_dashboard_time: true }
    }
  };
};

export const prevReportTransformer = (
  widget: RestWidget,
  dashboardMetadata: {
    dashboard_time_range_filter: string | { $gt: string | number; $lt: string | number };
  }
) => {
  const metadata = widget?.metadata;
  const { lastValue, nextValue } = getLastNextValue(dashboardMetadata);
  const _metadata = getMetadata(lastValue, nextValue);
  if (!metadata?.range_filter_choice?.["time_range"]) {
    metadata.range_filter_choice = {
      ...metadata.range_filter_choice,
      ..._metadata?.range_filter_choice
    };
  }
  if (!metadata.hasOwnProperty("dashBoard_time_keys")) {
    metadata.dashBoard_time_keys = _metadata.dashBoard_time_keys;
    const dateRange = get(dashboardMetadata, ["dashboard_time_range_filter"], "last_30_days");
    const newFilters = {
      ...widget.query,
      time_range: {
        $gt: dateRangeFilterValue(dateRange).$gt,
        $lt: dateRangeFilterValue(dateRange).$lt
      }
    };
    widget.query = newFilters;
  }
};

export const getDefaultMetadata = (param: { dashboard: RestDashboard }) => {
  const { dashboard } = param;
  const dashboardMetadata = get(dashboard, "metadata", {});
  const { lastValue, nextValue } = getLastNextValue(dashboardMetadata);
  return getMetadata(lastValue, nextValue);
};

export const getDefaultQuery = (param: { dashboard: RestDashboard }) => {
  const { dashboard } = param;
  const dateRange = get(dashboard, ["metadata", "dashboard_time_range_filter"], "last_30_days");
  return {
    time_range: {
      $gt: dateRangeFilterValue(dateRange).$gt,
      $lt: dateRangeFilterValue(dateRange).$lt
    }
  };
};

export const getDoraGrapthFilters = (params: {
  filters: any;
  widgetQuery: { time_range: { $gt: string | number; $lt: string | number } };
  widgetMetaData: { dashBoard_time_keys: { time_range: { use_dashboard_time: boolean } } };
  reportType: string;
}) => {
  const { filters, widgetMetaData, reportType, widgetQuery } = params;
  const dashBoard_time_keys = get(widgetMetaData, "dashBoard_time_keys", {});
  const useDashboardTime = get(dashBoard_time_keys, ["time_range", "use_dashboard_time"], false);
  let finalFilters = { ...filters };
  if (!useDashboardTime && filters?.filter) {
    finalFilters = {
      ...finalFilters,
      filter: {
        ...finalFilters.filter,
        time_range: get(widgetQuery, "time_range")
      }
    };
    finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, widgetMetaData, reportType, "");
  }
  return finalFilters;
};

export const removedUnusedFilterConfig = (filterConfig: any) => {
  const removedKeys = [
    "apply_ou_on_velocity_report",
    "issue_management_system",
    "ou_user_filter_designation",
    "cicd_job_run_end_time"
  ];
  return filterConfig.filter((item: any) => !removedKeys.includes(item.beKey));
};

export const onChartClickHandler = (stageName: string, statClicked: boolean, id?: string) => {
  if (id) {
    return { histogram_stage_name: stageName, name: stageName, widgetId: id };
  }
  if (statClicked) {
    return { null: ["Total"] };
  }
  return { histogram_stage_name: stageName };
};

export const getFiltereredIntegrationIds = (
  availableIntegrations: Integration[],
  profileType: string,
  integrationIds: string[]
) => {
  switch (profileType) {
    case "IM":
      return integrationIds.filter((id: string) => {
        const int = availableIntegrations.find((integration: Integration) => integration.id === id);
        const application = int?.application;
        if (application === IntegrationTypes.AZURE) {
          return ["scm", "cicd"].includes(int?.metadata?.subtype?.toString() || "");
        }
        return true;
      });
    case "SCM":
    case IM_ADO:
      return integrationIds.filter((id: string) => {
        const application = availableIntegrations.find(
          (integration: Integration) => integration.id === id
        )?.application;
        return !["jira"].includes(application as string);
      });
  }
};

export const getCICDSuportedFilters = (integrationApplication: string) => {
  let supportedFilters = ["job_status", "project_name", "cicd_user_id"];
  switch (integrationApplication) {
    case IntegrationTypes.JENKINS:
      supportedFilters = ["job_status", "instance_name", "cicd_user_id"];
      break;
    case IntegrationTypes.AZURE:
      supportedFilters = ["project_name", "job_status"];
      break;
    case IntegrationTypes.HARNESSNG:
      supportedFilters = [
        ROLLBACK_KEY,
        "cicd_user_id",
        "job_status",
        "service",
        "environment",
        "infrastructure",
        "deployment_type",
        "repository",
        "branch",
        "tag",
        "project_name",
        "job_name",
        "job_normalized_full_name"
      ];
      break;
  }
  return supportedFilters;
};
export const getDoraProfileIntegrationApplication = (params: any) => {
  let integrationApplication = get(
    params.workspaceOuProfilestate,
    [DORA_REPORT_TO_KEY_MAPPING[params.reportType], "application"],
    undefined
  );
  return integrationApplication;
};

export const doraFilterNameMapping: Dict<string, string> = {
  project_name: "Project",
  cicd_user_id: "Triggered By",
  job_status: "Status"
};
