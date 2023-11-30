import React, { useCallback, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import { v1 as uuid } from "uuid";
import {
  AcceptanceTimeUnit,
  RestVelocityConfigStage,
  TriggerEventType,
  VelocityConfigStage,
  RestVelocityConfigs
} from "classes/RestVelocityConfigs";
import { AntIcon, AntText, AntTooltip, SvgIcon, AntButton } from "shared-resources/components";
import Add from "shared-resources/assets/svg-icons/add.svg";
import { CreateStageModal, VelocityConfigStageItem } from "..";
import "./rearrange-stage.style.scss";
import cx from "classnames";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { velocityConfigsRestGetSelector } from "reduxConfigs/selectors/velocityConfigs.selector";
import { Switch } from "antd";
import { RestDevelopmentStageConfig } from "classes/RestWorkflowProfile";
import { IssueManagementOptions } from "constants/issueManagementOptions";

interface RearrangeStageProps {
  stages: RestVelocityConfigStage[];
  stageName?: string;
  type: VelocityConfigStage;
  onChange: (key: string, val: any) => void;
  hasStartingCommitEvent?: boolean;
  issueManagementSystem?: string;
  isDevelopmentStagesEnabled?: boolean;
  hideStages?: boolean;
  setHideStages?: (value: boolean) => void;
  isFixedStage?: boolean;
  isApiEvent?: boolean;
  restStageDora?: RestDevelopmentStageConfig;
  disabledSelectedEventTypeOption?: string[];
  jira_only?: boolean;
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
    isFixedStage,
    isApiEvent,
    restStageDora,
    disabledSelectedEventTypeOption,
    jira_only
  } = props;

  const [dragId, setDragId] = useState(undefined);
  const [showCreateModel, setShowCreateModel] = useState<any>(false);
  const [selectedStage, setSelectedStage] = useState<any>(undefined);
  const isJiraIssueManagement = props.issueManagementSystem === IssueManagementOptions.JIRA;

  const location = useLocation();
  const configId = useMemo(() => (queryString.parse(location.search).configId as string) || "new", [location]);

  const restConfig: RestVelocityConfigs = useParamSelector(velocityConfigsRestGetSelector, {
    config_id: configId
  });

  const getStageNo = useCallback(
    (stageId: string) => {
      let stageIndex;
      if (restStageDora) {
        stageIndex = restStageDora.all_stages.findIndex(rStage => rStage.id === stageId);
      } else {
        stageIndex = restConfig.all_stages.findIndex((rStage: RestVelocityConfigStage) => rStage.id === stageId);
      }
      return stageIndex !== -1 ? stageIndex + 1 : 0;
    },
    [restConfig, restStageDora]
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
    const stage = stages.find((stg: RestVelocityConfigStage) => stg.name === stageName);
    if (stage) {
      setSelectedStage(stage);
      setShowCreateModel(true);
    }
  }, [stageName]);

  const handleDrag = useCallback((e: any) => {
    setDragId(e.currentTarget.id);
  }, []);

  const handleDrop = useCallback(
    e => {
      const dragStage: any = stages.find((box: RestVelocityConfigStage) => box.id === dragId);
      const dropStage: any = stages.find((box: RestVelocityConfigStage) => box.id === e.currentTarget.id);

      if (!dragStage || !dropStage || dragStage?.type !== dropStage?.type) {
        setDragId(undefined);
        return;
      }

      const dragBoxOrder = dragStage?.order;
      const dropBoxOrder = dropStage?.order;

      const newStages = stages.map((stage: RestVelocityConfigStage) => {
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

  const handleCreateModalClose = useCallback(() => {
    setShowCreateModel(false);
  }, []);

  const handleAddStage = useCallback(
    (order: number) => {
      return (e: any) => {
        const newStage = new RestVelocityConfigStage();
        newStage.id = "new";
        newStage.type = type;
        newStage.order = order;
        newStage.event = {
          type: isJiraIssueManagement ? TriggerEventType.JIRA_STATUS : TriggerEventType.WORKITEM_STATUS,
          values: [],
          params: undefined
        };
        setSelectedStage(newStage);
        setShowCreateModel(true);
      };
    },
    [type, isJiraIssueManagement]
  );

  const handleEditStage = useCallback(
    (id: any) => {
      const stage = stages.find(item => item.id === id);
      if (stage) {
        setSelectedStage(stage);
        setShowCreateModel(true);
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

  const handleSaveStage = useCallback(
    (stageData: any) => {
      let newStages = stages ? [...stages] : [];
      const isNew = selectedStage.id === "new";
      const unit = stageData?.lower_limit_unit;
      const order = stageData?.order;

      if (
        stageData?.event?.params &&
        typeof stageData?.event?.params === "object" &&
        Object.keys(stageData?.event?.params).length > 0
      ) {
        let paramaNewObject = {};
        Object.keys(stageData?.event?.params).map(data => {
          if (data.trim()) paramaNewObject = { ...paramaNewObject, [data]: stageData?.event?.params[data] };
        });

        stageData = {
          ...stageData,
          event: {
            ...stageData.event,
            params: paramaNewObject
          }
        };
      }

      const updatedData = {
        ...stageData,
        lower_limit_value:
          unit === AcceptanceTimeUnit.MINUTES ? stageData.lower_limit_value * 60 : stageData.lower_limit_value,
        upper_limit_value:
          unit === AcceptanceTimeUnit.MINUTES ? stageData.upper_limit_value * 60 : stageData.upper_limit_value
      };

      if (isNew) {
        newStages.forEach((stage, index) => {
          if (stage.order >= order) {
            stage.order = stage.order + 1;
            newStages[index] = stage;
          }
        });

        newStages.push(
          new RestVelocityConfigStage({
            ...updatedData,
            id: uuid()
          })
        );
      } else {
        newStages = newStages.filter(stage => stage.id !== selectedStage.id);
        newStages.push(new RestVelocityConfigStage(updatedData));
      }
      onChange(
        type,
        newStages.map(stage => stage.json)
      );
      setSelectedStage(undefined);
      setShowCreateModel(false);
    },
    [stages, selectedStage, type]
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
          {type !== VelocityConfigStage.FIXED_STAGE && stage.event?.type !== TriggerEventType.JIRA_RELEASE && (
            <Add className={`add-stage-btn right-${stage.id}`} onClick={handleAddStage(stage.order + 1)} />
          )}
        </div>
      );
    });
  }, [
    [selectedStage?.event?.params?.target_branches],
    dragId,
    type,
    stages,
    getStageNo,
    isJiraIssueManagement,
    isDevelopmentStagesEnabled
  ]);

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
          "rearrange-stages-container",
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

  const createModal = useMemo(() => {
    if (!showCreateModel) return null;

    return (
      <CreateStageModal
        onCancel={handleCreateModalClose}
        stage={selectedStage}
        onSave={handleSaveStage}
        issueManagementSystem={props.issueManagementSystem}
        isFixedStage={isFixedStage}
        disabledSelectedEventTypeOption={disabledSelectedEventTypeOption}
        jira_only={jira_only}
      />
    );
  }, [showCreateModel, stages, selectedStage]);

  return (
    <div
      className={cx(
        "rearrange-stages-container",
        { [`${type}`]: !(type === VelocityConfigStage.FIXED_STAGE) || stages.length },
        { "no-fixed-stage": type === VelocityConfigStage.FIXED_STAGE && !stages.length }
      )}>
      {renderPlaceholder}
      {!hideStages && renderStages}
      {!hideStages && renderLines}
      {createModal}
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
