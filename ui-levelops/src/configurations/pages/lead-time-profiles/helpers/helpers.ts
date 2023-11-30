import { emitEvent } from "dataTracking/google-analytics";
import { STAGE_TYPE, TRIGGER_EVENT_TEXT } from "./constants";
import { TriggerEventType, VelocityConfigStage } from "classes/RestVelocityConfigs";
import { RestVelocityConfigStage } from "classes/RestVelocityConfigs";
import { AnalyticsCategoryType, WorkflowProfileAnalyticsActions } from "dataTracking/analytics.constants";
import { get, set } from "lodash";
import { getAttributesForm, getDynamicDefinationOrFilters } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import { integrationParameters } from "configurations/constants";

export const SAVE_ACTION_KEY = "action_save";
export const CANCEL_ACTION_KEY = "action_cancel";

export const getActionButtons = (disabled: Boolean = false, tooltip: string = "", label: string = "Save") => ({
  [SAVE_ACTION_KEY]: {
    type: "primary",
    label,
    hasClicked: false,
    disabled: disabled,
    showProgress: false,
    progressLabel: "Saving...",
    tooltip
  }
});

export const validatePartialKey = (values: any) => {
  const partialKey: string = Object.keys(values)?.[0] as string;
  const begins = values?.[partialKey]?.$begins ?? "";
  const contains = values?.[partialKey]?.$contains ?? "";
  const regex = values?.[partialKey]?.$regex ?? "";
  if (begins === "" && contains === "" && regex === "") {
    return false;
  }
  return true;
};

export const showExclamationIcon = (params: any) => {
  const keys = Object.keys(params || {});
  if (keys.length) {
    if (params?.target_branches?.length > 0) {
      return false;
    } else if (keys[0] === "partial_match") {
      const values = params[keys[0]];
      return !validatePartialKey(values);
    }
  }
  return true;
};

interface TrackingStageParams {
  originalPreStages: RestVelocityConfigStage[];
  originalPostStages: RestVelocityConfigStage[];
  currentPreStages: RestVelocityConfigStage[];
  currentPostStages: RestVelocityConfigStage[];
}
export const trackingStage = (params: TrackingStageParams) => {
  const { originalPreStages, originalPostStages, currentPreStages, currentPostStages } = params;

  interface StageData {
    stageType: string;
    originalStages: RestVelocityConfigStage[];
    currentStages: RestVelocityConfigStage[];
  }
  const stageData: StageData[] = [
    {
      stageType: STAGE_TYPE.preDevCustomStages,
      originalStages: originalPreStages,
      currentStages: currentPreStages
    },
    {
      stageType: STAGE_TYPE.postDevCustomStages,
      originalStages: originalPostStages,
      currentStages: currentPostStages
    }
  ];
  stageData.forEach(stage => {
    const { stageType, originalStages, currentStages } = stage;

    currentStages.forEach((currentStage: RestVelocityConfigStage) => {
      if (originalStages.filter((stage: any) => stage.id === currentStage.id).length === 0) {
        const { lower_limit_unit, lower_limit_value, upper_limit_unit, upper_limit_value, order } = currentStage;

        emitEvent(AnalyticsCategoryType.WORKFLOW_PROFILES, WorkflowProfileAnalyticsActions.ADD_STAGE, stageType);
        const eventType = currentStage?.event?.type || "";
        emitEvent(
          AnalyticsCategoryType.WORKFLOW_PROFILES,
          WorkflowProfileAnalyticsActions.ADD_STAGE,
          eventType === TriggerEventType.JIRA_STATUS ? TRIGGER_EVENT_TEXT.issueManagement : TRIGGER_EVENT_TEXT.cicd
        );
        lower_limit_unit === "DAYS" &&
          emitEvent(
            AnalyticsCategoryType.WORKFLOW_PROFILES,
            WorkflowProfileAnalyticsActions.ADD_STAGE,
            "thresholds_used_ideal",
            lower_limit_value
          );
        upper_limit_unit === "DAYS" &&
          emitEvent(
            AnalyticsCategoryType.WORKFLOW_PROFILES,
            WorkflowProfileAnalyticsActions.ADD_STAGE,
            "thresholds_used_acceptable",
            upper_limit_value
          );
      }
    });
    originalStages.forEach((originalStage: any) => {
      if (currentStages.filter((stage: any) => stage.id === originalStage.id).length === 0) {
        emitEvent(AnalyticsCategoryType.WORKFLOW_PROFILES, WorkflowProfileAnalyticsActions.DELETE_STAGE, stageType);
      }
    });
  });
};

export const checkValueEmptiness = (params: any) => {
  return params?.value === undefined || params?.value === "" || params?.value?.length === 0;
};

