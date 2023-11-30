import { Steps } from "antd";
import { DORAConfigDefinition } from "classes/DORAConfigDefinition";
import { DORAConfigValues } from "classes/DORAConfigValues";
import { getSCMRemoveFilters, SCMFilter } from "classes/RestWorkflowProfile";
import {
  DEFAULT_METRIC_IDENTIFIERS,
  DORA_SCM_DEFINITIONS
} from "configurations/pages/lead-time-profiles/helpers/constants";
import { get, cloneDeep } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { AntSelect, AntText } from "shared-resources/components";
import classNames from "classnames";
import CalculationCriteria from "../calculation-criteria/CalculationCriteria";
import { SCM_COMMON_NOTE, WORKFLOW_PROFILE_TABS } from "../constant";
import DeploymentSelector from "./DeploymentSelector";
import { SCMCommitCalculationFields, SCMPRCalculationFields } from "./helper";
import ScmConfig from "./ScmConfig";
import "./ScmDefinition.scss";

interface ScmDefinitionProps {
  filterKey: string;
  config: SCMFilter;
  onChange: (filter: SCMFilter) => void;
  calculationType: string;
}

const DEFAULT_SCM_VALUE = {
  [WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB]: DEFAULT_METRIC_IDENTIFIERS.release,
  [WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB]: DEFAULT_METRIC_IDENTIFIERS.release,
  [WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB]: DEFAULT_METRIC_IDENTIFIERS.hotfix
};

