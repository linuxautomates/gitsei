import {
  WorkflowIntegrationType,
  RestDevelopmentStageConfig,
  RestStageConfig,
  VelocityConfigStage
} from "classes/RestWorkflowProfile";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { Integration } from "model/entities/Integration";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntText } from "shared-resources/components";
import VelocityConfigStages from "../../velocity-config-stages/VelocityConfigStages";
import { LEAD_TIME_HEADER } from "./constants";
import "./leadTimeForChangesContent.scss";
import IntApplicationSelector from "../../int-application-selector/IntApplicationSelector";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface LeadTimeForChangesContentProps {
  leadTimeConfig: RestDevelopmentStageConfig;
  onChange: (newValue: any) => void;
  title: string;
  description: string;
  objectKey: "lead_time_for_changes" | "mean_time_to_restore";
  setExclamationFlag: (value: boolean) => void;
  // setSelectedStage: (stage?: RestStageConfig) => void;
  // setSelectedStageType: (type: VelocityConfigStage) => void;
}

const LeadTimeForChangesContent: React.FC<LeadTimeForChangesContentProps> = ({
  leadTimeConfig,
  onChange,
  title,
  description,
  objectKey,
  setExclamationFlag
  // setSelectedStage,
  // setSelectedStageType
}) => {
  const [selectedIntegration, setSelectedIntegration] = useState<Integration | null>(null);
  const { isLoading, findIntegrationWithId } = useAllIntegrationState();

  useEffect(() => {
    setSelectedIntegration(findIntegrationWithId(leadTimeConfig?.integration_id));
  }, [leadTimeConfig.integration_id, isLoading, findIntegrationWithId]);

  const integrationApplication = useMemo(() => {
    return selectedIntegration?.application;
  }, [selectedIntegration]);

  const header = useMemo(
    () => (
      <>
        <AntText className="medium-16">{title.toUpperCase()}</AntText>
        <AntText className="header" type="secondary">
          {description}
        </AntText>
        <AntText strong>
          {LEAD_TIME_HEADER.STEP_1}
          <span className="header-required">*</span>
        </AntText>
        <AntText className="header" type="secondary">
          {LEAD_TIME_HEADER.STEP_1_NOTE}
        </AntText>
      </>
    ),
    [title, description]
  );

  const integrationType = useMemo(() => {
    return leadTimeConfig?.issue_management_integrations?.[0];
  }, [leadTimeConfig?.issue_management_integrations]);

  const onIntegraiontTypeChange = useCallback(
    (integrationApplicationType: string) => {
      let _application = integrationApplicationType;
      if (_application.includes(IntegrationTypes.AZURE)) {
        _application = IntegrationTypes.AZURE;
      }
      const updatedLeadTimeConfig = new RestDevelopmentStageConfig(
        null,
        [_application],
        leadTimeConfig.json,
        "",
        objectKey
      );
      onChange({
        [objectKey]: updatedLeadTimeConfig.json
      });
      setExclamationFlag(true);
    },
    [onChange]
  );

  const integrationSelector = useMemo(() => {
    return (
      <div className="py-10 pl-10" style={{ width: "14.5rem" }}>
        <IntApplicationSelector
          value={
            leadTimeConfig?.issue_management_integrations
              ? leadTimeConfig?.issue_management_integrations[0]
              : integrationApplication || ""
          }
          onChange={onIntegraiontTypeChange}
          supportedIntegrationTypes={[WorkflowIntegrationType.IM]}
          integration_type={integrationType}
        />
      </div>
    );
  }, [leadTimeConfig, onChange, integrationType]);

  const onConfigChange = useCallback(
    (key: string, value: any) => {
      let showFixStage = {};
      if (key === "event" && value === "commit_created" && !leadTimeConfig.fixed_stages_enabled) {
        showFixStage = {
          fixed_stages_enabled: true
        };
      }
      onChange({
        [objectKey]: {
          ...leadTimeConfig.json,
          ...showFixStage,
          [key]: value
        }
      });
    },
    [leadTimeConfig, onChange]
  );

  const stages = useMemo(
    () => (
      <>
        <AntText className="stage-header" strong>
          {LEAD_TIME_HEADER.STEP_2}
        </AntText>
        {/* <VelocityConfigStages
          config={leadTimeConfig}
          onChange={onConfigChange}
          integration={selectedIntegration}
          setSelectedStage={setSelectedStage}
          setSelectedStageType={setSelectedStageType}
          objectKey={objectKey}
        /> */}
        <VelocityConfigStages
          config={leadTimeConfig}
          onChange={onConfigChange}
          integrationApplication={
            leadTimeConfig?.issue_management_integrations
              ? leadTimeConfig?.issue_management_integrations[0]
              : integrationApplication
          }
          integration={selectedIntegration}
          objectKey={objectKey}
        />
      </>
    ),
    [selectedIntegration, leadTimeConfig, onConfigChange]
  );

  return (
    <div className="lead-time-for-changes">
      {header}
      {integrationSelector}
      {stages}
    </div>
  );
};

export default LeadTimeForChangesContent;
