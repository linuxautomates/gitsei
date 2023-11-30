import { Checkbox, Icon } from "antd";
import { DORAConfigValues } from "classes/DORAConfigValues";
import { CommaSelectTag } from "shared-resources/containers/generic-form-elements";
import {
  DEFINITION_PARTIAL_OPTIONS,
  DORA_METRIC_CONFIGURABLE_DEFINITIONS
} from "configurations/pages/lead-time-profiles/helpers/constants";
import { cloneDeep } from "lodash";
import React from "react";
import { AntSelect, AntText } from "shared-resources/components";
import classnames from "classnames";
import "./scmConfig.scss";

interface ScmConfigProps {
  type: string;
  config: DORAConfigValues;
  onChange: (updatedConfig: DORAConfigValues) => void;
  hideCheckBox?: boolean;
  customDefinition?: Record<string, string>;
  isRequired?: boolean;
}

const ScmConfig = (props: ScmConfigProps) => {
  const { type, config, onChange, hideCheckBox, customDefinition, isRequired } = props;
  const configureDefinition = customDefinition || DORA_METRIC_CONFIGURABLE_DEFINITIONS;
  const handleDefinationConfigurationChange = (key: string, value: boolean | string | string[]) => {
    const updatedConfig: any = cloneDeep(config);
    updatedConfig[key] = value;
    onChange(updatedConfig);
  };
  const hasError = isRequired && !(config.checked && config.value);
  return (
    <div className="scm-definitions-container">
      {hideCheckBox ? (
        <div className="no-checkbox-definitions">{configureDefinition[type]} </div>
      ) : (
        <Checkbox
          key={type}
          checked={config.checked}
          disabled={isRequired}
          onChange={e => handleDefinationConfigurationChange("checked", e.target.checked)}>
          <div className={classnames("definitions", { required: isRequired })}>{configureDefinition[type]} </div>
        </Checkbox>
      )}
      <AntSelect
        className="key-select"
        options={DEFINITION_PARTIAL_OPTIONS}
        value={config.key}
        onChange={(value: any) => handleDefinationConfigurationChange("key", value)}
      />
      <CommaSelectTag
        className={hasError ? "definition-comma-select-error" : "definition-comma-select"}
        value={config.value}
        onChange={(value: string) => handleDefinationConfigurationChange("value", value)}
      />
      {hasError && (
        <div className="flex align-items-center ml-5">
          <Icon type="exclamation-circle" theme="filled" style={{ color: "#EE5F54" }} />
          <AntText type="danger" className="m-0 ml-10">
            This is a required field
          </AntText>
        </div>
      )}
    </div>
  );
};

export default ScmConfig;
