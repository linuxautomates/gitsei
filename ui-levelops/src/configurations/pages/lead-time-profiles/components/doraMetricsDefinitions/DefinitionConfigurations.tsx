import React, { useMemo } from "react";
import { Checkbox } from "antd";
import { CommaSelectTag } from "shared-resources/containers/generic-form-elements";
import { STARTS_WITH } from "dashboard/constants/constants";
import { get } from "lodash";
import { AntSelect, AntTitle } from "shared-resources/components";
import {
  DEFAULT_METRIC_IDENTIFIERS,
  DEFINITION_PARTIAL_OPTIONS,
  DORA_METRIC_CONFIGURABLE_DEFINITIONS
} from "../../helpers/constants";
import "./definitionConfigurations.scss";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { DORAConfigDefinition } from "classes/DORAConfigDefinition";

interface DefinitionConfigurationsProps {
  metric: { label: string; value: string };
  config: RestVelocityConfigs;
  onChange: (key: string, value: any) => void;
}

const DefinitionConfigurations = (props: DefinitionConfigurationsProps) => {
  const { metric, config, onChange } = props;
  const defaultValue = useMemo(
    () =>
      Object.keys(DORA_METRIC_CONFIGURABLE_DEFINITIONS).reduce((acc, key) => {
        return {
          ...acc,
          [key]: {
            checked: true,
            key: STARTS_WITH,
            value: DEFAULT_METRIC_IDENTIFIERS[metric.value]
          }
        };
      }, {}),
    [metric]
  );

  const currentMetricDefinition: DORAConfigDefinition = useMemo(
    () => get(config, [metric.value], defaultValue),
    [config, metric.value]
  );

  const getDefinitionConfigurationValue = (definitionkey: string, key: string) => {
    return get(currentMetricDefinition, [definitionkey, key], "");
  };

  const handleDefinationConfigurationChange = (
    definitionkey: string,
    key: string,
    value: boolean | string | string[]
  ) => {
    const updatedConfig: any = currentMetricDefinition;
    updatedConfig[definitionkey][key] = value;
    onChange(metric.value, updatedConfig);
  };

  return (
    <div className="metric-configurations-container">
      <AntTitle level={4} className="basic-info-container-title">
        {metric.label}
      </AntTitle>
      <div className="metric-configurations">
        <p>
          Detects{" "}
          <span className="highlighted-text">
            {metric.label.toLowerCase() === "releases"
              ? metric.label.replace("RELEASES", "new feature")
              : metric.label.toLowerCase()}
          </span>{" "}
          using <span className="highlighted-text">any</span> of the following:
        </p>
        {Object.keys(DORA_METRIC_CONFIGURABLE_DEFINITIONS).map(key => {
          return (
            <div className="definitions-container">
              <Checkbox
                key={key}
                checked={getDefinitionConfigurationValue(key, "checked")}
                onChange={e => handleDefinationConfigurationChange(key, "checked", e.target.checked)}>
                <div className="definitions">
                  {DORA_METRIC_CONFIGURABLE_DEFINITIONS[key]}{" "}
                  <span>
                    <AntSelect
                      className="key-select"
                      options={DEFINITION_PARTIAL_OPTIONS}
                      value={getDefinitionConfigurationValue(key, "key")}
                      onChange={(value: any) => handleDefinationConfigurationChange(key, "key", value)}
                    />
                  </span>
                </div>
              </Checkbox>
              <CommaSelectTag
                className="definition-comma-select"
                value={getDefinitionConfigurationValue(key, "value")}
                onChange={(value: string) => handleDefinationConfigurationChange(key, "value", value)}
              />
            </div>
          );
        })}
        <p className="tip-text">Tip: separate multiple values with a comma.</p>
        {currentMetricDefinition.hasError ? (
          <p className="error-text">You must have at least one definition for {metric.label.toLowerCase()}</p>
        ) : null}
      </div>
    </div>
  );
};

export default DefinitionConfigurations;