export const checkKeyParamValueEmptiness = (params: any) => {
  return (
    params?.key === "" ||
    params?.param === "" ||
    params?.value === undefined ||
    params?.value === "" ||
    params?.value.length === 0
  );
};

export const checkValueExistsForFilter = (params: any) => {
  return params?.key !== "" && params?.param !== "" && params?.value !== "" && params?.value?.length !== 0;
};

export const tarnsformFilterData = (velocityProfileCustomStage: any) => {
  let customStageData: any[] = [];
  velocityProfileCustomStage.map((data: {
    event: any; filter: sectionSelectedFilterType[];
  }) => {

    if (data?.event?.selectedJob === 'ALL')
      data.event.values = [];

    if (![TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS].includes(data?.event.type) && Array.isArray(data.filter)) {
      let customStageFilter = getDynamicDefinationOrFilters(data?.filter);
      customStageData.push({ ...data, filter: customStageFilter })
    } else {
      customStageData.push({ ...data })
    }
  });

  return customStageData.length > 0 ? customStageData : velocityProfileCustomStage;
}

export const tarnsformFilterGetData = (velocityProfileCustomStage: any, flagData?: any) => {
  let customStageData: any[] = [];

  const { jiraOnlyFlag, allJobFlag } = flagData;
  velocityProfileCustomStage.map((data: {
    event: any; filter: sectionSelectedFilterType[];
  }) => {
    if (data?.event?.values?.length <= 0)
      set(data?.event, ["selectedJob"], "ALL");
    if (data?.event?.values?.length <= 0 && allJobFlag)
      set(data?.event, ["values"], []);

    if (![TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS].includes(data?.event.type) && data.filter) {
      let customStageFilter = getAttributesForm(data?.filter);
      if ([TriggerEventType.CICD_JOB_RUN].includes(data?.event.type)) {
        customStageFilter = removedUnsupportedFilter(customStageFilter)
      }
      customStageData.push({ ...data, filter: customStageFilter })
    } else {
      customStageData.push({ ...data })
    }
  });
  if (jiraOnlyFlag) {
    let checkJiraReleaseStage = velocityProfileCustomStage.filter((data: { event: { type: TriggerEventType; }; }) => data.event.type === TriggerEventType.JIRA_RELEASE);

    if (checkJiraReleaseStage && checkJiraReleaseStage.length === 0) {
      let newStages = new RestVelocityConfigStage(null, TriggerEventType.JIRA_RELEASE, (customStageData.length || velocityProfileCustomStage.length));
      if (customStageData.length > 0) {
        customStageData = [...customStageData, newStages?.json];
      } else {
        velocityProfileCustomStage = [...velocityProfileCustomStage, newStages?.json];
      }
    }
  }

  return customStageData.length > 0 ? customStageData : velocityProfileCustomStage;
}

export const disabledSelectedEventTypeOption = (configData: any) => {
  const postDevelopmentStages = get(configData, VelocityConfigStage.POST_DEVELOPMENT_STAGE, []);
  const preDevelopmentStages = get(configData, VelocityConfigStage.PRE_DEVELOPMENT_STAGE, []);
  const jobs = [...postDevelopmentStages, ...preDevelopmentStages].reduce(
    (stage: string[], next: any) => {
      if (next?.event?.type === TriggerEventType.CICD_JOB_RUN) {
        stage = [...stage, ...[TriggerEventType.HARNESSCI_JOB_RUN, TriggerEventType.GITHUB_ACTIONS_JOB_RUN]]
      }
      if (next?.event?.type === TriggerEventType.HARNESSCD_JOB_RUN) {
        stage = [...stage, ...[TriggerEventType.HARNESSCD_JOB_RUN, TriggerEventType.CICD_JOB_RUN, TriggerEventType.GITHUB_ACTIONS_JOB_RUN]]
      }
      if (next?.event?.type === TriggerEventType.HARNESSCI_JOB_RUN) {
        stage = [...stage, ...[TriggerEventType.HARNESSCI_JOB_RUN, TriggerEventType.CICD_JOB_RUN, TriggerEventType.GITHUB_ACTIONS_JOB_RUN]]
      }
      if (next?.event?.type === TriggerEventType.GITHUB_ACTIONS_JOB_RUN) {
        stage = [...stage, ...[TriggerEventType.HARNESSCI_JOB_RUN, TriggerEventType.CICD_JOB_RUN, TriggerEventType.GITHUB_ACTIONS_JOB_RUN]]
      }

      return stage;
    },
    []
  );
  return jobs;
};

export const removedUnsupportedFilter = (deploymentFreqFilterConvert: any[]) => {
  return (deploymentFreqFilterConvert || []).filter((filter: any) => !([integrationParameters.CONTAINS, integrationParameters.STARTS_WITH].includes(filter?.param) && ["instance_names", "projects"].includes(filter?.key)));
}