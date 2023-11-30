import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import React, { useMemo, useState } from "react";
import VelocityConfigBasicInfo from "../velocity-config-basic-info/VelocityConfigBasicInfo";
import VelocityConfigStages from "../velocity-config-stages/VelocityConfigStages";
import DefinitionConfigurations from "../doraMetricsDefinitions/DefinitionConfigurations";
import { DORA_CONFIG_METRICS } from "../../helpers/constants";
import { notification } from "antd";
import { DORAConfigDefinition } from "classes/DORAConfigDefinition";

interface VelocityConfigProps {
  config: RestVelocityConfigs;
  onChange: (key: string, value: any) => void;
  nameExist?: boolean;
  stageName?: string;
}

const VelocityConfig = (props: VelocityConfigProps) => {
  const [selectedMetric, setSelectedMetric] = useState<{ label: string; value: string }>(DORA_CONFIG_METRICS[0]);

  const updateSelectedMetric = (metric: { label: string; value: string }) => {
    if (
      selectedMetric.label !== "STAGES" &&
      new DORAConfigDefinition(props.config.postData.scm_config[selectedMetric.value]).hasError
    ) {
      notification.error({
        message: `Please resolve any errors before continuing`
      });
      return;
    }
    setSelectedMetric(metric);
  };

  const rightPanelComponent =
    selectedMetric.label === "STAGES" ? (
      <VelocityConfigStages {...props} stageName={props.stageName} />
    ) : (
      <DefinitionConfigurations {...props} metric={selectedMetric} />
    );

  return (
    <div className="velocity-config-edit-create-container">
      <VelocityConfigBasicInfo {...props} selectedMetric={selectedMetric} setSelectedMetric={updateSelectedMetric} />
      {rightPanelComponent}
    </div>
  );
};

export default VelocityConfig;
