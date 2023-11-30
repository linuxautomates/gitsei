import React, { useCallback, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import { AntIcon, AntText, AntTooltip, SvgIcon, AntButton } from "shared-resources/components";
import Add from "shared-resources/assets/svg-icons/add.svg";
import "./rearrange-stage.style.scss";
import cx from "classnames";
import { Switch } from "antd";
import VelocityConfigStageItem from "../velocity-config-stages/velocity-config-stage/VelocityConfigStage";
import {
  AcceptanceTimeUnit,
  RestDevelopmentStageConfig,
  RestStageConfig,
  TriggerEventType,
  VelocityConfigStage
} from "classes/RestWorkflowProfile";

interface RearrangeStageProps {
  stages: RestStageConfig[];
  restConfig: RestDevelopmentStageConfig;
  stageName?: string;
  type: VelocityConfigStage;
  onChange: (key: string, val: any) => void;
  hasStartingCommitEvent?: boolean;
  issueManagementSystem?: string;
  isDevelopmentStagesEnabled?: boolean;
  hideStages?: boolean;
  setHideStages?: (value: boolean) => void;
  isApiEvent?: boolean;
  setSelectedStage: (stage?: RestStageConfig) => void;
  setSelectedStageType: (type: VelocityConfigStage) => void;
}

const RearrangeStage: React.FC<RearrangeStageProps> = props => {
  const {
    stages,
    type,
    onChange,
    stageName,
    hasStartingCommitEvent,
    isDevelopmentStagesEnabled,
    hideStages = false,
    setHideStages,
    isApiEvent,
    restConfig,
    setSelectedStage,
    setSelectedStageType
  } = props;

  const [dragId, setDragId] = useState(undefined);
  const isJiraIssueManagement = props.issueManagementSystem === "jira";

  const getStageNo = useCallback(
    (stageId: string) => {
      const stageIndex = restConfig.all_stages.findIndex((rStage: RestStageConfig) => rStage.id === stageId);
      return stageIndex !== -1 ? stageIndex + 1 : 0;
    },
    [restConfig]
  );

  useEffect(() => {
    // using this to not include undefined value
    if (isDevelopmentStagesEnabled === false) {
      setHideStages && setHideStages(true);
    } else if (isDevelopmentStagesEnabled === true) {
      setHideStages && setHideStages(false);
    }
  }, [isDevelopmentStagesEnabled]);

  useEffect(() => {
    const stage = stages.find((stg: RestStageConfig) => stg.name === stageName);
    if (stage) {
      setSelectedStage(stage);
      setSelectedStageType(type);
    }
  }, [stageName]);

  const handleDrag = useCallback((e: any) => {
    setDragId(e.currentTarget.id);
  }, []);

  const handleDrop = useCallback(
    e => {
      const dragStage: any = stages.find((box: RestStageConfig) => box.id === dragId);
      const dropStage: any = stages.find((box: RestStageConfig) => box.id === e.currentTarget.id);

      if (!dragStage || !dropStage || dragStage?.type !== dropStage?.type) {
        setDragId(undefined);
        return;
      }

      const dragBoxOrder = dragStage?.order;
      const dropBoxOrder = dropStage?.order;

      const newStages = stages.map((stage: RestStageConfig) => {
        if (stage.id === dragId) {
          stage.order = dropBoxOrder;
        }
        if (stage.id === e.currentTarget.id) {
          stage.order = dragBoxOrder;
        }
        return stage;
      });

      onChange(
        type,
        newStages.map(stage => stage.json)
      );
      setDragId(undefined);
    },
    [stages, dragId, type]
  );

  const handleAddStage = useCallback(
    (order: number) => {
      return (e: any) => {
        const newStage = new RestStageConfig();
        newStage.id = "new";
        newStage.type = type;
        newStage.order = order;
        newStage.event = {
          type: isJiraIssueManagement ? TriggerEventType.JIRA_STATUS : TriggerEventType.WORKITEM_STATUS,
          values: [],
          params: undefined
        };
        setSelectedStage(newStage);
        setSelectedStageType(type);
      };
    },
    [type, isJiraIssueManagement]
  );

  const handleEditStage = useCallback(
    (id: any) => {
      const stage = stages.find(item => item.id === id);
      if (stage) {
        setSelectedStage(stage);
        setSelectedStageType(type);
      }
    },
    [stages]
  );

  const handleDeleteStage = useCallback(
    (id: any) => {
      const newStages = stages.filter(item => item.id !== id);
      onChange(
        type,
        newStages.map(stage => stage.json)
      );
    },
    [stages, type]
  );

  const sortedStages = useMemo(() => stages.sort((a, b) => a.order - b.order), [stages]);

  const headerActionHeightStyle = useMemo(() => ({ height: "22px" }), []);
  const headerLockIconStyle = useMemo(() => ({ fontSize: "18px", padding: "2px" }), []);

  const fixedHeader = useMemo(
    () => (
      <div className="fixed_stages__header flex justify-space-between">
        <div className="flex direction-column">
          <div className="heading">Development Stages</div>
          <div className="sub-heading">Measure time spent in code development stages.</div>
        </div>
        <div className="flex align-center" style={headerActionHeightStyle}>
          <div>
            {isDevelopmentStagesEnabled ? "Active" : "Disabled"} &nbsp;
            <Switch checked={isDevelopmentStagesEnabled} onChange={value => onChange("fixed_stages_enabled", value)} />
          </div>
          <AntTooltip title="PR review stages cannot be rearranged">
            <AntIcon className="lock-icon ml-30" style={headerLockIconStyle} type="lock" />
          </AntTooltip>
        </div>
      </div>
    ),
    [isDevelopmentStagesEnabled, onChange]
  );

  const renderPlaceholder = useMemo(() => {
    if (type === VelocityConfigStage.FIXED_STAGE && !hasStartingCommitEvent) {
      return fixedHeader;
    } else if (type === VelocityConfigStage.FIXED_STAGE && hasStartingCommitEvent) {
      return (
        <div className="ticket-placeholder has-start-commit">
          <AntIcon type={"pull-request"} className="tickets-icon" />
          <AntText>Commit Created</AntText>
          <div className="arrow" />
          <div className="v-line" />
        </div>
      );
    }

    return (
      <>
        {type === VelocityConfigStage.PRE_DEVELOPMENT_STAGE && !isApiEvent && (
          <div className="ticket-placeholder">
            <SvgIcon icon="tickets" className="tickets-icon" />
            <AntText>Ticket Created</AntText>
            <div className="arrow" />
            <div className="v-line" />
          </div>
        )}
        {isApiEvent && (
          <div className="ticket-placeholder">
            <SvgIcon icon="tickets" className="tickets-icon" />
            <AntText>Api Event</AntText>
            <div className="arrow" />
            <div className="v-line" />
          </div>
        )}
        <div className={`${type === VelocityConfigStage.PRE_DEVELOPMENT_STAGE ? "add-pre-stage" : "add-post-stage"}`}>
          <Add className="add-stage-btn" onClick={handleAddStage(0)} />
        </div>
      </>
    );
  }, [type, hasStartingCommitEvent, isJiraIssueManagement, isDevelopmentStagesEnabled, isApiEvent, fixedHeader]);

  const renderStages = useMemo(() => {
    return sortedStages.map((stage, index) => {
      let fixedStagesEnabled = true;
      if (type === VelocityConfigStage.FIXED_STAGE) {
        fixedStagesEnabled = !!isDevelopmentStagesEnabled;
      }

      return (
        <div className="stage-item">
          <VelocityConfigStageItem
            key={`${index}-${stage.id}`}
            stage={stage}
            onDrag={handleDrag}
            onDrop={handleDrop}
            onEdit={handleEditStage}
            onDelete={handleDeleteStage}
            stageNo={getStageNo(stage.id as string)}
            fixedStagesEnabled={fixedStagesEnabled}
          />
          {type !== VelocityConfigStage.FIXED_STAGE && (
            <Add className={`add-stage-btn right-${stage.id}`} onClick={handleAddStage(stage.order + 1)} />
          )}
        </div>
      );
    });
  }, [, dragId, type, stages, getStageNo, isJiraIssueManagement, isDevelopmentStagesEnabled]);

  const renderLines = useMemo(() => {
    const length = sortedStages.length;
    const lines = [];

    for (let i = 0; i < length; i++) {
      const stage = sortedStages[i];
      if (type === VelocityConfigStage.FIXED_STAGE) {
        if (hasStartingCommitEvent && i === 0) {
          lines.push([`ticket-placeholder has-start-commit`, `velocity-config-stage ${stage.id}`]);
        }
        if (i !== length - 1) {
          const nextStage = sortedStages[i + 1];
          lines.push([`velocity-config-stage ${stage.id}`, `velocity-config-stage ${nextStage.id}`]);
        }
      } else {
        if (i === 0) {
          lines.push([`${stage.isPreStage ? "add-pre-stage" : "add-post-stage"}`, `velocity-config-stage ${stage.id}`]);
        }

        lines.push([`velocity-config-stage ${stage.id}`, `add-stage-btn right-${stage.id}`]);

        if (i !== length - 1) {
          const nextStage = sortedStages[i + 1];
          lines.push([`add-stage-btn right-${stage.id}`, `velocity-config-stage ${nextStage.id}`]);
        }
      }
    }
    return lines.map(item => (
      <SteppedLineTo
        from={item[0]}
        to={item[1]}
        within={cx(
          "new-rearrange-stages-container",
          { [`${type}`]: !(type === VelocityConfigStage.FIXED_STAGE) || stages.length },
          { "no-fixed-stage": type === VelocityConfigStage.FIXED_STAGE && !stages.length }
        )}
        borderColor="#595959"
        borderStyle="dashed"
        borderWidth={1}
        zIndex={4}
        delay
      />
    ));
  }, [hasStartingCommitEvent, stages, type]);

  return (
    <div
      className={cx(
        "new-rearrange-stages-container",
        { [`${type}`]: !(type === VelocityConfigStage.FIXED_STAGE) || stages.length },
        { "no-fixed-stage": type === VelocityConfigStage.FIXED_STAGE && !stages.length }
      )}>
      {renderPlaceholder}
      {!hideStages && renderStages}
      {!hideStages && renderLines}
      {/* {createModal} */}
      {isDevelopmentStagesEnabled === false && (
        <div className="w-100 mb-20">
          <AntButton type="link" onClick={() => setHideStages && setHideStages(!hideStages)}>
            {hideStages ? "View Stages" : "Hide Stages"}
          </AntButton>
        </div>
      )}
    </div>
  );
};

export default RearrangeStage;
