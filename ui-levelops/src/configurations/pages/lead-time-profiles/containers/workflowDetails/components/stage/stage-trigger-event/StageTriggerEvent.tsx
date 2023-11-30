import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Radio } from "antd";
import FormItem from "antd/lib/form/FormItem";
import { Integration } from "model/entities/Integration";
import { findIntegrationType } from "helper/integration.helper";
import { RestStageConfig, TriggerEventType } from "classes/RestWorkflowProfile";
import IMEventParameter from "./parameter-containers/IMEventParameter";
import CICDEventParameter from "./parameter-containers/CICDEventParameter";
import { AntText } from "shared-resources/components";
import Loader from "components/Loader/Loader";
import { IntegrationTypes } from "constants/IntegrationTypes";

const { Group } = Radio;

interface StageTriggerEventProps {
  stage: RestStageConfig;
  onChange: (val: any) => void;
  integration: Integration;
  disabledSelectedJobs?: string[];
}

const StageTriggerEvent: React.FC<StageTriggerEventProps> = ({
  stage,
  onChange,
  integration,
  disabledSelectedJobs
}) => {
  const [optionType, setOptionType] = useState<string>(stage?.event?.type || TriggerEventType.JIRA_STATUS);
  const type = stage?.event?.type || TriggerEventType.JIRA_STATUS;
  const isIssueManagement =
    findIntegrationType(integration) === "IM"
      ? [TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS].includes(optionType as TriggerEventType)
      : false;

  const [refreshing, setRefreshing] = useState(false);
  useEffect(() => {
    setOptionType(
      !isIssueManagement ? TriggerEventType.CICD_JOB_RUN : stage?.event?.type || TriggerEventType.JIRA_STATUS
    );
    if (isIssueManagement) setRefreshing(true);
  }, [stage, isIssueManagement]);

  useEffect(() => {
    refreshing && setRefreshing(false);
  }, [refreshing]);

  const isJiraIssueManagement = integration.application === IntegrationTypes.JIRA;

  const triggerEventOptions: any[] = useMemo(() => {
    let options = [];
    if (findIntegrationType(integration) === "IM") {
      options.push({
        label: "Issue Management - Time spent in status",
        value: isJiraIssueManagement ? TriggerEventType.JIRA_STATUS : TriggerEventType.WORKITEM_STATUS
      });
    } else {
      setOptionType(TriggerEventType.CICD_JOB_RUN);
    }
    options.push({
      label: "CICD - Time till the first completion of a job",
      value: TriggerEventType.CICD_JOB_RUN
    });
    return options;
  }, [integration, setOptionType, stage]);

  const handleChange = useCallback(
    (key: string) => {
      return (e: any) => {
        const stageData = stage.json;
        switch (key) {
          case "type":
            setOptionType(e.target.value);
            onChange({
              ...stageData,
              event: {
                ...stageData.event,
                type: e.target.value,
                values: [],
                params: {}
              }
            });
            break;
          case "filter":
            onChange({
              ...stageData,
              event: {
                ...stageData.event,
                type: optionType,
                values: e
              }
            });
            break;
          case "param":
            onChange({
              ...stageData,
              event: {
                ...stageData.event,
                ...e.event
              }
            });
            break;
        }
      };
    },
    [stage, onChange, optionType, setOptionType]
  );

  if (refreshing) return <Loader />;
  return (
    <div className="stage-trigger-event-container">
      <FormItem key="trigger-event" label="Trigger Event">
        <AntText disabled>Select an event that identifies the trigger point.</AntText>
        <Group defaultValue={optionType} options={triggerEventOptions} onChange={handleChange("type")} />
      </FormItem>
      {isIssueManagement ? (
        <IMEventParameter stage={stage} integration={integration} onChange={handleChange("filter")} />
      ) : (
        <CICDEventParameter
          stage={stage}
          integration={integration}
          onChange={handleChange("filter")}
          onChangeParam={handleChange("param")}
          disabledSelectedJobs={disabledSelectedJobs}
        />
      )}
    </div>
  );
};

export default StageTriggerEvent;
