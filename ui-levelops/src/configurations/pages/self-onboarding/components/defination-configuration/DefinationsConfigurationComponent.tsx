import { Checkbox, Icon, Result } from "antd";
import { CommaSelectTag } from "shared-resources/containers/generic-form-elements";
import { STARTS_WITH } from "dashboard/graph-filters/components/tag-select/TagSelect";
import { cloneDeep, forEach, get, map, set } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntButton, AntSelect } from "shared-resources/components";
import {
  DEFINITION_PARTIAL_OPTIONS,
  DORA_METRIC_CONFIGURABLE_DEFINITIONS,
  DORA_METRIC_DEFINITIONS,
  SelfOnboardingFormFields
} from "../../constants";
import { getDefaultMetricDefinitionState } from "../../helpers/getDefaultDefinitionState";
import { DefinitionConfigType } from "../../types/integration-step-components-types";
import "./definitionConfiguarationStyles.scss";

interface DefinationsConfigurationProps {
  getFromSelfOnboardingForm: (key: string) => any;
  updateSelfOnboardingForm: (key: string, value: any) => void;
}

const DefinationsConfigurationComponent: React.FC<DefinationsConfigurationProps> = (
  props: DefinationsConfigurationProps
) => {
  const { getFromSelfOnboardingForm, updateSelfOnboardingForm } = props;
  const [currentMetric, setCurrentMetric] = useState<number>(0);
  const [error, setShowError] = useState<boolean>(false);

  const metricDefinitionConfig = useMemo(() => {
    return getFromSelfOnboardingForm(SelfOnboardingFormFields.DEFINITION_CONFIGURATIONS) ?? {};
  }, [getFromSelfOnboardingForm]);

  useEffect(() => {
    if (!Object.keys(metricDefinitionConfig).includes(currentMetric.toString())) {
      const defaultConfig: DefinitionConfigType = getDefaultMetricDefinitionState(
        DORA_METRIC_DEFINITIONS[currentMetric].label
      );
      let newDefinitionConfigurations = cloneDeep(metricDefinitionConfig);
      newDefinitionConfigurations[currentMetric.toString()] = defaultConfig;
      updateSelfOnboardingForm(SelfOnboardingFormFields.DEFINITION_CONFIGURATIONS, newDefinitionConfigurations);
    }
  }, [currentMetric, metricDefinitionConfig]);

  const getDefinitionConfigurationValue = (definitionkey: string, key: string, defaultValue: any) => {
    const currentMetricDefinition = get(metricDefinitionConfig, [currentMetric], {});
    return get(currentMetricDefinition, [definitionkey, key], defaultValue);
  };

  const handleBackMetric = useCallback(() => {
    setCurrentMetric(prev => prev - 1);
  }, [currentMetric]);

  const handleNextMetric = useCallback(() => {
    if (currentMetric === 3) {
      updateSelfOnboardingForm("success", true);
      return;
    }
    setCurrentMetric(prev => prev + 1);
  }, [currentMetric, updateSelfOnboardingForm]);

  const isValidDefinitions = (currentMetricDefinition: { [x: string]: DefinitionConfigType }) => {
    let isValid = false;
    forEach(Object.values(currentMetricDefinition), item => {
      isValid = isValid || item.checked;
    });
    return isValid;
  };

  const handleDefinationConfigurationChange = (definitionkey: string, key: string, value: any) => {
    let newDefinitionConfigurations = cloneDeep(metricDefinitionConfig);
    const currentMetricDefinition = get(newDefinitionConfigurations, [currentMetric], {});
    set(currentMetricDefinition, [definitionkey, key], value);
    if (key === "checked") {
      if (!isValidDefinitions(currentMetricDefinition)) {
        setShowError(true);
      } else {
        setShowError(false);
      }
    }
    newDefinitionConfigurations[currentMetric.toString()] = currentMetricDefinition;
    updateSelfOnboardingForm(SelfOnboardingFormFields.DEFINITION_CONFIGURATIONS, newDefinitionConfigurations);
  };

  return (
    <div className="definition-configuration-container">
      <div className="metric-switches">
        {map(DORA_METRIC_DEFINITIONS, (item: { label: string; value: string }, index: number) => {
          return (
            <div className="metric-switch">
              <p style={{ fontWeight: currentMetric === index ? "bold" : "normal" }}>{item.label}</p>
              <Icon type="edit" />
            </div>
          );
        })}
      </div>
      {getFromSelfOnboardingForm("success") ? (
        <Result
          status="success"
          title="Success!"
          subTitle="These definitions will be saved in Settings/Workflow profiles."
          className="result-success"
        />
      ) : (
        <div className="metric-configurations">
          <p>
            Detects <span className="highlighted-text">{DORA_METRIC_DEFINITIONS[currentMetric].label}</span> using{" "}
            <span className="highlighted-text">any</span> of the following:
          </p>
          {map(Object.keys(DORA_METRIC_CONFIGURABLE_DEFINITIONS), key => {
            return (
              <div className="definitions-container">
                <Checkbox
                  key={key}
                  checked={getDefinitionConfigurationValue(key, "checked", false)}
                  onChange={e => handleDefinationConfigurationChange(key, "checked", e.target.checked)}>
                  <div className="definitions">
                    {DORA_METRIC_CONFIGURABLE_DEFINITIONS[key]}{" "}
                    <span>
                      <AntSelect
                        options={DEFINITION_PARTIAL_OPTIONS}
                        value={getDefinitionConfigurationValue(key, "key", STARTS_WITH)}
                        onChange={(value: string) => handleDefinationConfigurationChange(key, "key", value)}
                      />
                    </span>
                  </div>
                </Checkbox>
                <CommaSelectTag
                  className="definition-comma-select"
                  value={getDefinitionConfigurationValue(key, "value", "")}
                  onChange={(value: string) => handleDefinationConfigurationChange(key, "value", value)}
                />
              </div>
            );
          })}
          <p className="tip-text">Tip: seperate multiple value with comma.</p>
          {error ? (
            <p className="error-text">
              You must select atleast one definition for {DORA_METRIC_DEFINITIONS[currentMetric].label}
            </p>
          ) : null}
          <div className="definition-nav-buttons">
            <AntButton type="default" onClick={handleBackMetric} disabled={currentMetric === 0}>
              Back
            </AntButton>
            <AntButton type="primary" onClick={handleNextMetric} disabled={error}>
              Next
            </AntButton>
          </div>
        </div>
      )}
    </div>
  );
};

export default DefinationsConfigurationComponent;
