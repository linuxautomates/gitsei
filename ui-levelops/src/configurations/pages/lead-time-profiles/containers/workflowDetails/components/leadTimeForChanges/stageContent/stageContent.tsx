import {
  AcceptanceTimeUnit,
  RestDevelopmentStageConfig,
  RestStageConfig,
  VelocityConfigStage
} from "classes/RestWorkflowProfile";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import React, { useCallback, useMemo } from "react";
import { v1 as uuid } from "uuid";
import StageDefinition from "../../stage/StageDefinition";

interface StageContentProps {
  leadTimeConfig: RestDevelopmentStageConfig;
  onChange: (newValue: any) => void;
  onCancel: () => void;
  selectedStage: RestStageConfig;
  type: VelocityConfigStage;
  title: string;
}

const StageContent: React.FC<StageContentProps> = ({
  selectedStage,
  onCancel,
  leadTimeConfig,
  type,
  onChange,
  title
}) => {
  const { isLoading, findIntegrationWithId } = useAllIntegrationState();

  const handleSaveStage = useCallback(
    (stageData: RestStageConfig) => {
      const stages = leadTimeConfig[type];
      let newStages = stages ? [...stages] : [];
      const isNew = selectedStage.id === "new";
      const unit = stageData?.lower_limit_unit;
      const order = stageData?.order;

      const updatedData = {
        ...stageData.json,
        lower_limit_value:
          unit === AcceptanceTimeUnit.MINUTES ? (stageData.lower_limit_value || 0) * 60 : stageData.lower_limit_value,
        upper_limit_value:
          unit === AcceptanceTimeUnit.MINUTES ? (stageData.upper_limit_value || 0) * 60 : stageData.upper_limit_value
      };

      if (isNew) {
        newStages.forEach((stage, index) => {
          if (stage.order >= order) {
            stage.order = stage.order + 1;
            newStages[index] = stage;
          }
        });

        newStages.push(
          new RestStageConfig({
            ...updatedData,
            id: uuid()
          })
        );
      } else {
        newStages = newStages.filter(stage => stage.id !== selectedStage.id);
        newStages.push(new RestStageConfig(updatedData));
      }
      onChange({
        [type]: newStages.map(stage => stage.json)
      });
    },
    [leadTimeConfig, selectedStage, type, onChange]
  );

  const integration = useMemo(
    () => findIntegrationWithId(leadTimeConfig.integration_id),
    [isLoading, leadTimeConfig.integration_id]
  );

  return (
    <div className="new-create-stage-container">
      <StageDefinition
        stage={selectedStage}
        onSave={handleSaveStage}
        onCancel={onCancel}
        isFixedStage={type === VelocityConfigStage.FIXED_STAGE}
        integration={integration!}
        title={title}
        leadTimeConfig={leadTimeConfig}
      />
    </div>
  );
};

export default StageContent;
