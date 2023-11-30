import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Steps } from "antd";
import {
  AcceptanceTimeUnit,
  RestVelocityConfigStage,
  TriggerEventType,
  VelocityConfigStage
} from "classes/RestVelocityConfigs";
import { AntButton, AntModal, AntRow } from "shared-resources/components";
import { StageAdditionalInformation, StageCriteria, StageInfo, StageTriggerEvent } from "..";
import "./create-stage-modal.style.scss";
import StageDescriptionComponent from "../stage-description/StageDescription";
import { validatePartialKey } from "../../helpers/helpers";
import { StageEndOptions } from "classes/StageEndOptions";

const { Step } = Steps;

interface CreateStageModalProps {
  stage: RestVelocityConfigStage;
  onCancel: () => void;
  onSave: (stageData: any) => void;
  issueManagementSystem?: string;
  isFixedStage?: boolean;
  disabledSelectedEventTypeOption?: string[];
  jira_only?: boolean;
}

const CreateStageModal: React.FC<CreateStageModalProps> = props => {
  const { onCancel, stage, onSave, isFixedStage = false, disabledSelectedEventTypeOption, jira_only } = props;

  const [currentStep, setCurrentStep] = useState<number>(0);
  const [restStage, setRestStage] = useState(new RestVelocityConfigStage());

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
      new RestVelocityConfigStage({
        ...stageData,
        lower_limit_value: lower_limit,
        upper_limit_value: upper_limit,
        lower_limit_unit: unit,
        upper_limit_unit: unit
      } as any)
    );
  }, [stage]);

  const next = useCallback(() => setCurrentStep(prev => prev + 1), [currentStep]);
  const prev = useCallback(() => setCurrentStep(prev => prev - 1), [currentStep]);

  const handleNext = useCallback(() => {
    currentStep === steps.length - 1 ? onSave(restStage.json) : next();
  }, [currentStep, restStage]);

  const handleCancel = useCallback(() => {
    onCancel();
  }, []);

  const handleResetCriteria = useCallback(() => {
    let unit = stage?.lower_limit_unit;
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

    const stageData = {
      ...restStage.json,
      lower_limit_unit: unit,
      upper_limit_unit: undefined,
      lower_limit_value: lower_limit,
      upper_limit_value: upper_limit
    };
    setRestStage(new RestVelocityConfigStage(stageData as any));
  }, [stage, restStage]);

  const handleFieldChange = useCallback(data => {
    setRestStage(new RestVelocityConfigStage(data));
  }, []);

  const steps = useMemo(() => {
    if (restStage.type === VelocityConfigStage.FIXED_STAGE) {
      const stages = [
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
        restStage.event?.type === StageEndOptions.SCM_PR_CREATED
      ) {
        stages.splice(1, 0, {
          title: "Stage Definition",
          content: <StageDescriptionComponent stage={restStage} onChange={handleFieldChange} />
        });
      }
      if (restStage.event?.type === StageEndOptions.SCM_PR_MERGED) {
        stages.splice(1, 0, {
          title: "Additional Information",
          content: <StageAdditionalInformation stage={restStage} onChange={handleFieldChange} />
        });
      }
      return stages;
    } else {
      const stages = [
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
              issueManagementSystem={props.issueManagementSystem}
              disabledSelectedEventTypeOption={disabledSelectedEventTypeOption}
              currentSelectedEventType={stage?.event?.type}
              jira_only={jira_only}
            />
          )
        },
        {
          title: "Thresholds",
          content: <StageCriteria stage={restStage} onChange={handleFieldChange} />
        }
      ];

      if (restStage.event?.type === TriggerEventType.JIRA_RELEASE) {
        stages.splice(1, 1);
      }
      return stages;
    }
  }, [restStage]);

  const isValid = useMemo(() => {
    if (currentStep === 0) {
      return restStage?.name && restStage?.name?.length > 0;
    } else if (currentStep === 1) {
      const params = restStage?.event?.params || {};
      const keys = Object.keys(params);
      const type = restStage?.event?.type || TriggerEventType.JIRA_STATUS;
      if (
        [TriggerEventType.CICD_JOB_RUN, TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS, TriggerEventType.HARNESSCD_JOB_RUN, TriggerEventType.HARNESSCI_JOB_RUN, TriggerEventType.JIRA_RELEASE].includes(
          type as TriggerEventType
        )
      ) {
        return true;
      }
      if (restStage.event?.type === StageEndOptions.SCM_PR_MERGED && keys.length === 0) {
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
            return validatePartialKey(values);
          }
          if (!values.length && keys[i] !== "partial_match") {
            return false;
          }
        }
      }

      if (restStage?.type === VelocityConfigStage.FIXED_STAGE) {
        if (
          restStage.event?.type === StageEndOptions.SCM_PR_LABEL_ADDED ||
          restStage.event?.type === StageEndOptions.SCM_PR_CREATED
        ) {
          return !!restStage?.event?.type;
        } else {
          return (
            restStage?.lower_limit_unit &&
            restStage?.upper_limit_unit &&
            restStage?.upper_limit_value &&
            restStage?.lower_limit_value
          );
        }
      }
      return restStage?.event?.type && restStage?.event?.values && (restStage?.event?.values || []).length;
    } else if (currentStep === 2) {
      return (
        restStage?.lower_limit_unit &&
        restStage?.upper_limit_unit &&
        restStage?.upper_limit_value &&
        restStage?.lower_limit_value
      );
    }
    return true;
  }, [currentStep, restStage]);

  const renderFooter = useMemo(() => {
    return (
      <AntRow type="flex" justify="space-between">
        <div>
          {currentStep === 2 && (
            <AntButton type="link" onClick={handleResetCriteria}>
              Reset Criteria
            </AntButton>
          )}
        </div>
        <div>
          <AntButton type="ghost" onClick={handleCancel}>
            Cancel
          </AntButton>
          {!!currentStep && (
            <AntButton type="ghost" onClick={prev}>
              Back
            </AntButton>
          )}
          <AntButton type="primary" disabled={!isValid} onClick={handleNext}>
            {currentStep === steps.length - 1 ? 'Save' : 'Next'}
          </AntButton>
        </div>
      </AntRow>
    );
  }, [currentStep, restStage, stage]);

  const renderBody = useMemo(() => {
    return (
      <div className="w-100 h-100 flex direction-column">
        <Steps
          className="p-20"
          current={currentStep}
          onChange={current => restStage?.isValid && setCurrentStep(current)}>
          {steps.map(item => (
            <Step key={item.title} title={item.title} />
          ))}
        </Steps>
        {steps[currentStep].content}
      </div>
    );
  }, [currentStep, restStage]);

  return (
    <AntModal
      title="Create Stage"
      centered
      destroyOnClose
      mask
      visible
      wrapClassName="create-stage-container"
      footer={renderFooter}
      closable={false}
      maskClosable={false}
      onCancel={handleCancel}>
      {renderBody}
    </AntModal>
  );
};

export default CreateStageModal;
