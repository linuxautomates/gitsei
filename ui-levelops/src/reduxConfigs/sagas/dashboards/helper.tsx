import React from "react";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { timeStampToValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { valuesToFilters } from "dashboard/constants/constants";
import { cloneDeep, get } from "lodash";
import {
  API_FILTER_REQUIREMENT_INITIAL_STATE,
  COMMITTER_FIELDS,
  COMMITTER_SUPPORTER_FILTERS,
  SCM_COMMITTER_FILTERS_SUPPORTED_APPLICATION,
  SCM_FIELDS,
  SCM_SUPPORTED_FILTERS
} from "./constants";
import { API_REQUEST, API_REQUIREMENTS_TYPE, OU_Integration } from "./types";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { ROLLBACK_KEY } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { DateFormats } from "utils/dateUtils";
import { getIsStandaloneApp } from "helper/helper";
import LabelWithLock from "shared-resources/components/LabelWithLock/LabelWithLock";

export const getWidgetTimeConstant = (widgetMetaData: any, useDashboardTimeValue: boolean) => {
  return Object.keys(widgetMetaData?.dashBoard_time_keys || {}).reduce((acc: any, next: any) => {
    return { ...acc, [next]: { use_dashboard_time: useDashboardTimeValue } };
  }, {});
};

export const getTransformedOUs = (apiOUs: any[], allowedOUs: string[]) => {
  return (apiOUs || [])
    .map((rootOu: any) => ({
      ...rootOu,
      label: getIsStandaloneApp() ? (
        rootOu.name
      ) : (
        <LabelWithLock label={rootOu.name} isLocked={!rootOu.access_response?.view} />
      ),
      value: rootOu?.ou_id,
      disabled: allowedOUs.length ? !allowedOUs.includes(rootOu?.id) : false
    }))
    .sort(stringSortingComparator());
};

export const getAllChildren = (root: any, childs: any) => {
  if (root === null || childs?.length === 0 || root === undefined) {
    return null;
  }
  const rootChilds = childs.filter((ou: any) => ou?.parent_ref_id == root?.id);
  const nonrootChilds = childs.filter((ou: any) => ou?.parent_ref_id != root?.id);
  const newRoot = { ...root };
  newRoot.children = rootChilds;
  (newRoot?.children || [])?.forEach((element: any) => {
    const children = getAllChildren(element, nonrootChilds);
    if (children && children?.length) {
      element.children = children;
    }
  });

  return newRoot.children;
};

export const getApplicationKey = (application: string, filterKey: string) => {
  if (SCM_COMMITTER_FILTERS_SUPPORTED_APPLICATION.includes(application) && SCM_SUPPORTED_FILTERS.includes(filterKey)) {
    return `${application}_scm`;
  }
  if (
    SCM_COMMITTER_FILTERS_SUPPORTED_APPLICATION.includes(application) &&
    COMMITTER_SUPPORTER_FILTERS.includes(filterKey)
  ) {
    return `${application}_committer`;
  }
  return application;
};

export const ApiFilterKeys: any = {
  jira: {
    keys: ["reporters", "assignees"]
  },
  github_committer: {
    keys: COMMITTER_SUPPORTER_FILTERS
  },
  github_scm: {
    keys: SCM_SUPPORTED_FILTERS
  },
  azure_devops: {
    keys: ["workitem_assignees", "workitem_reporters"]
  },
  azure_devops_scm: {
    keys: SCM_SUPPORTED_FILTERS
  },
  azure_devops_committer: {
    keys: COMMITTER_SUPPORTER_FILTERS
  },
  gitlab_committer: {
    keys: COMMITTER_SUPPORTER_FILTERS
  },
  gitlab_scm: {
    keys: SCM_SUPPORTED_FILTERS
  },
  bitbucket_server_scm: {
    keys: SCM_SUPPORTED_FILTERS
  },
  bitbucket_server_committer: {
    keys: COMMITTER_SUPPORTER_FILTERS
  },
  gerrit_scm: {
    keys: SCM_SUPPORTED_FILTERS
  },
  gerrit_committer: {
    keys: COMMITTER_SUPPORTER_FILTERS
  },
  pagerduty: {
    keys: ["pd_service_ids", "user_ids"]
  },
  helix_scm: {
    keys: SCM_SUPPORTED_FILTERS
  },
  helix_committer: {
    keys: COMMITTER_SUPPORTER_FILTERS
  }
};

const getTimeFilterValue = (value: any) => {
  if (!Array.isArray(value) && value.hasOwnProperty("$age")) {
    const numOfDays = Math.round(parseInt(value?.["$age"]) / 86400);
    return numOfDays > 1 ? `Last ${numOfDays} Days` : `Last ${numOfDays} Day`;
  } else if (!Array.isArray(value) && value.hasOwnProperty("$lt") && value.hasOwnProperty("$gt")) {
    const momentValue = [timeStampToValue(value.$gt, DateFormats.DAY), timeStampToValue(value.$lt, DateFormats.DAY)];
    return `${momentValue[0]} - ${momentValue[1]}`;
  }
  return value;
};
export const getIntegrationFilters = (
  filters: any,
  customFieldOptions: any[],
  application: string,
  apiFiltersData: any
) => {
  const customFieldKey = application === IntegrationTypes.JIRA ? "custom_fields" : "workitem_custom_fields";
  const partialCustomKey = application === IntegrationTypes.JIRA ? "customfield_" : "Custom.";
  let allFilters: any = [];
  if (filters && Object.keys(filters)) {
    const normalFiltersKeys = Object.keys(filters).filter(
      (key: string) =>
        ![
          "partial_match",
          "custom_fields",
          "workitem_custom_fields",
          "exclude",
          "workitem_attributes",
          ROLLBACK_KEY
        ].includes(key)
    );
    let transformedFilters = normalFiltersKeys?.map((key: string) => {
      const applicationKey = getApplicationKey(application, key);
      const keyIsApiFilterKey = ApiFilterKeys?.[applicationKey]?.keys?.includes(key) ? true : false;
      let value = filters?.[key] || undefined;
      if (keyIsApiFilterKey && Array.isArray(value)) {
        const apiData = get(apiFiltersData, [applicationKey, key], []);
        value = value.map((val: any) => apiData.find((data: any) => data?.key === val)?.additional_key);
      } else if (key === "workitem_priorities" && Array.isArray(value)) {
        value = value.map((val: any) => get(staticPriorties, [val], val));
      } else {
        value = getTimeFilterValue(value);
      }
      return {
        label: key,
        key: key,
        value: value,
        type: "normal"
      };
    });
    allFilters = [...allFilters, ...(transformedFilters || [])];
    if (filters.hasOwnProperty("workitem_attributes")) {
      const attributesFiltersKeys = Object.keys(filters["workitem_attributes"] || {});
      const attributesFilters = attributesFiltersKeys.map((key: string) => {
        const value = get(filters, ["workitem_attributes", key], []);
        return {
          label: key,
          value: value,
          key: key,
          type: "normal"
        };
      });
      allFilters = [...allFilters, ...(attributesFilters || [])];
    }
    if (filters.hasOwnProperty(customFieldKey)) {
      const customFieldsKeys = Object.keys(filters[customFieldKey] || {});
      const customFieldsFilters = customFieldsKeys.map((key: string) => {
        const option = customFieldOptions.find((option: any) => option.field_key === key);
        let value = filters?.[customFieldKey]?.[key];
        if (!Array.isArray(value)) {
          value = getTimeFilterValue(value);
        }
        return {
          label: option?.name,
          value: value,
          key: key,
          type: "normal"
        };
      });
      allFilters = [...allFilters, ...customFieldsFilters];
    }
    if (filters.hasOwnProperty("partial_match")) {
      const partialKeys = Object.keys(filters["partial_match"] || {});
      const customPartialKeys = partialKeys.filter((key: string) => {
        const customFieldkeys = customFieldOptions.map((item: any) => item.field_key);
        return customFieldkeys.includes(key);
      });
      const normalPartialKeys = partialKeys.filter((key: string) => !customPartialKeys.includes(key));
      const customFilters = customPartialKeys.map((key: string) => {
        const option = customFieldOptions.find((option: any) => option.field_key === key);
        const value = filters?.["partial_match"]?.[key];
        const startWith = value?.hasOwnProperty("$begins") ? "STARTS" : "CONTAINS";
        return {
          label: option?.name,
          value: value?.hasOwnProperty("$begins") ? value?.["$begins"] : value?.["$contains"],
          key: key,
          type: startWith
        };
      });
      allFilters = [...allFilters, ...(customFilters || [])];
      const normalPartialFilters = normalPartialKeys.map((key: string) => {
        const value = filters?.["partial_match"]?.[key];
        const startWith = value?.hasOwnProperty("$begins") ? "STARTS" : "CONTAINS";
        return {
          label: key,
          value: value.hasOwnProperty("$begins") ? value["$begins"] : value["$contains"],
          key: key,
          type: startWith
        };
      });
      allFilters = [...allFilters, ...(normalPartialFilters || [])];
    }
    if (filters.hasOwnProperty("exclude")) {
      const normalExcludeFiltersKeys = Object.keys(filters["exclude"] || {}).filter(
        (key: string) => !["custom_fields", "workitem_attributes", "workitem_custom_fields", ROLLBACK_KEY].includes(key)
      );
      const normalExcludeFilters = normalExcludeFiltersKeys.map((key: string) => {
        const applicationKey = getApplicationKey(application, key);
        const keyIsApiFilterKey = ApiFilterKeys?.[applicationKey]?.keys?.includes(key) ? true : false;
        let value = filters?.["exclude"]?.[key] || undefined;
        if (keyIsApiFilterKey && Array.isArray(value)) {
          const apiData = get(apiFiltersData, [applicationKey, key], []);
          value = value.map((val: any) => apiData.find((data: any) => data?.key === val)?.additional_key);
        }
        if (key === "workitem_priorities" && Array.isArray(value)) {
          value = value.map((val: any) => get(staticPriorties, [val], val));
        }
        return {
          label: key,
          value: value,
          type: "exclude",
          key: key
        };
      });
      allFilters = [...allFilters, ...(normalExcludeFilters || [])];
      if (filters.exclude && filters["exclude"].hasOwnProperty("workitem_attributes")) {
        const attributesFiltersKeys = Object.keys(filters["exclude"]["workitem_attributes"] || {});
        const attributesFilters = attributesFiltersKeys.map((key: string) => {
          const value = get(filters, ["exclude", "workitem_attributes", key], []);
          return {
            label: key,
            value: value,
            key: key,
            type: "exclude"
          };
        });
        allFilters = [...allFilters, ...(attributesFilters || [])];
      }
      if (filters?.exclude?.hasOwnProperty(customFieldKey)) {
        const customFieldsKeys = Object.keys(filters["exclude"][customFieldKey] || {});
        const customFieldsFilters = customFieldsKeys.map((key: string) => {
          const option = customFieldOptions.find((option: any) => option.field_key === key);
          const value = filters?.["exclude"]?.[customFieldKey]?.[key];
          return {
            label: option?.name,
            value: value,
            key: key,
            type: "exclude"
          };
        });
        allFilters = [...allFilters, ...customFieldsFilters];
      }
      if (filters?.exclude?.hasOwnProperty(ROLLBACK_KEY)) {
        const value = get(filters, ["exclude", ROLLBACK_KEY], "")?.toString();
        const rollbackArr = [
          {
            label: ROLLBACK_KEY,
            key: ROLLBACK_KEY,
            value: value,
            type: "exclude"
          }
        ];
        allFilters = [...allFilters, ...(rollbackArr || [])];
      }
    }
    if (filters?.hasOwnProperty(ROLLBACK_KEY)) {
      const value = get(filters, [ROLLBACK_KEY], "")?.toString();
      const rollbackArr = [
        {
          label: ROLLBACK_KEY,
          key: ROLLBACK_KEY,
          value: value,
          type: "normal"
        }
      ];
      allFilters = [...allFilters, ...(rollbackArr || [])];
    }
    return allFilters;
  } else {
    return [];
  }
};

export const getApiFilterRequired = (filtersKeys: string[], excludeFilterKeys: string[], applicationKey: string) => {
  const apiFilters = ApiFilterKeys?.[applicationKey]?.keys;
  const normalRequired = filtersKeys.filter((key: any) => apiFilters.includes(key));
  const excludeRequired = excludeFilterKeys.filter((key: string) => apiFilters.includes(key));
  if (normalRequired?.length || excludeRequired.length) {
    return true;
  }
  return false;
};

export const getApiFiltersData = (jiraFieldsState: any) => {
  const data = get(jiraFieldsState, ["data", "records"], {});
  const jiraApiFiltersData = data.reduce((acc: any, next: any) => {
    const key = Object.keys(next)[0];
    const Modifieldkey = get(valuesToFilters, key, key);
    return { ...acc, [Modifieldkey]: next[key] };
  }, {});
  return jiraApiFiltersData;
};

export const getApiFiltersRequirement = (integrations: OU_Integration[]) => {
  let apiRequirement: API_REQUIREMENTS_TYPE = { ...API_FILTER_REQUIREMENT_INITIAL_STATE };
  integrations?.forEach((integration: OU_Integration) => {
    const filters = integration?.filters || {};
    const filtersKeys = Object.keys(filters);
    const excludeFilterKeys = Object.keys(filters?.["exclude"] || {});
    const partialFilterKeys = Object.keys(filters?.["partial_match"] || {});
    const jiraCustomPartialKeys = partialFilterKeys.filter((key: string) => key?.includes("customfield_"));
    if (
      integration.type === IntegrationTypes.JIRA &&
      ([...filtersKeys, ...excludeFilterKeys].includes("custom_fields") || jiraCustomPartialKeys)
    ) {
      apiRequirement = {
        ...apiRequirement,
        jiraCustomFieldsRequired: true
      };
    }
    if (integration.type === IntegrationTypes.AZURE) {
      apiRequirement = {
        ...apiRequirement,
        AzureCustomFieldsRequired: true
      };
    }

    if (integration.type === IntegrationTypes.JIRA && !apiRequirement?.jiraApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        jiraApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, integration.type)
      };
    }

    if (integration.type === IntegrationTypes.AZURE && !apiRequirement?.azureApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        azureApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, integration.type)
      };
    }

    if (integration.type === IntegrationTypes.AZURE && !apiRequirement?.azureSCMApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        azureSCMApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "azure_devops_scm")
      };
    }

    if (integration.type === IntegrationTypes.AZURE && !apiRequirement?.azureCommitterApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        azureCommitterApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "azure_devops_committer")
      };
    }

    if (integration.type === IntegrationTypes.GITHUB && !apiRequirement?.githubSCMApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        githubSCMApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "github_scm")
      };
    }

    if (integration.type === IntegrationTypes.GITHUB && !apiRequirement?.githubCommitterApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        githubCommitterApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "github_committer")
      };
    }

    if (integration.type === IntegrationTypes.GITLAB && !apiRequirement?.gitlabCommitterApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        gitlabCommitterApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "gitlab_committer")
      };
    }

    if (integration.type === IntegrationTypes.GITLAB && !apiRequirement?.gitlabSCMApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        gitlabSCMApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "gitlab_scm")
      };
    }
    if (integration.type === IntegrationTypes.BITBUCKET_SERVER && !apiRequirement?.bitbucketScmApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        bitbucketScmApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "bitbucket_server_scm")
      };
    }

    if (
      integration.type === IntegrationTypes.BITBUCKET_SERVER &&
      !apiRequirement?.bitbucketCommitterApiFiltersRequired
    ) {
      apiRequirement = {
        ...apiRequirement,
        bitbucketCommitterApiFiltersRequired: getApiFilterRequired(
          filtersKeys,
          excludeFilterKeys,
          "bitbucket_server_committer"
        )
      };
    }

    if (integration.type === IntegrationTypes.GERRIT && !apiRequirement?.gerritScmApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        gerritScmApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "gerrit_scm")
      };
    }

    if (integration.type === IntegrationTypes.GERRIT && !apiRequirement?.gerritCommitterApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        gerritCommitterApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "gerrit_committer")
      };
    }
    if (integration.type === IntegrationTypes.PAGERDUTY && !apiRequirement?.pagerdutyApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        pagerdutyApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, integration.type)
      };
    }
    if (integration.type === IntegrationTypes.HELIX && !apiRequirement?.helixScmApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        helixScmApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "helix_scm")
      };
    }
    if (integration.type === IntegrationTypes.HELIX && !apiRequirement?.helixCommitterApiFiltersRequired) {
      apiRequirement = {
        ...apiRequirement,
        helixCommitterApiFiltersRequired: getApiFilterRequired(filtersKeys, excludeFilterKeys, "helix_committer")
      };
    }
    if (integration?.users && integration.users.length && !apiRequirement.usersApiRequirement) {
      apiRequirement = {
        ...apiRequirement,
        usersApiRequirement: true
      };
    }
  });

  return apiRequirement;
};

