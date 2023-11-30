import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import {
  defaultUserSectionConfig,
  managersConfigType,
  orgRestSectionType,
  orgUnitJSONType,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import { integrationParameters } from "configurations/constants";
import { applicationType } from "configurations/pages/integrations/integration-table-config";
import {
  AZURE_CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_PREFIX,
  valuesToFilters
} from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { extractFilterAPIData, genericGetFilterAPIData } from "dashboard/report-filters/helper";
import { capitalize, cloneDeep, forEach, get, uniq, uniqBy, unset } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { MeResponse } from "reduxConfigs/types/response/me.response";
import { sanitizeObject, sanitizeObjectCompletely } from "utils/commonUtils";
import { orgFilterNameMapping } from "../Constants";
import { ROLLBACK_KEY } from "../Filters/harnessng/harnessng-job-filter.config";
import OrganisationFilterSelect from "../Filters/organisationFilterSelect";

const getFilterKey = (key: string) => {
  const filterKey = Object.keys(valuesToFilters || {}).find((item: string) => get(valuesToFilters, item) === key);

  // This check is added because valuesToFilters have multiple keys for job_normalized_full_names
  if (key === "job_normalized_full_names") {
    key = "job_normalized_full_name";
  } else if (!!filterKey) {
    key = filterKey;
  }
  return key;
};

export const getOrgIntegrationFilterOptions = () => {
  const applications = applicationType()
    .map(integrationOption => integrationOption?.value)
    .filter(value => {
      let valid = false;
      forEach(Object.values(widgetConstants), report => {
        if ((report as any).application === value) {
          valid = true;
        }
      });
      return valid;
    });
  return applications.map((application: string) => ({
    label: capitalize(application.replace(/-|_/g, " ")),
    value: application
  }));
};

export const getOUTransformedFilterValuesOptions = (
  records: any[],
  customFieldMapping?: { key: string; name: string }[]
) => {
  return (records || []).map(record => {
    let key: string = Object.keys(record)[0];
    let label = get(orgFilterNameMapping, key, capitalize(key.replace(/_/g, " ")));
    if (customFieldMapping?.length) {
      const keyMapping = customFieldMapping.find(obj => obj.key === key);
      if (keyMapping) {
        label = keyMapping.name || "";
      }
    }
    const values = (record[key] || [])
      .map((record: any) => {
        if (typeof record === "object") {
          return {
            label: record?.additional_key ?? record?.key ?? record?.name,
            value: record?.key ?? record?.id
          };
        }
        return {
          label: record,
          value: record
        };
      })
      .filter((value: { label: string | undefined; value: string | undefined }) => !!value?.label && !!value?.value);
    return { label, value: get(valuesToFilters, [key], key), options: values };
  });
};

export const getManagerOptions = (managers: managersConfigType[]) => {
  return managers.map((record: any) => ({
    label: record?.full_name,
    value: record?.id
  }));
};

export const getAdminUsersOptions = (adminUsers: (MeResponse & { name: string })[]) => {
  return adminUsers.map(record => ({
    label: record?.name,
    value: record?.id
  }));
};

export const getUserFromCSV = (data: string[][], userList: managersConfigType[]) => {
  let users: string[] = [];
  if (!data?.length) return false;

  const emailIndex = data[0].findIndex(key => (key || "").toLowerCase() === "email");
  if (emailIndex === -1) return false;

  for (let i = 1; i < data.length; i++) {
    const email = data[i][emailIndex];
    if (!email) continue;
    const user = userList.find(user => user?.email === email);
    if (!user) {
      return false;
    }

    users.push(user?.id || "");
  }
  return uniq(users);
};

export const getDynamicDefinationOrFilters = (definitions: sectionSelectedFilterType[]) => {
  let exclude: any = {};
  let normal: any = {};
  let partial: any = {};

  forEach(definitions, definition => {
    let payloadKey = definition?.key;
    if (definition?.metadata?.transformed) {
      payloadKey = payloadKey.replace(definition?.metadata?.transformed, "");
    }
    switch (definition?.param) {
      case integrationParameters.EQUALS:
        if ((definition?.key || "").includes(CUSTOM_FIELD_PREFIX)) {
          normal = {
            ...normal,
            custom_fields: {
              ...get(normal, ["custom_fields"], {}),
              [definition?.key]: definition?.value
            }
          };
        } else if ((definition?.key || "").includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          normal = {
            ...normal,
            workitem_custom_fields: {
              ...get(normal, ["workitem_custom_fields"], {}),
              [payloadKey]: definition?.value
            }
          };
        } else if (["teams", "code_area"].includes(definition?.key || "")) {
          normal = {
            ...normal,
            workitem_attributes: {
              ...get(normal, ["workitem_attributes"], {}),
              [definition?.key]: definition?.value
            }
          };
        } else if ([ROLLBACK_KEY].includes(definition?.key || "")) {
          normal = {
            ...normal,
            [definition?.key]: definition?.value === "true" ? true : false
          };
        } else {
          normal = {
            ...normal,
            [definition?.key]: definition?.value
          };
        }
        break;
      case integrationParameters.DOES_NOT_EQUAL:
        if ((definition?.key || "").includes(CUSTOM_FIELD_PREFIX)) {
          exclude = {
            ...exclude,
            custom_fields: {
              ...get(exclude, ["custom_fields"], {}),
              [definition?.key]: definition?.value
            }
          };
        } else if ((definition?.key || "").includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          exclude = {
            ...exclude,
            workitem_custom_fields: {
              ...get(exclude, ["workitem_custom_fields"], {}),
              [payloadKey]: definition?.value
            }
          };
        } else if (["teams", "code_area"].includes(definition?.key || "")) {
          exclude = {
            ...exclude,
            workitem_attributes: {
              ...get(exclude, ["workitem_attributes"], {}),
              [definition?.key]: definition?.value
            }
          };
        } else if ([ROLLBACK_KEY].includes(definition?.key || "")) {
          exclude = {
            ...exclude,
            [definition?.key]: definition?.value === "true" ? true : false
          };
        } else {
          exclude = {
            ...exclude,
            [definition?.key]: definition?.value
          };
        }
        break;
      case integrationParameters.STARTS_WITH:
        {
          const key = getFilterKey(payloadKey || "");
          partial = {
            ...partial,
            [key]: { $begins: definition?.value }
          };
        }
        break;

      case integrationParameters.CONTAINS:
        {
          const key = getFilterKey(payloadKey || "");
          partial = {
            ...partial,
            [key]: { $contains: definition?.value }
          };
        }
        break;
      case integrationParameters.IS_BETWEEN:
        const $gt_$lt: any = Object.keys(definition?.value || {}).length ? definition?.value : { $lt: "", $gt: "" };
        if ((definition?.key || "").includes(CUSTOM_FIELD_PREFIX)) {
          normal = {
            ...normal,
            custom_fields: {
              ...get(normal, ["custom_fields"], {}),
              [definition?.key]: $gt_$lt
            }
          };
        } else if ((definition?.key || "").includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          normal = {
            ...normal,
            workitem_custom_fields: {
              ...get(normal, ["workitem_custom_fields"], {}),
              [payloadKey]: $gt_$lt
            }
          };
        } else {
          normal = {
            ...normal,
            [definition?.key]: $gt_$lt
          };
        }
        break;
    }
  });

  return sanitizeObject({
    ...sanitizeObject(normal || {}),
    exclude: sanitizeObject(exclude || {}),
    partial_match: sanitizeObject(partial || {})
  });
};

const sanitizeManagers = (managers: managersConfigType[]) => {
  return managers.map(manager => ({
    id: manager?.id,
    email: manager?.email,
    full_name: manager?.full_name,
    version: manager?.version || ""
  }));
};

export const transformDraftOrgUnitData = (orgUnitToCreate: orgUnitJSONType) => {
  let newOrgUnit: any = {
    name: (orgUnitToCreate?.name || "").trim(),
    description: orgUnitToCreate?.description || "",
    tags: orgUnitToCreate?.tags || [],
    managers: sanitizeManagers(orgUnitToCreate?.managers || []),
    ou_category_id: orgUnitToCreate?.ou_category_id ?? "",
    parent_ref_id: orgUnitToCreate?.parent_ref_id,
    default_dashboard_id: orgUnitToCreate.default_dashboard_id
  };

  if (orgUnitToCreate?.id) {
    newOrgUnit = {
      ...newOrgUnit,
      id: orgUnitToCreate?.id
    };
  }

  const getDefaultSection = (defaultSection: defaultUserSectionConfig) => {
    let userIds: string[] = [];
    const getCSVUser = get(defaultSection, ["csv_users"], {});
    const users: string[] = get(defaultSection, ["users"], []);
    if (Object.keys(getCSVUser).length) {
      forEach(Object.values(getCSVUser), (value: any) => {
        userIds = [...userIds, ...(value || [])];
      });
    }
    if (users.length) {
      userIds = [...users];
    }
    let dynamicDefinitions: any = getDynamicDefinationOrFilters(get(defaultSection, ["dynamic_user_definition"], []));
    dynamicDefinitions = sanitizeObjectCompletely(dynamicDefinitions);

    if (Object.keys(dynamicDefinitions).length) {
      return {
        dynamic_user_definition: dynamicDefinitions
      };
    }
    return {
      users: userIds
    };
  };

  newOrgUnit["default_section"] = getDefaultSection(
    orgUnitToCreate?.default_section || { dynamic_user_definition: [], users: [], csv_users: {} }
  );

  let sections: orgRestSectionType[] = [];
  forEach(orgUnitToCreate?.sections || [], section => {
    let newSection: any = { id: section?.id };
    const applicationAndId: string[] = (section?.type || "").split("@");
    let typeId = applicationAndId[1];
    let type: string = applicationAndId[0];
    const filters = get(section, ["integration", "filters"], []);
    const filterObject = getDynamicDefinationOrFilters(filters);
    newSection["integrations"] = { [typeId]: { type: type, filters: filterObject } };
    const userGroup = (section?.user_groups || []).length
      ? (section?.user_groups[0] as defaultUserSectionConfig)
      : ({ dynamic_user_definition: [], users: [], csv_users: {} } as defaultUserSectionConfig);
    const users = getDefaultSection(userGroup);
    newSection = {
      ...(newSection || {}),
      ...(users || {})
    };
    sections.push(newSection);
  });

  newOrgUnit["sections"] = sections;
  return sanitizeObjectCompletely(newOrgUnit);
};

export const getAttributesForm = (defination: any, customFieldRecords: IntegrationTransformedCFTypes[] = []) => {
  let excludedStuff = get(defination, ["exclude"], {});
  const partialMatch = get(defination, "partial_match", {});
  const customFields = get(defination, ["custom_fields"], {});
  const excludeCustomFields = get(excludedStuff, ["custom_fields"], {});
  const azureCustomFields = get(defination, ["workitem_custom_fields"], {});
  const azureExcludeCustomFields = get(excludedStuff, ["workitem_custom_fields"], {});
  const workItemAttributes = get(defination, ["workitem_attributes"], {});
  const excludeWoritemAttributes = get(excludedStuff, ["workitem_attributes"], {});

  if (Object.keys(customFields).length > 0) {
    defination = {
      ...(defination || {}),
      ...(customFields || {})
    };
    unset(defination, ["custom_fields"]);
  }

  if (Object.keys(excludeCustomFields).length) {
    excludedStuff = {
      ...excludedStuff,
      ...excludeCustomFields
    };
    unset(excludedStuff, "custom_fields");
  }

  if (Object.keys(azureCustomFields).length > 0) {
    defination = {
      ...(defination || {}),
      ...(azureCustomFields || {})
    };
    unset(defination, ["workitem_custom_fields"]);
  }

  if (Object.keys(azureExcludeCustomFields).length) {
    excludedStuff = {
      ...(excludedStuff ?? {}),
      ...(azureExcludeCustomFields ?? {})
    };
    unset(excludedStuff, "workitem_custom_fields");
  }

  if (Object.keys(workItemAttributes).length > 0) {
    defination = {
      ...(defination || {}),
      ...(workItemAttributes || {})
    };
    unset(defination, ["workitem_attributes"]);
  }

  if (Object.keys(excludeWoritemAttributes).length) {
    excludedStuff = {
      ...(excludedStuff ?? {}),
      ...(excludeWoritemAttributes ?? {})
    };
    unset(excludedStuff, "workitem_attributes");
  }

  const dateKeys = Object.keys(defination).filter(
    key =>
      typeof defination?.[key] === "object" &&
      (Object.keys(defination?.[key] || {}).includes("$lt") || Object.keys(defination?.[key] || {}).includes("$age"))
  );
  const normalKeys = Object.keys(defination).filter(
    key => ![...(dateKeys || []), "exclude", "partial_match", ROLLBACK_KEY].includes(key)
  );
  const excludeKeys = Object.keys(excludedStuff).filter(key => ![ROLLBACK_KEY].includes(key));
  const partialMatchKeys = Object.keys(partialMatch);

  let attributes: sectionSelectedFilterType[] = [];

  const getTransformedKey = (key: string) => {
    if (customFieldRecords.length) {
      const transformedKeyObject: any = customFieldRecords.find((rec: any) => {
        if (rec?.metadata?.transformed) {
          const keyWithoutTransformation = rec?.field_key?.replace(rec?.metadata?.transformed, "");
          if (keyWithoutTransformation === key) return true;
        }
        return false;
      });
      if (transformedKeyObject) return transformedKeyObject?.field_key ?? key;
    }
    return key;
  };

  const rollbackKeyNormal = Object.keys(defination).filter(key => [ROLLBACK_KEY].includes(key));

  const rollbackKeyExclude = Object.keys(excludedStuff).filter(key => [ROLLBACK_KEY].includes(key));

  forEach(rollbackKeyNormal, key => {
    const value = get(defination, [key], "")?.toString();
    const nKey = getTransformedKey(key);

    attributes.push({ key: get(valuesToFilters, [nKey], nKey), param: integrationParameters.EQUALS, value: value });
  });

  forEach(normalKeys, key => {
    const value = get(defination, [key], "");
    const nKey = getTransformedKey(key);

    attributes.push({ key: get(valuesToFilters, [nKey], nKey), param: integrationParameters.EQUALS, value: value });
  });

  forEach(excludeKeys, key => {
    const value = get(excludedStuff, [key], "");
    const nKey = getTransformedKey(key);
    attributes.push({
      key: get(valuesToFilters, [nKey], nKey),
      param: integrationParameters.DOES_NOT_EQUAL,
      value: value
    });
  });

  forEach(rollbackKeyExclude, key => {
    const value = get(excludedStuff, [key], "")?.toString();
    const nKey = getTransformedKey(key);

    attributes.push({
      key: get(valuesToFilters, [nKey], nKey),
      param: integrationParameters.DOES_NOT_EQUAL,
      value: value
    });
  });

  forEach(partialMatchKeys, key => {
    const data = get(defination, ["partial_match", key], {});
    const hasContainParam = data.hasOwnProperty("$contains");
    let param = hasContainParam ? integrationParameters.CONTAINS : integrationParameters.STARTS_WITH;
    const value = get(defination, ["partial_match", key, hasContainParam ? "$contains" : "$begins"], "");
    const nKey = getTransformedKey(key);
    attributes.push({ key: get(valuesToFilters, [nKey], nKey), param: param, value: value });
  });

  forEach(dateKeys, key => {
    let value = get(defination, [key], {
      $gt: "",
      $lt: ""
    });
    const nKey = getTransformedKey(key);
    attributes.push({ key: get(valuesToFilters, [nKey], nKey), param: integrationParameters.IS_BETWEEN, value: value });
  });

  return attributes;
};

export const transformUsersSelection = (
  section: {
    dynamic_user_definition?: { [x: string]: any };
    users?: string[];
    [x: string]: any;
  },
  curActiveUserIds: string[]
) => {
  if (section.hasOwnProperty("dynamic_user_definition")) {
    let dynamicDefinitions = cloneDeep(get(section, ["dynamic_user_definition"], {}));
    section["dynamic_user_definition"] = getAttributesForm(dynamicDefinitions)?.filter(
      (defination: any) => defination?.key !== "tags"
    );
  }
  if (section.hasOwnProperty("users")) {
    section["users"] = get(section, ["users"], []).filter(userId => (curActiveUserIds || []).includes(userId));
  }
  return section;
};

export const getCustomDataParams = (application: string, integration: string) => {
  switch (application) {
    case "jira":
      return {
        fieldUri: "jira_fields",
        fieldId: `org_${application}_fields_${integration}`,
        integConfigUri: "jira_integration_config",
        integConfigId: `org_${application}_integConfig_${integration}`
      };
    case "azure_devops":
      return {
        fieldUri: "issue_management_workItem_Fields_list",
        fieldId: `org_${application}_fields_${integration}`,
        integConfigUri: "jira_integration_config",
        integConfigId: `org_${application}_integConfig_${integration}`
      };
    default:
      return {
        fieldUri: "jira_fields",
        fieldId: `org_${application}_fields_${integration}`,
        integConfigUri: "jira_integration_config",
        integConfigId: `org_${application}_integConfig_${integration}`
      };
  }
};

export const getCustomFieldConfig = (customData: any[], fieldsList?: { key: string; type: string; name: string }[]) => {
  return uniqBy(
    customData.map((custom: any) => {
      const isCustomSprint = ((custom.name || "") as string).toLowerCase() === "sprint";
      let isTimeBased = false;
      const itemFromFieldsList = (fieldsList || []).find(
        (item: { key: string; type: string; name: string }) => item.key === custom.key
      );
      if (itemFromFieldsList) {
        isTimeBased = CustomTimeBasedTypes.includes(itemFromFieldsList.type);
      }
      return baseFilterConfig(custom.key, {
        renderComponent: OrganisationFilterSelect,
        apiContainer: isTimeBased || isCustomSprint ? undefined : APIFilterContainer,
        label: custom.name,
        deleteSupport: true,
        supportPaginatedSelect: true,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        partialSupport: true,
        excludeSupport: true,
        partialKey: custom.key,
        filterMetaData: {
          selectMode: "multiple",
          uri: "jira_custom_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [custom.key],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => genericGetFilterAPIData(args, custom.key),
          specialKey: custom.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData,
        BEType: itemFromFieldsList?.type
      });
    }),
    "beKey"
  );
};

export const getAzureCustomFieldConfig = (
  customData: any[],
  fieldsList?: { key: string; type: string; name: string }[]
) => {
  return uniqBy(
    customData.map((custom: any) => {
      let isTimeBased = false;
      const itemFromFieldsList = (fieldsList || []).find(
        (item: { key: string; type: string; name: string }) => item.key === custom.key
      );
      if (itemFromFieldsList) {
        isTimeBased = CustomTimeBasedTypes.includes(itemFromFieldsList.type);
      }
      const transformedPrefix = get(custom, ["metadata", "transformed"]);
      const fieldKey = !!transformedPrefix ? custom?.key?.replace(transformedPrefix, "") : custom?.key;
      return baseFilterConfig(custom.key, {
        renderComponent: OrganisationFilterSelect,
        apiContainer: isTimeBased ? undefined : APIFilterContainer,
        label: custom.name,
        deleteSupport: true,
        supportPaginatedSelect: true,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        partialSupport: true,
        excludeSupport: true,
        partialKey: custom.key,
        filterMetaData: {
          selectMode: "multiple",
          uri: "issue_management_custom_field_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [fieldKey],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          specialKey: custom.key,
          options: (args: any) => {
            const data = extractFilterAPIData(args, fieldKey);
            const newData = get(data, ["records"], []);
            return (newData as Array<any>)?.map((item: any) => ({
              label: item?.key,
              value: item?.key
            }));
          },
          sortOptions: true,
          createOption: true
        } as ApiDropDownData,
        BEType: itemFromFieldsList?.type
      });
    }),
    "beKey"
  );
};
