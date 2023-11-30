import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntButton, AntText } from "shared-resources/components";
import "./stageDefinition.style.scss";
import StageDescription from "./stage-description/StageDescription";
import StageInfo from "./stage-info/StageInfo";
import StageCriteria from "./stage-criteria/StageCriteria";
import StageAdditionalInformation from "./stage-additional-information/stage-additional-information";
import StageTriggerEvent from "./stage-trigger-event/StageTriggerEvent";
import { validatePartialKey } from "configurations/pages/lead-time-profiles/helpers/helpers";
import {
  AcceptanceTimeUnit,
  RestDevelopmentStageConfig,
  RestStageConfig,
  TriggerEventType,
  VelocityConfigStage
} from "classes/RestWorkflowProfile";
import { Integration } from "model/entities/Integration";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { toTitleCase } from "utils/stringUtils";
import { StageEndOptions } from "classes/StageEndOptions";
import { get } from "lodash";

interface StageDefinitionProps {
  stage: RestStageConfig;
  onCancel: () => void;
  onSave: (stageData: any) => void;
  isFixedStage?: boolean;
  integration: Integration;
  title: string;
  leadTimeConfig?: RestDevelopmentStageConfig;
}

const StageDefinition: React.FC<StageDefinitionProps> = props => {
  const { onCancel, stage, onSave, integration, isFixedStage = false, title, leadTimeConfig } = props;

  const [restStage, setRestStage] = useState(new RestStageConfig());

  const disabledSelectedJobs = useMemo(() => {
    const config = leadTimeConfig?.json;
    const postDevelopmentStages = get(config, VelocityConfigStage.POST_DEVELOPMENT_STAGE, []);
    const preDevelopmentStages = get(config, VelocityConfigStage.PRE_DEVELOPMENT_STAGE, []);
    const jobs = [...postDevelopmentStages, ...preDevelopmentStages].reduce(
      (stage: string[], next: RestStageConfig) => {
        if (next?.event?.type === TriggerEventType.CICD_JOB_RUN) {
          return [...(stage || []), ...(next.event?.values || [])];
        }
        return stage;
      },
      []
    );
    return jobs;
  }, [leadTimeConfig]);

  useEffect(() => {
    const stageData = stage?.json || {};
    let unit = stageData?.lower_limit_unit || AcceptanceTimeUnit.DAYS;

    let lower_limit = stage?.lower_limit_value || 0;
    let upper_limit = stage?.upper_limit_value || 0;

    switch (unit) {
      case AcceptanceTimeUnit.SECONDS:
        if (lower_limit > 86400) {
          lower_limit = Math.trunc(lower_limit / 86400);
          upper_limit = Math.trunc(upper_limit / 86400);
          unit = AcceptanceTimeUnit.DAYS;
        } else {
          lower_limit = Math.trunc(lower_limit / 3600);
          upper_limit = Math.trunc(upper_limit / 3600);
          unit = AcceptanceTimeUnit.MINUTES;
        }
        break;
      case AcceptanceTimeUnit.MINUTES:
        lower_limit = Math.trunc(lower_limit / 60);
        upper_limit = Math.trunc(upper_limit / 60);
        break;
    }

    setRestStage(
      new RestStageConfig({
        ...stageData,
        lower_limit_value: lower_limit,
        upper_limit_value: upper_limit,
        lower_limit_unit: unit,
        upper_limit_unit: unit
      } as any)
    );
  }, [stage]);

  const handleFieldChange = useCallback(data => {
    setRestStage(new RestStageConfig(data));
  }, []);

  const steps = useMemo(() => {
    if (restStage.type === VelocityConfigStage.FIXED_STAGE) {
      const stepsList = [
        {
          title: "Stage Info",
          content: <StageInfo stage={restStage} onChange={handleFieldChange} isFixedStage={isFixedStage} />
        },
        {
          title: "Thresholds",
          content: <StageCriteria stage={restStage} onChange={handleFieldChange} />
        }
      ];
      if (
        restStage.event?.type === StageEndOptions.SCM_PR_LABEL_ADDED ||
        restStage.event?.type === StageEndOptions.SCM_PR_CREATED ||
        restStage.event?.type === StageEndOptions.SCM_PR_SOURCE_BRANCH
      ) {
        stepsList.splice(1, 0, {
          title: "Stage Definition",
          content: <StageDescription stage={restStage} onChange={handleFieldChange} type={restStage.event.type} />
        });
      }
      if (restStage.event?.type === StageEndOptions.SCM_PR_MERGED) {
        stepsList.splice(1, 0, {
          title: "Additional Information",
          content: <StageAdditionalInformation stage={restStage} onChange={handleFieldChange} />
        });
      }
      return stepsList;
    } else {
      return [
        {
          title: "Stage Info",
          content: <StageInfo stage={restStage} onChange={handleFieldChange} isFixedStage={isFixedStage} />
        },
        {
          title: "Stage Definition",
          content: (
            <StageTriggerEvent
              stage={restStage}
              onChange={handleFieldChange}
              integration={integration}
              disabledSelectedJobs={disabledSelectedJobs}
            />
          )
        },
        {
          title: "Thresholds",
          content: <StageCriteria stage={restStage} onChange={handleFieldChange} />
        }
      ];
    }
  }, [restStage, disabledSelectedJobs]);

  const isValid = useMemo(() => {
    let isValid = restStage?.name && restStage?.name?.length > 0;
    if (!isValid) {
      return false;
    }
    const params = restStage?.event?.params || {};
    const keys = Object.keys(params);
    const type = restStage?.event?.type || TriggerEventType.JIRA_STATUS;
    const values = restStage?.event?.values || [];
    if (
      [TriggerEventType.CICD_JOB_RUN, TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS].includes(
        type as TriggerEventType
      ) &&
      values.length === 0
    ) {
      return false;
    }
    if (restStage.event?.type === StageEndOptions.SCM_PR_MERGED && restStage?.event?.scm_filters?.hasError) {
      return false;
    }
    if (keys.length) {
      for (let i = 0; i < keys.length; i++) {
        const values = params[keys[i]];
        if (
          restStage.event?.type === StageEndOptions.SCM_PR_MERGED &&
          typeof values === "object" &&
          !Array.isArray(values) &&
          keys.length === 1
        ) {
          isValid = validatePartialKey(values);
          if (!isValid) return false;
        } /*  */
        if (!values.length && keys[i] !== "partial_match") {
          return false;
        }
      }
    }

    if (restStage?.type === VelocityConfigStage.FIXED_STAGE) {
      if (restStage.event?.type === StageEndOptions.SCM_PR_SOURCE_BRANCH && restStage.event.scm_filters?.hasError) {
        return false;
      }
    } else {
      isValid = !!(restStage?.event?.type && restStage?.event?.values && (restStage?.event?.values || []).length);
    }

    if (restStage?.event?.type === "SCM_PR_SOURCE_BRANCH" && !restStage?.event?.scm_filters?.source_branch?.value) {
      isValid = false;
    }

    return (
      isValid &&
      restStage?.lower_limit_unit &&
      restStage?.upper_limit_unit &&
      restStage?.upper_limit_value &&
      restStage?.lower_limit_value
    );
  }, [restStage]);

  const onConfirmClick = useCallback(() => onSave(restStage), [onSave, restStage]);

  const header = useMemo(
    () => (
      <div className="header">
        <div className="stage-title">
          <span className="back-icon" onClick={onCancel}>
            <SvgIconComponent icon="arrowLeftCircle" />
          </span>
          <AntText className="name">{`${toTitleCase(title)} > Create Stage`}</AntText>
        </div>
        <div>
          <AntButton onClick={onCancel} className="mx-5">
            Cancel
          </AntButton>
          <AntButton onClick={onConfirmClick} type={"primary"} disabled={!isValid} className="mx-5">
            Confirm
          </AntButton>
        </div>
      </div>
    ),
    [isValid, onCancel, title, onConfirmClick]
  );

  return (
    <>
      {header}
      <div className="stage-content">
        {steps.map(step => (
          <div className="steps">
            <AntText className="step-title">{step.title}</AntText>
            <div className="step-content">{step.content}</div>
            <hr />
          </div>
        ))}
      </div>
    </>
  );
};

export default StageDefinition;
