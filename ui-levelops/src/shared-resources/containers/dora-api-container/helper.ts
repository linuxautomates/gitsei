import { RestWorkflowProfile, WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { JIRA_SCM_COMMON_PARTIAL_FILTER_KEY, PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { JENKINS_CICD_FILTER_KEY_MAPPING } from "dashboard/reports/jenkins/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { cloneDeep, get, unset } from "lodash";
import { ROLLBACK_KEY } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { dateRangeFilterValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { CALCULATION_RELEASED_IN_KEY } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const extractFilterKeys = (filterObject: any) => {
  if (!filterObject) return [];
  return [
    ...Object.keys(filterObject || {}),
    ...Object.keys(filterObject.exclude || {}),
    ...Object.keys(filterObject.partial_match || {})
  ].filter((key: string) => key !== "exclude" && key !== "partial_match");
};

export const extractProfileFilter = (workflowProfileObj: RestWorkflowProfile, reportType: string) => {
  let profileFilter: string[] = [];
  switch (reportType) {
    case "deployment_frequency_report":
      const { deployment_frequency } = workflowProfileObj.deployment_frequency.filter;
      profileFilter =
        deployment_frequency.integration_type === WorkflowIntegrationType.IM ||
        deployment_frequency.integration_type === WorkflowIntegrationType.CICD
          ? extractFilterKeys(deployment_frequency.filter)
          : [];
      break;
    case "change_failure_rate":
      const { failed_deployment, total_deployment } = workflowProfileObj.change_failure_rate.filter;
      const failed_deployment_filter =
        failed_deployment.integration_type === WorkflowIntegrationType.IM ||
        failed_deployment.integration_type === WorkflowIntegrationType.CICD
          ? extractFilterKeys(failed_deployment.filter)
          : [];
      const total_deployment_filter =
        total_deployment.integration_type === WorkflowIntegrationType.IM ||
        total_deployment.integration_type === WorkflowIntegrationType.CICD
          ? extractFilterKeys(total_deployment.filter)
          : [];
      profileFilter = [...failed_deployment_filter, ...total_deployment_filter];
  }
  return profileFilter;
};

export const findIntegrationInOrg = (orgUnit: any, profileIntId: string) => {
  if (profileIntId && orgUnit?.sections?.length > 0)
    return orgUnit?.sections.find((section: any) => Object.keys(section.integrations).includes(`${profileIntId}`));
  return undefined;
};

export const etractOrgFilters = (profileIntId: string, orgUnit: any) => {
  const intSection = findIntegrationInOrg(orgUnit, profileIntId);
  if (intSection) {
    const orgIntegration = intSection.integrations[`${profileIntId}`];
    return extractFilterKeys(orgIntegration?.filters);
  }
  return [];
};

export const hasCommonString = (array1: string[], array2: string[]) =>
  array1?.some((item: string) => array2?.includes(item));

export const changeCustomFieldPrefix = (finalFilters: any, reportType: string) => {
  let _finalFilters = cloneDeep(finalFilters);
  const customFields = get(_finalFilters, ["filter", "custom_fields"], {});
  const excludeFields = get(_finalFilters, ["filter", "exclude"], {});
  const excludeCustomFields = get(_finalFilters, ["filter", "exclude", "custom_fields"], {});
  const partialFilterKey = getWidgetConstant(reportType, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
  let partialFields = get(_finalFilters, ["filter", partialFilterKey], {});
  let custom_field_prefix = "workitem_";
  let custom_field_key = "workitem_custom_fields";

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
    _finalFilters = {
      ...(_finalFilters || {}),
      filter: {
        ...(_finalFilters?.filter || {}),
        [partialFilterKey]: {
          ...(partialFields || {})
        }
      }
    };
  }
  if (Object.keys(customFields).length > 0 || _finalFilters.filter.hasOwnProperty("custom_fields")) {
    delete _finalFilters.filter.custom_fields;
    _finalFilters = {
      ...(_finalFilters || {}),
      filter: {
        ...(_finalFilters?.filter || {}),
        [custom_field_key]: {
          ...(customFields || {})
        }
      }
    };
  }
  if (Object.keys(excludeFields).length > 0 && excludeFields?.custom_fields) {
    delete _finalFilters.filter.exclude.custom_fields;
    _finalFilters = {
      ...(_finalFilters || {}),
      filter: {
        ...(_finalFilters?.filter || {}),
        exclude: {
          ..._finalFilters.filter.exclude,
          [custom_field_key]: { ...excludeCustomFields }
        }
      }
    };
  }
  return _finalFilters;
};

export const changeIterationKey = (filters: any) => {
  let finalFilters = { ...filters };
  const azureIterationValues: any = get(finalFilters, ["filter", "azure_iteration"], undefined);
  const excludeAzureIterationValues: any = get(finalFilters, ["filter", "exclude", "azure_iteration"], undefined);
  const partialAzureIterationValue: any = get(finalFilters, ["filter", "partial_match", "azure_iteration"], undefined);
  if (excludeAzureIterationValues) {
    const newExcludeAzureIterationValues = excludeAzureIterationValues.map((value: any) => {
      if (typeof value === "object") {
        return `${value.parent}\\${value.child}`;
      } else {
        // This is just for backward compatibility with old version that had string values
        return value;
      }
    });
    let key = "workitem_sprint_full_names";
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
    unset(finalFilters, ["filter", "exclude", "azure_iteration"]);
  }

  if (partialAzureIterationValue) {
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
    unset(finalFilters, ["filter", "partial_match", "azure_iteration"]);
  }
  if (azureIterationValues) {
    const newAzureIterationValues = azureIterationValues.map((value: any) => {
      if (typeof value === "object") {
        return `${value.parent}\\${value.child}`;
      } else {
        return value;
      }
    });
    let key = "workitem_sprint_full_names";

    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...(finalFilters?.filter || {}),
        [key]: newAzureIterationValues
      }
    };
    unset(finalFilters, ["filter", "azure_iteration"]);
  }
  return finalFilters;
};

export const changeAzureAreaKey = (filters: any) => {
  let finalFilters = cloneDeep(filters);
  const azureCodeAreaValues: any = get(finalFilters, ["filter", "workitem_attributes", "code_area"], undefined);
  if (azureCodeAreaValues) {
    const newAzureCodeAreaValues = azureCodeAreaValues.map((value: any) => {
      if (typeof value === "object") {
        return `${value.child}`;
      } else {
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
  return finalFilters;
};

export const extractProfileType = (workflowProfileObj: RestWorkflowProfile, reportType: string) => {
  let profileType: string = "";
  switch (reportType) {
    case "deployment_frequency_report":
      const { deployment_frequency } = workflowProfileObj.deployment_frequency.filter;
      profileType = deployment_frequency.integration_type;
      break;
    case "change_failure_rate":
      const { failed_deployment } = workflowProfileObj.change_failure_rate.filter;
      profileType = failed_deployment.integration_type;
  }
  return profileType;
};

export const hasCommonCicdFilter = (orgFilter: string[], profileType: string, profileEventValue: any) =>
  profileType === WorkflowIntegrationType.CICD &&
  orgFilter?.includes(JENKINS_CICD_FILTER_KEY_MAPPING.job_normalized_full_name) &&
  profileEventValue &&
  profileEventValue.length > 0
    ? true
    : false;

export const extractProfileEventValue = (workflowProfileObj: any, reportType: string) => {
  let eventValue;
  switch (reportType) {
    case "deployment_frequency_report":
      const { deployment_frequency } = workflowProfileObj.deployment_frequency.filter;
      eventValue = deployment_frequency?.event?.values;
      break;
    case "change_failure_rate":
      const { failed_deployment, total_deployment } = workflowProfileObj.change_failure_rate.filter;
      eventValue = [...(failed_deployment?.event?.values || []), ...(total_deployment?.event?.values || [])];
      break;
  }
  return eventValue;
};

export const getDoraProfileIntegrationApplication = (reportType: string, workflowProfile: RestWorkflowProfile) => {
  const getdoraProfileApplication = get(
    widgetConstants,
    [reportType as string, "getDoraProfileIntegrationApplication"],
    undefined
  );
  if (getdoraProfileApplication) {
    return getdoraProfileApplication({ workspaceOuProfilestate: workflowProfile, reportType: reportType });
  }
};

export const updateRollback = (widgetNewFilterParam: any) => {
  let widgetNewFilter = widgetNewFilterParam;
  if (widgetNewFilter.hasOwnProperty(ROLLBACK_KEY))
    widgetNewFilter = {
      ...widgetNewFilter,
      [ROLLBACK_KEY]: widgetNewFilter[ROLLBACK_KEY] === "true"
    };

  if (widgetNewFilter?.exclude && widgetNewFilter?.exclude.hasOwnProperty(ROLLBACK_KEY))
    widgetNewFilter = {
      ...widgetNewFilter,
      exclude: {
        ...widgetNewFilter.exclude,
        [ROLLBACK_KEY]: widgetNewFilter.exclude[ROLLBACK_KEY] === "true"
      }
    };
  return widgetNewFilter;
};

export const getDateTime = (
  dashboardMetaData: any,
  widgetMetaData: any,
  widgetTimeRange?: {
    $lt: string;
    $gt: string;
  }
) => {
  const dashBoard_time_keys = get(widgetMetaData, "dashBoard_time_keys", {});
  const useDashboardTime = get(dashBoard_time_keys, ["time_range", "use_dashboard_time"], false);
  if (!useDashboardTime && widgetTimeRange) return widgetTimeRange;
  return !dashboardMetaData.dashboard_time_range_filter ||
    typeof dashboardMetaData.dashboard_time_range_filter === "string"
    ? dateRangeFilterValue(dashboardMetaData.dashboard_time_range_filter || "last_30_days")
    : dashboardMetaData.dashboard_time_range_filter;
};

export const extractProfileCalculationField = (workflowProfileObj: RestWorkflowProfile, reportType: string) => {
  let calculationField: string | undefined = "";
  switch (reportType) {
    case "deployment_frequency_report":
      const { deployment_frequency } = workflowProfileObj.deployment_frequency.filter;
      calculationField = deployment_frequency?.calculation_field;
      break;
    case "change_failure_rate":
      const { failed_deployment } = workflowProfileObj.change_failure_rate.filter;
      calculationField = failed_deployment?.calculation_field;
  }
  return calculationField;
};

export const hasAppliedFilterOnWidgetForJira = (
  profileType: string,
  profileApplication: string,
  profileCalculationFiled: string | undefined,
  widgetFilter: string[]
) => {
  if (
    profileType === WorkflowIntegrationType.IM &&
    profileApplication === IntegrationTypes.JIRA &&
    profileCalculationFiled === CALCULATION_RELEASED_IN_KEY
  ) {
    let checkForFilter = widgetFilter.filter(filter => filter !== "time_range");
    return checkForFilter?.length;
  }
  return false;
};
