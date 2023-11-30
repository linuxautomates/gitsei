import { RestDevelopmentStageConfig, WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import {
  getAttributesForm,
  getDynamicDefinationOrFilters
} from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { get, isEmpty } from "lodash";
import { removedUnsupportedFilter, tarnsformFilterData, tarnsformFilterGetData } from "./helpers";
import { Integration } from "model/entities/Integration";
import { JENKINS_CICD_JOB_COUNT_CHILD_FILTER_LABLE } from "dashboard/reports/jenkins/constants";
import { CALCULATION_RELEASED_IN_KEY } from "../containers/workflowDetails/components/constant";

const transformFilters = (initialFilter: any, application?: string) => {
  if (Array.isArray(initialFilter.filter) && initialFilter.integration_type === WorkflowIntegrationType.IM) {
    let deploymentFreqImFilter = getDynamicDefinationOrFilters(initialFilter?.filter);
    return {
      ...initialFilter,
      filter: initialFilter?.calculation_field === CALCULATION_RELEASED_IN_KEY && application === IntegrationTypes.JIRA ? {} : deploymentFreqImFilter
    };
  } else if (
    initialFilter.integration_type === WorkflowIntegrationType.SCM &&
    initialFilter.scm_filters &&
    Object.keys(initialFilter.scm_filters).length > 0
  ) {
    let deploymentFreqScmFilter = initialFilter.scm_filters.postData;
    return {
      ...initialFilter,
      scm_filters: deploymentFreqScmFilter
    };
  } else if (Array.isArray(initialFilter.filter) && initialFilter.integration_type === WorkflowIntegrationType.CICD) {
    if (
      initialFilter.hasOwnProperty("is_ci_job") &&
      !initialFilter.is_ci_job &&
      initialFilter.hasOwnProperty("is_cd_job") &&
      !initialFilter.is_cd_job
    ) {
      //IF BOTH CHECKBOX NOT CHECKED THEN REMOVE THIS KEYS
      delete initialFilter.is_ci_job;
      delete initialFilter.is_cd_job;
    } else if (
      initialFilter.hasOwnProperty("is_ci_job") &&
      initialFilter.is_ci_job &&
      initialFilter.hasOwnProperty("is_cd_job") &&
      !initialFilter.is_cd_job
    ) {
      //IF CI IS TRUE & CD IS FALSE THEN SET CD AS NULL
      initialFilter.is_cd_job = null;
    } else if (
      initialFilter.hasOwnProperty("is_ci_job") &&
      !initialFilter.is_ci_job &&
      initialFilter.hasOwnProperty("is_cd_job") &&
      initialFilter.is_cd_job
    ) {
      //IF CD IS TRUE & CI IS FALSE THEN SET CI AS NULL
      initialFilter.is_ci_job = null;
    }

    if (initialFilter.event.params && initialFilter.event.params[" "]) {
      delete initialFilter.event.params[" "];
    }

    if (initialFilter.event?.selectedJob === "ALL") {
      initialFilter.event.values = [];
    }

    const convertFilter = initialFilter?.filter.reduce((acc: any, defaultData: any) => {
      acc.push(defaultData);
      if (defaultData.childKeys && defaultData.childKeys.length > 0) {
        acc.push(...defaultData.childKeys);
      }
      return acc;
    }, []);

    let deploymentFreqCicdFilter = getDynamicDefinationOrFilters(convertFilter);
    return {
      ...initialFilter,
      filter: deploymentFreqCicdFilter
    };
  }
  return initialFilter;
};

const transformStagesFilter = (stageData: RestDevelopmentStageConfig) => {
  let preDevelopmentCustomStage = tarnsformFilterData(get(stageData, ["pre_development_custom_stages"], []));
  let postDevelopmentCustomStage = tarnsformFilterData(get(stageData, ["post_development_custom_stages"], []));

  return {
    ...stageData,
    pre_development_custom_stages: preDevelopmentCustomStage,
    post_development_custom_stages: postDevelopmentCustomStage
  };
};

export const transformWorkflowProfileData = (workflowprofileData: any) => {
  let { deployment_frequency, change_failure_rate, lead_time_for_changes, mean_time_to_restore } = workflowprofileData;
  let deploymentFreqFilter = get(workflowprofileData, ["deployment_frequency", "filters", "deployment_frequency"], []);
  let changeFailureRateFailed = get(workflowprofileData, ["change_failure_rate", "filters", "failed_deployment"], []);
  let changeFailureRateTotal = get(workflowprofileData, ["change_failure_rate", "filters", "total_deployment"], []);
  let leadTimeForChangesFilter = get(workflowprofileData, ["lead_time_for_changes"], []);
  let meanTimeToRestoreFilter = get(workflowprofileData, ["mean_time_to_restore"], []);


  deploymentFreqFilter = transformFilters(deploymentFreqFilter, deployment_frequency?.application);
  changeFailureRateFailed = transformFilters(changeFailureRateFailed);
  changeFailureRateTotal = transformFilters(changeFailureRateTotal);
  leadTimeForChangesFilter = transformStagesFilter(lead_time_for_changes);
  meanTimeToRestoreFilter = transformStagesFilter(meanTimeToRestoreFilter);

  let finalCreateJson: any = {
    name: workflowprofileData.name,
    is_new: workflowprofileData.is_new,
    description: workflowprofileData.description,
    associated_ou_ref_ids: workflowprofileData.associated_ou_ref_ids,
    deployment_frequency: {
      ...deployment_frequency,
      filters: {
        deployment_frequency: {
          ...deploymentFreqFilter
        }
      }
    },
    change_failure_rate: {
      ...change_failure_rate,
      is_absolute: change_failure_rate.is_absolute ?? false,
      filters: {
        failed_deployment: {
          ...changeFailureRateFailed
        },
        total_deployment: {
          ...(change_failure_rate.is_absolute ? "" : changeFailureRateTotal)
        }
      }
    },
    lead_time_for_changes: {
      ...lead_time_for_changes,
      ...leadTimeForChangesFilter
    },
    mean_time_to_restore: {
      ...mean_time_to_restore,
      ...meanTimeToRestoreFilter
    }
  };

  return finalCreateJson;
};

export const transformChildFilter = (initialFilter: any) => {
  let updateFilter = initialFilter.reduce((acc: any, defaultData: any) => {
    let childFilterData = JENKINS_CICD_JOB_COUNT_CHILD_FILTER_LABLE.filter(
      (data: any) => data.childKey === defaultData.key
    );
    if (childFilterData && childFilterData.length > 0) {
      let findParent = acc.findIndex((data: any) => data.key === childFilterData[0].parnentKey);
      acc[findParent] = {
        ...acc[findParent],
        childKeys: [
          ...(acc[findParent]?.childKeys || []),
          { ...defaultData, buttonName: childFilterData[0].lableName, showButton: false }
        ]
      };
    } else {
      acc.push(defaultData);
    }
    return acc;
  }, []);
  return updateFilter;
};

const transformIMFilter = (initialFilter: any, fieldListRecords: IntegrationTransformedCFTypes[]) => {
  if (initialFilter.integration_type === WorkflowIntegrationType.IM) {
    let deploymentFreqFilterConvert = getAttributesForm(get(initialFilter, ["filter"], {}), fieldListRecords);

    return {
      ...initialFilter,
      filter: deploymentFreqFilterConvert
    };
  } else if (initialFilter.integration_type === WorkflowIntegrationType.CICD) {
    let deploymentFreqFilterConvert = getAttributesForm(get(initialFilter, ["filter"], {}), []);
    deploymentFreqFilterConvert = removedUnsupportedFilter(deploymentFreqFilterConvert);
    deploymentFreqFilterConvert = transformChildFilter(deploymentFreqFilterConvert);
    return {
      ...initialFilter,
      filter: deploymentFreqFilterConvert
    };
  }
  return initialFilter;
};

const updateFixedStage = (leadTimeConfig: any, basicStages: any) => {
  const updatedBasicStage = leadTimeConfig.starting_event_is_commit_created
    ? basicStages.fixed_stages.filter((stage: any) => get(stage, ["event", "type"], "") !== "SCM_COMMIT_CREATED")
    : basicStages.fixed_stages;

  //IF FIX STAGES ARE DISABLED THEN IT COME BLANK ARRAY FROM API FOR NEED TO ADD DEFAULT ALL IN DISABLED
  //ELSE IF ONE OF STAGES ARE DISABLED & OTHER ARE ENABLED THE FROM API ONLY ANABLED WILL COME WE HAVE TO ADD DISABLED AD DEFAULT IN DISABLED STAGE
  if (!leadTimeConfig.fixed_stages) {
    leadTimeConfig = {
      ...leadTimeConfig,
      fixed_stages: updatedBasicStage,
      fixed_stages_enabled: false
    };
  } else {
    const newLeadTimeFixedStageData = updatedBasicStage.reduce((acc: any, defaultData: any) => {
      let findExistingData = leadTimeConfig.fixed_stages.filter(
        (existingData: { name: any }) => existingData.name === defaultData.name
      );
      if (findExistingData.length <= 0) {
        defaultData = {
          ...defaultData,
          enabled: false
        };
        acc.push(defaultData);
      } else {
        acc.push(findExistingData[0]);
      }
      return acc;
    }, []);

    leadTimeConfig = {
      ...leadTimeConfig,
      fixed_stages: newLeadTimeFixedStageData
    };
  }
  return leadTimeConfig;
};

const transformStagesFilterGet = (stageData: RestDevelopmentStageConfig) => {
  let preDevelopmentCustomStage = tarnsformFilterGetData(get(stageData, ["pre_development_custom_stages"], []), {
    allJobFlag: true
  });
  let postDevelopmentCustomStage = tarnsformFilterGetData(get(stageData, ["post_development_custom_stages"], []), {
    allJobFlag: true
  });

  return {
    ...stageData,
    pre_development_custom_stages: preDevelopmentCustomStage,
    post_development_custom_stages: postDevelopmentCustomStage
  };
};

export const getApplicationBasedOnIntegration = (config: any, integrations: Integration[]) => {
  if (config?.application) {
    return config.application;
  } else if (config?.integration_ids && config?.integration_ids?.length) {
    const int = config?.integration_ids?.[0]?.toString();
    return (integrations || []).find(integration => integration?.id === int)?.application;
  } else {
    const int = config?.integration_id?.toString();
    return (integrations || []).find(integration => integration?.id === int)?.application;
  }
};

export const transformWorkflowProfileDataGet = (
  workflowprofileData: any,
  basicStages: any,
  fieldListRecords: IntegrationTransformedCFTypes[],
  integrations: Integration[] = []
) => {
  let { deployment_frequency, change_failure_rate, lead_time_for_changes, mean_time_to_restore } = workflowprofileData;

  let deploymentFreqFilter = get(workflowprofileData, ["deployment_frequency", "filters", "deployment_frequency"], []);
  let changeFailureRateFailed = get(workflowprofileData, ["change_failure_rate", "filters", "failed_deployment"], []);
  let changeFailureRateTotal = get(workflowprofileData, ["change_failure_rate", "filters", "total_deployment"], []);
  let leadTimeForChangesFilter = get(workflowprofileData, ["lead_time_for_changes"], []);
  let meanTimeToRestoreFilter = get(workflowprofileData, ["mean_time_to_restore"], []);

  deploymentFreqFilter = transformIMFilter(deploymentFreqFilter, fieldListRecords);
  changeFailureRateFailed = transformIMFilter(changeFailureRateFailed, fieldListRecords);
  changeFailureRateTotal = transformIMFilter(changeFailureRateTotal, fieldListRecords);

  if (!isEmpty(changeFailureRateTotal)) {
    changeFailureRateTotal = {
      ...changeFailureRateTotal,
      calculation_field:
        change_failure_rate?.filters?.total_deployment?.calculation_field || change_failure_rate?.calculation_field
    };
  }

  lead_time_for_changes = basicStages ? updateFixedStage(lead_time_for_changes, basicStages) : lead_time_for_changes;
  mean_time_to_restore = basicStages ? updateFixedStage(mean_time_to_restore, basicStages) : mean_time_to_restore;

  leadTimeForChangesFilter = transformStagesFilterGet(lead_time_for_changes);
  meanTimeToRestoreFilter = transformStagesFilterGet(mean_time_to_restore);
  const deploymentFrequencyApplication = getApplicationBasedOnIntegration(deployment_frequency, integrations);
  const changeFailureRateApplication = getApplicationBasedOnIntegration(change_failure_rate, integrations);
  let finalCreateJson: any = {
    ...workflowprofileData,
    deployment_frequency: {
      ...deployment_frequency,
      application: deploymentFrequencyApplication,
      integration_id: deployment_frequency.integration_id.toString(),
      integration_ids: deployment_frequency?.integration_ids?.map((id: any) => id?.toString()) || [
        deployment_frequency?.integration_id?.toString()
      ],
      filters: {
        deployment_frequency: {
          ...deploymentFreqFilter,
          calculation_field:
            deployment_frequency?.filters?.deployment_frequency?.calculation_field ||
            deployment_frequency?.calculation_field
        }
      }
    },
    change_failure_rate: {
      ...change_failure_rate,
      application: changeFailureRateApplication,
      is_absolute: change_failure_rate.is_absolute ?? false,
      integration_id: change_failure_rate.integration_id.toString(),
      integration_ids: change_failure_rate?.integration_ids?.map((id: any) => id?.toString()) || [
        change_failure_rate?.integration_id?.toString()
      ],
      filters: {
        failed_deployment: {
          ...changeFailureRateFailed,
          calculation_field:
            change_failure_rate?.filters?.failed_deployment?.calculation_field || change_failure_rate?.calculation_field
        },
        total_deployment: {
          ...changeFailureRateTotal
        }
      }
    },
    lead_time_for_changes: {
      ...lead_time_for_changes,
      ...leadTimeForChangesFilter,
      integration_id: lead_time_for_changes?.integration_id?.toString(),
      issue_management_integrations: lead_time_for_changes?.issue_management_integrations ?? [IntegrationTypes.JIRA]
    },
    mean_time_to_restore: {
      ...mean_time_to_restore,
      ...meanTimeToRestoreFilter,
      integration_id: mean_time_to_restore?.integration_id?.toString(),
      issue_management_integrations: mean_time_to_restore?.issue_management_integrations ?? [IntegrationTypes.JIRA]
    }
  };
  return finalCreateJson;
};
