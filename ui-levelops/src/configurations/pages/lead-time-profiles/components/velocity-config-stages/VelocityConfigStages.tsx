import React, { useCallback, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import {
  RestVelocityConfigs,
  RestVelocityConfigStage,
  TriggerEventType,
  VelocityConfigStage
} from "classes/RestVelocityConfigs";
import { AntBadge, AntSelect, AntText, Label } from "shared-resources/components";
import { RearrangeStage } from "../index";
import "./velocity-config-stages.scss";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  velocityConfigsBaseTemplateSelector,
  VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { velocityConfigsBasicTemplateGet } from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import { Form, Tooltip } from "antd";
import { disabledSelectedEventTypeOption } from "../../helpers/helpers";
import { JIRA_RELEASE_EVENT_DISABLED_MESSAGE } from "../../helpers/constants";

interface VelocityConfigStagesProps {
  config: RestVelocityConfigs;
  stageName?: string;
  onChange: (key: string, value: any) => void;
}

const VelocityConfigStages: React.FC<VelocityConfigStagesProps> = props => {
  const { config, onChange, stageName } = props;

  const [hideFixedStages, setHideFixedStages] = useState(false); // used by fixed stages
  const [loadingStages, setLoadingStages] = useState<boolean>(false);
  const dispatch = useDispatch();

  const restBasicTemplateState = useParamSelector(velocityConfigsBaseTemplateSelector, {
    id: VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE
  });

  useEffect(() => {
    if (loadingStages) {
      const loading = get(restBasicTemplateState, ["loading"], true);
      const error = get(restBasicTemplateState, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(restBasicTemplateState, ["data"], {});
          if (config.starting_event_is_commit_created) {
            const fixed_stages = (data?.fixed_stages || []).filter(
              (stage: any) => get(stage, ["event", "type"], "") !== "SCM_COMMIT_CREATED"
            );
            props.onChange("fixed_stages", fixed_stages || []);
          } else {
            props.onChange("fixed_stages", data?.fixed_stages || []);
          }
        }
        setLoadingStages(false);
      }
    }
  }, [restBasicTemplateState]);

  useEffect(() => {
    if (config.jira_only && (config.starting_event_is_commit_created || config.starting_event_is_generic_event)) {
      handleAdvancedSettingsChange('ticket_created');
    }
  }, [config.jira_only])

  const advancedSettingsValue = useMemo(() => {
    if (config.starting_event_is_commit_created) {
      return "commit_created";
    }
    if (!config.starting_event_is_commit_created && !config.starting_event_is_generic_event) {
      return "ticket_created";
    }
    if (config.starting_event_is_generic_event) {
      return "api_event";
    }
    return "ticket_created";
  }, [config.starting_event_is_commit_created, config.starting_event_is_generic_event]);

  const advancedSettingsOption = useMemo(
    () => [
      { label: "Ticket Created (Default)", value: "ticket_created" },
      { label: "Commit Created", value: "commit_created" },
      { label: "API Event", value: "api_event" }
    ],
    []
  );

  const handleAdvancedSettingsChange = useCallback(
    (value: string) => {
      setLoadingStages(true);
      dispatch(velocityConfigsBasicTemplateGet(VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE));
      if (value === "commit_created") {
        props.onChange("starting_event_is_commit_created", true);
        props.onChange("starting_event_is_generic_event", false);
        !config.fixed_stages_enabled && props.onChange("fixed_stages_enabled", true);
      } else if (value === "ticket_created") {
        props.onChange("starting_event_is_commit_created", false);
        props.onChange("starting_event_is_generic_event", false);
      } else {
        props.onChange("starting_event_is_commit_created", false);
        props.onChange("starting_event_is_generic_event", true);
      }
    },
    [props.onChange, config]
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
      <div className="stages-container">
        {!config.starting_event_is_commit_created && (
          <RearrangeStage
            stages={config.pre_development_custom_stages || []}
            stageName={stageName}
            type={VelocityConfigStage.PRE_DEVELOPMENT_STAGE}
            onChange={onChange}
            issueManagementSystem={config.issue_management_integrations?.[0]}
            isApiEvent={config.starting_event_is_generic_event}
            disabledSelectedEventTypeOption={disabledSelectedEventTypeOption(config?.json)}
            jira_only={config?.jira_only}
          />
        )}
        {!config.jira_only &&
          <>
            <RearrangeStage
              stageName={stageName}
              stages={config.fixed_stages || []}
              type={VelocityConfigStage.FIXED_STAGE}
              onChange={onChange}
              hasStartingCommitEvent={config.starting_event_is_commit_created}
              isDevelopmentStagesEnabled={config.fixed_stages_enabled}
              hideStages={hideFixedStages}
              setHideStages={setHideFixedStages}
              isFixedStage={true}
            />
            <RearrangeStage
              stageName={stageName}
              stages={config.post_development_custom_stages || []}
              type={VelocityConfigStage.POST_DEVELOPMENT_STAGE}
              onChange={onChange}
              issueManagementSystem={config.issue_management_integrations?.[0]}
              disabledSelectedEventTypeOption={disabledSelectedEventTypeOption(config?.json)}
            />
          </>
        }
      </div>
    );
  }, [config, stageName, hideFixedStages, disabledSelectedEventTypeOption]);

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

  const renderStartEvent = (
    <Form colon={false}>
      <Form.Item label="START EVENT">
      <Tooltip title={config.jira_only  ? JIRA_RELEASE_EVENT_DISABLED_MESSAGE : ""}>
        <AntSelect
          style={{ width: "200px" }}
          value={advancedSettingsValue}
          options={advancedSettingsOption}
          onChange={handleAdvancedSettingsChange}
          disabled={config.jira_only ? true : false}
        />
        </Tooltip>
      </Form.Item>
    </Form>
  );

  return (
    <div className="velocity-config-stages-container">
      {renderHeader}
      {renderStartEvent}
      {renderStages}
      {renderLines}
    </div>
  );
};

export default VelocityConfigStages;
