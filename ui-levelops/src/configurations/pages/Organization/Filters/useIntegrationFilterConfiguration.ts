import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { CICDApplications, findIntegrationType } from "helper/integration.helper";
import { get } from "lodash";
import { useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import { OrgCustomConfigData } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { FILTERS_CONFIG } from "../Constants";
import { getCustomDataParams, getAzureCustomFieldConfig, getCustomFieldConfig } from "../Helpers/OrgUnit.helper";

export type customFlagType = {
  removeCicdJobNameFilter?: boolean;
  removeProjectFilter?: boolean;
  integrationType?: string;
  selectedApplication?: string;
  removeCicdStageStepFilter?: boolean;
};

export function useIntegrationFilterConfiguration(
  application: string,
  integrationIds: string[],
  customFlag: customFlagType = {}
) {
  const dispatch = useDispatch();
  const { isLoading, findIntegrationWithId } = useAllIntegrationState();
  const integrationKeys = integrationIds.sort().join("_");
  const { fieldUri, fieldId, integConfigUri, integConfigId } = getCustomDataParams(application, integrationKeys);
  const integConfigListState = useParamSelector(getGenericUUIDSelector, {
    uri: integConfigUri,
    method: "list",
    uuid: integConfigId
  });
  const integFieldsListState = useParamSelector(getGenericUUIDSelector, {
    uri: fieldUri,
    method: "list",
    uuid: fieldId
  });

  const {
    removeCicdJobNameFilter,
    removeProjectFilter,
    integrationType,
    selectedApplication,
    removeCicdStageStepFilter
  } = customFlag;

  useEffect(() => {
    if (application) {
      if ([IntegrationTypes.JIRA, IntegrationTypes.AZURE].includes(application as IntegrationTypes)) {
        dispatch(
          OrgCustomConfigData(
            (integrationIds || [])?.map(item => item?.toString()),
            fieldUri,
            integConfigUri,
            fieldId,
            integConfigId
          )
        );
      }
    }
  }, [application]);

  const getCustomIntegrationApplication = useMemo(() => {
    const integrationId = integrationIds?.[0];
    const IntegrationIdData = findIntegrationWithId(integrationId);
    switch (application) {
      case IntegrationTypes.AZURE:
        if (
          integrationType === WorkflowIntegrationType.CICD &&
          selectedApplication === IntegrationTypes.AZURE_NON_SPLITTED
        ) {
          return WorkflowIntegrationType.CICD;
        }
        return findIntegrationType(IntegrationIdData);
        break;
      case IntegrationTypes.GITLAB:
        if(integrationType === WorkflowIntegrationType.CICD){
          return IntegrationTypes.GITLAB_CICD
        }
        return application;
      default:
        break;
    }
  }, [integrationIds, isLoading, findIntegrationWithId, application, integrationType, selectedApplication]);

  const transformCustomFieldsData = (customFields: any[], fieldList: any[]) => {
    if (application === IntegrationTypes.AZURE) {
      return (customFields ?? []).map((custom: { key: string }) => {
        const fieldListCustom: { key: string } = (fieldList ?? []).find((field: { key: string }) =>
          field?.key?.includes(custom?.key)
        );
        if (fieldListCustom) {
          const initialFieldKey = fieldListCustom.key?.replace(AZURE_CUSTOM_FIELD_PREFIX, "");
          if (initialFieldKey === custom.key) {
            return {
              ...(custom ?? {}),
              key: fieldListCustom?.key,
              metadata: {
                transformed: AZURE_CUSTOM_FIELD_PREFIX
              }
            };
          }
        }
        return custom;
      });
    }
    return customFields;
  };

  const allFilterConfig = useMemo(() => {
    let config: any = [];
    switch (getCustomIntegrationApplication) {
      case WorkflowIntegrationType.IM:
        config = get(FILTERS_CONFIG, ["azure_devops_im"], []);
        break;
      case WorkflowIntegrationType.SCM:
        config = get(FILTERS_CONFIG, ["azure_devops_scm"], []);
        break;
      case WorkflowIntegrationType.CICD:
        config = get(FILTERS_CONFIG, ["azure_devops_cicd"], []);
        break;
      case IntegrationTypes.GITLAB_CICD:
        config = get(FILTERS_CONFIG, [IntegrationTypes.GITLAB_CICD], []);
        break;
      default:
        config = get(FILTERS_CONFIG, [application], []);
        break;
    }

    const fieldLoading = get(integFieldsListState, ["loading"], true);
    const fieldError = get(integFieldsListState, ["error"], true);
    const configLoading = get(integConfigListState, ["loading"], true);
    const configError = get(integConfigListState, ["loading"], true);
    let fieldsData = [];
    let ConfigData = [];
    if (!fieldLoading && !fieldError) {
      fieldsData = get(integFieldsListState, ["data"], []);
    }
    if (!configLoading && !configError) {
      ConfigData = get(integConfigListState, ["data"], []);
      if (Array.isArray(ConfigData)) {
        ConfigData = transformCustomFieldsData(ConfigData, fieldsData);
      }
    }

    //THIS FALG WILL COME TRUE FROM WORKFLOW PROFILE CICD FILTER PART IN THAT WE DON'T WANT JOB NAME AS FILTER BECAUE WE SELECT THE JOBS OVER THERE
    //WE CAN NOT ADD THIS CONFIGURATION ON CONFIG LEVEL BECAUSE WE NEED JOB NAME FILTER IN WIDGET LEVEL & OU LEVEL
    if (
      removeCicdJobNameFilter &&
      [...CICDApplications, IntegrationTypes.AZURE].includes(application as IntegrationTypes)
    ) {
      config = config.filter(
        (configData: { id: string }) => !["job_names", "job_normalized_full_names"].includes(configData.id)
      );
    }

    if (
      removeCicdStageStepFilter &&
      [...CICDApplications, IntegrationTypes.AZURE].includes(application as IntegrationTypes)
    ) {
      config = config.filter((configData: { id: string }) => !["stage_name", "step_name"].includes(configData.id));
    }

    // THIS FALG WILL COME TRUE FROM WORKFLOW PROFILE LEAD TIME & MTTR & VELOCITY PROFILE STAGES PART IN THAT WE HAVE TO REMOVE PROJECT FILTER BECAUSE BE SUPPORT IS NOT THERE ONCE IT DONE THEN WE REMOVE THIS
    if (
      removeProjectFilter &&
      [...CICDApplications, IntegrationTypes.AZURE].includes(application as IntegrationTypes)
    ) {
      config = config.filter((configData: { id: string }) => !["projects"].includes(configData.id));
    }

    if (Array.isArray(fieldsData) && Array.isArray(ConfigData)) {
      const customFieldConfig =
        application === IntegrationTypes.AZURE
          ? getAzureCustomFieldConfig(ConfigData, fieldsData)
          : getCustomFieldConfig(ConfigData, fieldsData);
      return [...config, ...customFieldConfig];
    }
    return config;
  }, [
    application,
    integConfigListState,
    integFieldsListState,
    removeCicdJobNameFilter,
    getCustomIntegrationApplication
  ]);

  const selectedIntegrationFilters = useMemo(() => {
    return allFilterConfig.map((config: any) => {
      let childFilterKeys;
      if (config?.childFilterKeys && typeof config.childFilterKeys === "function") {
        let childFilterKeysData = config.childFilterKeys(config.beKey);
        const childKeysData = childFilterKeysData.reduce((acc: any, data: any) => {
          let childFilterKeysLable = config.childButtonLableName(data, "filterName");
          let childFilterButtonName = config.childButtonLableName(data);
          acc.push({ label: childFilterKeysLable, value: data, options: [], buttonName: childFilterButtonName });
          return acc;
        }, []);

        childFilterKeys = childFilterKeysData && childFilterKeysData.length > 0 ? { childKeys: childKeysData } : {};
      }
      return { label: config.label, value: config?.beKey, options: [], ...childFilterKeys };
    });
  }, [allFilterConfig]);
  return [allFilterConfig, selectedIntegrationFilters];
}
