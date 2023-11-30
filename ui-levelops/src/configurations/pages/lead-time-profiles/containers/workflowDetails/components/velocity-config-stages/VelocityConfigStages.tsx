import React, { useCallback, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import { AntBadge, AntSelect, AntText } from "shared-resources/components";
import "./velocity-config-stages.scss";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  velocityConfigsBaseTemplateSelector,
  VELOCITY_CONFIG_BASIC_TEMPLATE
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { get } from "lodash";
import { Form } from "antd";
import {
  RestDevelopmentStageConfig,
  RestStageConfig,
  VelocityConfigStage,
  WorkflowIntegrationType
} from "classes/RestWorkflowProfile";
import { Integration } from "model/entities/Integration";
import { findIntegrationType } from "helper/integration.helper";
import "./velocity-config-stages.scss";
import { DEFAULT_METRIC_IDENTIFIERS } from "configurations/pages/lead-time-profiles/helpers/constants";
import { v1 as uuid } from "uuid";
import { RearrangeStage } from "configurations/pages/lead-time-profiles/components";
import { disabledSelectedEventTypeOption } from "configurations/pages/lead-time-profiles/helpers/helpers";

interface VelocityConfigStagesProps {
  config: RestDevelopmentStageConfig;
  stageName?: string;
  onChange: (key: string, value: any) => void;
  integration: Integration | null;
  integrationApplication: string | undefined;
  objectKey: "lead_time_for_changes" | "mean_time_to_restore";
  // setSelectedStage: (stage?: RestStageConfig) => void;
  // setSelectedStageType: (type: VelocityConfigStage) => void;
}

const VelocityConfigStages: React.FC<VelocityConfigStagesProps> = props => {
  const { config, onChange, stageName, integration, objectKey, integrationApplication } = props;
  const starting_event_is_commit_created = config.event === "commit_created";
  const [loadingStages, setLoadingStages] = useState<boolean>(false);
  const integrationType = findIntegrationType(integration);

  const [hideFixedStages, setHideFixedStages] = useState(false); // used by fixed stages

  const restBasicTemplateState = useParamSelector(velocityConfigsBaseTemplateSelector, {
    id: VELOCITY_CONFIG_BASIC_TEMPLATE
  });

  const defaultScmValue = useMemo(
    () =>
      objectKey === "lead_time_for_changes" ? DEFAULT_METRIC_IDENTIFIERS.release : DEFAULT_METRIC_IDENTIFIERS.defect,
    [objectKey]
  );

  useEffect(() => {
    if (loadingStages) {
      const data = get(restBasicTemplateState, ["data"], {});
      let fixedStages = data?.fixed_stages;
      if (starting_event_is_commit_created) {
        fixedStages = (data?.fixed_stages || []).filter(
          (stage: RestStageConfig) => get(stage, ["event", "type"], "") !== "SCM_COMMIT_CREATED"
        );
      }
      const mappedStages = fixedStages.map((stage: any) => {
        const hiddenKeys =
          stage.event.type === "SCM_PR_MERGED"
            ? ["source_branch", "tags"]
            : stage.event.type === "SCM_PR_SOURCE_BRANCH"
            ? ["target_branch", "commit_branch", "tags", "labels"]
            : ["target_branch", "commit_branch", "tags", "labels", "source_branch"];
        return new RestStageConfig(
          {
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.FIXED_STAGE,
            cicd_job_id_name_mappings: {}
          },
          defaultScmValue,
          hiddenKeys
        ).json;
      });
      props.onChange("fixed_stages", mappedStages || []);
      setLoadingStages(false);
    }
  }, [starting_event_is_commit_created, loadingStages]);

  const advancedSettingsOption = useMemo(() => {
    return [
      { label: "Ticket Created (Default)", value: "ticket_created" },
      { label: "API Event", value: "api_event" },
      { label: "Commit Created", value: "commit_created" }
    ];
  }, [integrationApplication]);

  const onEventChange = (value?: string) => {
    setLoadingStages(true);
    props.onChange("event", value || "ticket_created");
  };
  const handleAdvancedSettingsChange = useCallback(
    (value: string) => {
      onEventChange(value);
    },
    [props.onChange, config, onEventChange]
  );

  const renderHeader = useMemo(
    () => (
      <div className="flex direction-row align-center">
        <AntText className="stage-title">STAGES</AntText>
        <AntBadge className="stage-count" count={config.all_stages.length} overflowCount={1000} />
      </div>
    ),
    [config]
  );

  const renderStages = useMemo(() => {
    return (
      //COMMENTED CODE IS FOR NEW LEAD TIME & MTTR LIKE BEFORE WE MERGE WITH RETRO FIT PART , SO WHEN WE NEED THIS NEW JUST OPEN THE CODE
      // <div className="stages-container">
      //   {!starting_event_is_commit_created && (
      //     <RearrangeStage
      //       stages={config.pre_development_custom_stages || []}
      //       stageName={stageName}
      //       type={VelocityConfigStage.PRE_DEVELOPMENT_STAGE}
      //       onChange={onChange}
      //       issueManagementSystem={integration?.application}
      //       isApiEvent={config.event === "api_event"}
      //       restConfig={config}
      //       setSelectedStage={setSelectedStage}
      //       setSelectedStageType={setSelectedStageType}
      //     />
      //   )}
      //   <RearrangeStage
      //     stageName={stageName}
      //     stages={config.fixed_stages || []}
      //     type={VelocityConfigStage.FIXED_STAGE}
      //     onChange={onChange}
      //     hasStartingCommitEvent={starting_event_is_commit_created}
      //     isDevelopmentStagesEnabled={config.fixed_stages_enabled}
      //     hideStages={hideFixedStages}
      //     setHideStages={setHideFixedStages}
      //     restConfig={config}
      //     setSelectedStage={setSelectedStage}
      //     setSelectedStageType={setSelectedStageType}
      //   />
      //   <RearrangeStage
      //     stageName={stageName}
      //     stages={config.post_development_custom_stages || []}
      //     type={VelocityConfigStage.POST_DEVELOPMENT_STAGE}
      //     onChange={onChange}
      //     issueManagementSystem={integration?.application}
      //     restConfig={config}
      //     setSelectedStage={setSelectedStage}
      //     setSelectedStageType={setSelectedStageType}
      //   />
      // </div>

      <div className="stages-container">
        {!starting_event_is_commit_created && (
          <RearrangeStage
            stages={config.pre_development_custom_stages || []}
            stageName={stageName}
            type={VelocityConfigStage.PRE_DEVELOPMENT_STAGE}
            onChange={onChange}
            issueManagementSystem={config.issue_management_integrations?.[0]}
            isApiEvent={config.event === "api_event"}
            restStageDora={config}
            disabledSelectedEventTypeOption={disabledSelectedEventTypeOption(config?.json)}
          />
        )}
        <RearrangeStage
          stageName={stageName}
          stages={config.fixed_stages || []}
          type={VelocityConfigStage.FIXED_STAGE}
          onChange={onChange}
          hasStartingCommitEvent={starting_event_is_commit_created}
          isDevelopmentStagesEnabled={config.fixed_stages_enabled}
          hideStages={hideFixedStages}
          setHideStages={setHideFixedStages}
          isFixedStage={true}
          restStageDora={config}
        />
        <RearrangeStage
          stageName={stageName}
          stages={config.post_development_custom_stages || []}
          type={VelocityConfigStage.POST_DEVELOPMENT_STAGE}
          onChange={onChange}
          issueManagementSystem={config.issue_management_integrations?.[0]}
          restStageDora={config}
          disabledSelectedEventTypeOption={disabledSelectedEventTypeOption(config?.json)}
        />
      </div>
    );
  }, [config, stageName, hideFixedStages, onChange, disabledSelectedEventTypeOption]);

  const renderLines = useMemo(() => {
    const lines = [];
    let lastElement;
    const preDevelopmentStages = config.pre_development_custom_stages || [];
    const fixedStageClassName = config.fixed_stages?.length === 0 ? "no-fixed-stage" : "fixed_stages";
    if (preDevelopmentStages.length) {
      const lastEle = preDevelopmentStages[preDevelopmentStages.length - 1];
      lastElement = `add-stage-btn right-${lastEle?.id}`;
    } else {
      lastElement = "add-pre-stage";
    }

    lines.push(["ticket-placeholder", "add-pre-stage"]);
    lines.push([lastElement, fixedStageClassName]);
    lines.push([fixedStageClassName, "add-post-stage"]);

    return lines.map(item => (
      <SteppedLineTo
        from={item[0]}
        to={item[1]}
        toAnchor={item[1] === fixedStageClassName ? "50% 15%" : "center"}
        fromAnchor={item[0] === fixedStageClassName ? "50% 85%" : "center"}
        within="stages-container"
        borderColor="#595959"
        borderStyle="dashed"
        borderWidth={1}
        delay
      />
    ));
  }, [config, hideFixedStages]);

  const renderStartEvent = useMemo(() => {
    return (
      <Form colon={false}>
        <Form.Item label="START EVENT" required>
          <AntSelect
            style={{ width: "200px" }}
            value={config.event}
            options={advancedSettingsOption}
            onChange={handleAdvancedSettingsChange}
          />
        </Form.Item>
      </Form>
    );
  }, [advancedSettingsOption, config.event, onChange]);

  return (
    <div className="velocity-config-stages">
      {renderHeader}
      {renderStartEvent}
      {renderStages}
      {renderLines}
    </div>
  );
};

export default VelocityConfigStages;
