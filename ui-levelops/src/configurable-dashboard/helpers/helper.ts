import { AZURE_TIME_FILTERS_KEYS, GROUP_BY_TIME_FILTERS } from "constants/filters";
import { forEach, get, uniqBy } from "lodash";
import {
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  LEAD_TIME_REPORTS
} from "dashboard/constants/applications/names";
import {
  AZURE_CUSTOM_FIELD_PREFIX,
  GROUP_BY_ROOT_FOLDER,
  valuesToFilters
} from "dashboard/constants/constants";

export const REPORT_TYPE_HINT =
  "Single widget allows you to deep dive into a metric from all aspects, where Multi-Metric widget allows you to look into different metrics across multiple data points.";


const getFilterKey = (key: string, widgetValuesToFiltersMapping: any = {}) => {
  let _key = key;

  if (widgetValuesToFiltersMapping.hasOwnProperty(key)) {
    _key = widgetValuesToFiltersMapping[key];
  } else {
    _key = get(valuesToFilters, [key], key);
  }

  return _key;
};

export const getFilterValue = (filters: any, key: string, fromEIProfile = false, widgetValuesToFiltersMapping = {}) => {
  if (key.includes("customfield_") || key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
    let customFieldKey =
      fromEIProfile && key.includes(AZURE_CUSTOM_FIELD_PREFIX) ? "workitem_custom_fields" : "custom_fields";
    const excludeVal = get(filters, ["exclude", customFieldKey, key], undefined);
    return excludeVal ? excludeVal : get(filters, [customFieldKey, key], []);
  } else {
    const filterKey = getFilterKey(key, widgetValuesToFiltersMapping);
    const excludeVal = get(filters, ["exclude", filterKey], undefined);
    return excludeVal ? excludeVal : get(filters, [filterKey], []);
  }
};

export const getMetadataValue = (metaData: any, key: string, defaultValue: any) => {
  if (key === "metrics") {
    const value = get(metaData, [key], defaultValue);
    return value.length === 0 ? defaultValue : value;
  }
  return get(metaData, [key], defaultValue);
};

export const getAcrossValue = (filters: any, reportType: string) => {
  let across = filters?.across;

  if (
    [
      ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
      "resolution_time_report",
      "tickets_report",
      "zendesk_tickets_report",
      "scm_issues_time_resolution_report",
      ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
      ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
      JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT
    ].includes(reportType)
  ) {
    if ([...GROUP_BY_TIME_FILTERS, "ticket_created", ...AZURE_TIME_FILTERS_KEYS].includes(across)) {
      const interval = get(filters, ["interval"]);
      if (interval) across = `${across}_${interval}`;
    }
  }

  if (
    [JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT, JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT].includes(reportType as any)
  ) {
    // added trend to fix the bug caused by old reports still sending trend in across
    if (["job_end", "trend"].includes(across)) {
      const interval = get(filters, ["interval"], "day");
      across = `job_end_${interval}`;
    }
  }

  if (reportType === "scm_issues_time_across_stages_report" && ["issue_created", "issue_closed"].includes(across)) {
    const interval = get(filters, ["interval"], "day");
    across = `${across}_${interval}`;
  }

  if (["github_commits_report", "scm_rework_report"].includes(reportType) && ["trend"].includes(across)) {
    const interval = get(filters, ["interval"], "day");
    across = `${across}_${interval}`;
  }

  if (
    [JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_REPORT].includes(reportType as any) &&
    ["pr_created", "pr_closed"].includes(across)
  ) {
    const interval = get(filters, ["interval"], "week");
    across = `${across}_${interval}`;
  }

  if (reportType === LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT) {
    if (filters?.stacks?.length) {
      across = filters?.stacks[0];
    } else {
      across = "issue_type";
    }
  }

  return across;
};

export const getFilterOptions = (data: any): Array<any> => {
  return data
    .filter((item: any) => !!item["key"])
    .map((item: any, index: number) => ({
      label: item["key"].replace("_", " "),
      value: item["key"],
      ...(item.hasOwnProperty("parent_key") ? { parent_key: item["parent_key"] } : {})
    }));
};

export const isExcludeVal = (
  filters: any,
  key: string,
  customFieldsKey = "custom_fields",
  widgetValuesToFilterMapping: any = {}
) => {
  if (key.includes("customfield_") || key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
    return !!get(filters, ["exclude", customFieldsKey, key], undefined);
  } else {
    let filterKey = key;
    if (widgetValuesToFilterMapping.hasOwnProperty(key)) {
      filterKey = widgetValuesToFilterMapping[key];
    } else {
      filterKey = get(valuesToFilters, [key], key);
    }
    return !!get(filters, ["exclude", filterKey], undefined);
  }
};

export const validateConfigTableWidget = (data: any) => {
  const widgetType = get(data, ["metadata", "widget_type"], "");
  const groupBy = get(data, ["metadata", "groupBy"], "");
  const xAxis = get(data, ["metadata", "xAxis"], "");
  const yAxis = get(data, ["metadata", "yAxis"], []);
  const tableId = get(data, ["metadata", "tableId"], "");
  const type = get(data, ["type"], "");

  const valid = type.length && tableId.length;

  return groupBy
    ? !!(valid && xAxis.length)
    : widgetType.includes("stat")
    ? !!(valid && yAxis.length)
    : !!(valid && xAxis.length && yAxis.length);
};


export const getGroupByRootFolderKey = (selectedReport?: string) => {
  return `${GROUP_BY_ROOT_FOLDER}_${selectedReport || ""}`;
};

export const groupByRootFolderKeyCheck = (metadata: any) => {
  let exist = false;
  forEach(Object.keys(metadata), (key: string) => {
    if (key.includes(GROUP_BY_ROOT_FOLDER)) {
      exist = true;
    }
  });
  return exist;
};

export const jiraCustomFieldsList = (data: any) => {
  const customfieldsdata = data
    .map((item: any) => {
      if (Object.keys(item.config || {}).length) {
        return item.config;
      } else return undefined;
    })
    .filter((item: any) => item !== undefined);

  let allcustomfileds: any = [];

  //TODO find a better approach to do this.
  customfieldsdata.forEach((item: any) => {
    const keys = Object.keys(item);
    keys.forEach((fields: any) => {
      item[fields].forEach((keyValue: any) => {
        allcustomfileds.push(keyValue);
      });
    });
  });
  return uniqBy(allcustomfileds, "key");
};

export const singularPartialKey = {
  scm_file_repo_ids: "scm_file_repo_id",
  repo_ids: "repo_id"
};