export const getApiRequest = (integrations: any[], application: string, uri: string, key: string, fields: any) => {
  const filteredIntegrations = integrations.filter((integration: any) => integration.type.includes(application));
  const ids = filteredIntegrations.map((integration: any) => integration.id)?.sort((a: any, b: any) => a - b) || [];
  const apiRequest = {
    key: key,
    uri: uri,
    method: "list",
    filters: {
      fields: fields,
      filter: { integration_ids: ids },
      integration_ids: ids
    },
    complete: `${ids?.join("_")}_${key}_complete_id`,
    uuid: ids?.join("_")
  };
  return apiRequest;
};

export const getCustomFieldApiRequest = (integrations: any[], application: string, uri: string, key: string) => {
  const filteredIntegrations = integrations.filter((integration: any) => integration.type.includes(application));
  const ids = filteredIntegrations.map((integration: any) => integration.id)?.sort((a: any, b: any) => a - b) || [];
  const apiRequest = {
    key: key,
    uri: uri,
    method: "list",
    filters: {
      filter: { integration_ids: ids }
    },
    complete: `${ids?.join("_")}_${key}_complete_id`,
    uuid: ids?.join("_")
  };
  return apiRequest;
};

export const getApiCalls = (integrations: OU_Integration[], apiRequirement: API_REQUIREMENTS_TYPE) => {
  let apiCalls: API_REQUEST[] = [];

  if (apiRequirement?.jiraCustomFieldsRequired) {
    const apiRequest = getCustomFieldApiRequest(
      integrations,
      IntegrationTypes.JIRA,
      "jira_fields",
      "jira_custom_fields"
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.AzureCustomFieldsRequired) {
    const apiRequest = getCustomFieldApiRequest(
      integrations,
      IntegrationTypes.AZURE,
      "issue_management_workItem_Fields_list",
      "azure_custom_fields"
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.jiraApiFiltersRequired) {
    const fields = ["reporter", "assignee"];
    const apiRequest = getApiRequest(integrations, IntegrationTypes.JIRA, "jira_filter_values", "jira_values", fields);
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.azureApiFiltersRequired) {
    const fields = ["reporter", "assignee"];
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.AZURE,
      "issue_management_workitem_values",
      "azure_values",
      fields
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.azureSCMApiFiltersRequired) {
    const fields = ["assignee", "creator", "approver", "reviewer"];
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.AZURE,
      "github_prs_filter_values",
      "azure_scm_values",
      fields
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.azureCommitterApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.AZURE,
      "github_commits_filter_values",
      "azure_committer_values",
      COMMITTER_FIELDS
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.githubSCMApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.GITHUB,
      "github_prs_filter_values",
      "github_scm_values",
      SCM_FIELDS
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.githubCommitterApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.GITHUB,
      "github_commits_filter_values",
      "github_committer_values",
      COMMITTER_FIELDS
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.gitlabCommitterApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.GITLAB,
      "github_commits_filter_values",
      "gitlab_committer_values",
      COMMITTER_FIELDS
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.gitlabSCMApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.GITLAB,
      "github_prs_filter_values",
      "gitlab_scm_values",
      SCM_FIELDS
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.bitbucketScmApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.BITBUCKET_SERVER,
      "github_prs_filter_values",
      "bitbucket_server_scm_values",
      SCM_FIELDS
    );
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.bitbucketCommitterApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.BITBUCKET_SERVER,
      "github_commits_filter_values",
      "bitbucket_server_committer_values",
      COMMITTER_FIELDS
    );
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.gerritCommitterApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.GERRIT,
      "github_commits_filter_values",
      "gerrit_committer_values",
      COMMITTER_FIELDS
    );
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.gerritScmApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.GERRIT,
      "github_prs_filter_values",
      "gerrit_scm_values",
      SCM_FIELDS
    );
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.pagerdutyApiFiltersRequired) {
    const fields = ["pd_service", "user_id"];
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.PAGERDUTY,
      "pagerduty_filter_values",
      "pagerduty_values",
      fields
    );
    apiCalls.push(apiRequest);
  }

  if (apiRequirement?.helixCommitterApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.HELIX,
      "github_commits_filter_values",
      "helix_committer_values",
      COMMITTER_FIELDS
    );
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.helixScmApiFiltersRequired) {
    const apiRequest = getApiRequest(
      integrations,
      IntegrationTypes.HELIX,
      "github_prs_filter_values",
      "helix_scm_values",
      SCM_FIELDS
    );
    apiCalls.push(apiRequest);
  }
  if (apiRequirement?.usersApiRequirement) {
    const apiRequest = {
      key: "org_users",
      uri: "org_users",
      method: "list",
      filters: {
        page: 0,
        page_size: 1000
      },
      complete: `org_users_complete_id`,
      uuid: "ou_defination_users"
    };
    apiCalls.push(apiRequest);
  }
  return apiCalls;
};

export const getAllUsers = (users: string[], usersApiData: any[]) => {
  const allUsers = (users || [])?.map((id: string) => {
    return (usersApiData || [])?.find((user: any) => user?.id === id)?.full_name;
  });
  return allUsers;
};

export const getAllAncestors = (childOUarray: any[], allOus: any[], childOU: any) => {
  if (!childOU?.parent_ref_id) {
    return childOUarray;
  }
  const parentOu = allOus.find((ou: any) => ou.id == childOU.parent_ref_id);
  let _childouArray = [...(childOUarray || [])];
  if (parentOu) {
    _childouArray.push(parentOu);
    _childouArray = getAllAncestors(_childouArray, allOus, parentOu);
  }
  return _childouArray;
};