const ScmDefinition: React.FC<ScmDefinitionProps> = ({ filterKey, config, onChange, calculationType }) => {
  const [removeKeys, setRemoveKeys] = useState<string[]>(["tags", "commit_branch"]);
  const visibleKeys = Object.keys(DORA_SCM_DEFINITIONS).filter(key => !(removeKeys || []).includes(key));

  useEffect(() => {
    const removeFilters = getSCMRemoveFilters(config);
    setRemoveKeys(removeFilters);
  }, [config.deployment_route, config.deployment_criteria]);

  const setcalculationRoute = (calculationRoute: string) => {
    if (calculationRoute === "pr") {
      const hiddenKeys = ["tags", "commit_branch"];
      // @ts-ignore
      const scmFilter = new DORAConfigDefinition(null, DEFAULT_SCM_VALUE[calculationType], hiddenKeys);
      onChange({
        ...config,
        deployment_route: calculationRoute,
        calculation_field: "pr_merged_at",
        deployment_criteria: "pr_merged",
        scm_filters: scmFilter
      });
    } else {
      const hiddenKeys = ["source_branch", "target_branch", "labels", "tags"];
      // @ts-ignore
      const scmFilter = new DORAConfigDefinition(null, DEFAULT_SCM_VALUE[calculationType], hiddenKeys, [
        "commit_branch"
      ]);
      onChange({
        ...config,
        deployment_route: "commit",
        calculation_field: "commit_pushed_at",
        deployment_criteria: "commit_merged_to_branch",
        scm_filters: scmFilter
      });
    }
  };

  const setcalculationCriteria = (
    calculationCriteria:
      | "pr_merged"
      | "pr_closed"
      | "pr_merged_closed"
      | "commit_merged_to_branch"
      | "commit_with_tag"
      | "commit_merged_to_branch_with_tag"
  ) => {
    switch (calculationCriteria) {
      case "pr_closed":
        onChange({
          ...config,
          deployment_criteria: calculationCriteria,
          calculation_field: "pr_closed_at"
        });
        break;
      case "pr_merged":
      case "pr_merged_closed":
        onChange({
          ...config,
          deployment_criteria: calculationCriteria,
          calculation_field: "pr_merged_at"
        });
        break;
      case "commit_merged_to_branch_with_tag":
        {
          const hiddenKeys = ["source_branch", "target_branch", "labels"];
          setRemoveKeys(hiddenKeys);
          // @ts-ignore
          const scmFilter = new DORAConfigDefinition(null, DEFAULT_SCM_VALUE[calculationType], hiddenKeys, [
            "commit_branch",
            "tags"
          ]);
          onChange({
            ...config,
            deployment_criteria: calculationCriteria,
            calculation_field: "commit_pushed_at",
            scm_filters: scmFilter
          });
        }
        break;
      case "commit_merged_to_branch":
        {
          const hiddenKeys = ["source_branch", "target_branch", "labels", "tags"];
          setRemoveKeys(hiddenKeys);
          // @ts-ignore
          const scmFilter = new DORAConfigDefinition(null, DEFAULT_SCM_VALUE[calculationType], hiddenKeys, [
            "commit_branch"
          ]);
          onChange({
            ...config,
            deployment_criteria: calculationCriteria,
            calculation_field: "commit_pushed_at",
            scm_filters: scmFilter
          });
        }
        break;
      case "commit_with_tag":
        {
          const hiddenKeys = ["source_branch", "commit_branch", "labels", "target_branch"];
          setRemoveKeys(hiddenKeys);
          // @ts-ignore
          const scmFilter = new DORAConfigDefinition(null, DEFAULT_SCM_VALUE[calculationType], hiddenKeys, ["tags"]);
          onChange({
            ...config,
            deployment_criteria: calculationCriteria,
            calculation_field: "committed_at",
            scm_filters: scmFilter
          });
        }
        break;
      default:
        onChange({
          ...config,
          deployment_criteria: calculationCriteria
        });
    }
  };

  const setCalculationField = (
    calculationField: "pr_merged_at" | "pr_closed_at" | "commit_pushed_at" | "committed_at" | "tag_added_at"
  ) => {
    onChange({
      ...config,
      calculation_field: calculationField
    });
  };

  const handleDefinationConfigurationChange = (key: string, newValue: DORAConfigValues) => {
    const updatedConfig: any = cloneDeep(config.scm_filters);
    updatedConfig[key] = newValue;
    onChange({
      ...config,
      scm_filters: updatedConfig
    });
  };

  const getDefinitionConfigurationValue = (definitionkey: string) =>
    get(config.scm_filters, [definitionkey], {
      checked: false,
      key: "",
      value: ""
    });

  const step1 = useMemo(
    () => (
      <Steps.Step
        title={<AntText strong>What activity do you use to define a deployment?</AntText>}
        description={
          <DeploymentSelector calculationRoute={config.deployment_route} setCalculationRoute={setcalculationRoute} />
        }
      />
    ),
    [config.deployment_route, setcalculationRoute]
  );

  const step2 = useMemo(() => {
    return (
      <Steps.Step
        title={
          <AntText strong>
            {`Based on the above selection, a ${filterKey} is when`}
            <CalculationCriteria
              calculationRoute={config.deployment_route}
              onChange={setcalculationCriteria as any}
              value={config.deployment_criteria}
            />
          </AntText>
        }
        description={
          ["commit_merged_to_branch_with_tag", "pr_merged_closed"].includes(config.deployment_criteria || "") && (
            <AntText strong>
              and it is calculated based on the
              <AntSelect
                style={{ width: "160px" }}
                value={config.calculation_field}
                options={config.deployment_route === "pr" ? SCMPRCalculationFields : SCMCommitCalculationFields}
                onChange={setCalculationField}
              />
              in the selected time range on the insight.
            </AntText>
          )
        }
      />
    );
  }, [config, setCalculationField, setcalculationCriteria]);

  const step3 = useMemo(
    () => (
      <Steps.Step
        title={
          <AntText
            className={classNames({ required: config.deployment_route === "pr" })}
            strong>{`Please add any additional attributes that can help to identify the ${config.deployment_route}s for ${filterKey}.`}</AntText>
        }
        description={
          <div>
            <AntText className="note-text">{`Note: ${SCM_COMMON_NOTE}`}</AntText>
            <div className="scm-configurations">
              {visibleKeys.map(key => (
                <ScmConfig
                  type={key}
                  config={getDefinitionConfigurationValue(key)}
                  onChange={(value: DORAConfigValues) => handleDefinationConfigurationChange(key, value)}
                  customDefinition={DORA_SCM_DEFINITIONS}
                  isRequired={config.deployment_route === "commit"}
                />
              ))}
              {config.scm_filters?.hasError && config.deployment_route === "pr" ? (
                <p className="error-text">You must have at least one definition</p>
              ) : null}
            </div>
          </div>
        }
      />
    ),
    [
      config.deployment_route,
      filterKey,
      visibleKeys,
      getDefinitionConfigurationValue,
      handleDefinationConfigurationChange
    ]
  );

  return (
    <Steps direction="vertical" current={-1} className="scm-definition">
      {step1}
      {step2}
      {step3}
    </Steps>
  );
};

export default ScmDefinition;
