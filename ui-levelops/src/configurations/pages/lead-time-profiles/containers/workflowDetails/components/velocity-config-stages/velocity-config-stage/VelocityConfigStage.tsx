import React, { useCallback, useMemo, useState } from "react";
import { AntIcon, AntText, AntTooltip, ConfirmationModal } from "shared-resources/components";
import exclamation from "assets/svg/exclamation.svg";
import cx from "classnames";
import { Tooltip } from "antd";
import { AcceptanceTimeUnit, RestStageConfig } from "classes/RestWorkflowProfile";

interface VelocityConfigStageProps {
  stage: RestStageConfig;
  onDrag: (e: any) => void;
  onDrop: (e: any) => void;
  onEdit: (id: any) => void;
  onDelete: (id: any) => void;
  stageNo: number;
  fixedStagesEnabled?: boolean;
}

const VelocityConfigStageItem: React.FC<VelocityConfigStageProps> = props => {
  const { stage, onDrag, onDrop, onEdit, onDelete, stageNo, fixedStagesEnabled = true } = props;

  const [showConfirmationModal, setShowConfirmationModal] = useState<boolean>(false);

  const enabled = fixedStagesEnabled && stage.enabled;

  const target_value = useMemo(() => {
    let value = stage?.lower_limit_value || 0;
    let unit: any = stage?.lower_limit_unit || AcceptanceTimeUnit.DAYS;

    switch (unit) {
      case AcceptanceTimeUnit.SECONDS:
        if (value > 86400) {
          value = Math.trunc(value / 86400);
          unit = "Days";
        } else {
          value = Math.trunc(value / 3600);
          unit = "Hours";
        }
        break;
      case AcceptanceTimeUnit.MINUTES:
        value = Math.trunc(value / 60);
        unit = "Hours";
        break;
    }

    return `${value} ${(unit || "")?.toLowerCase()}`;
  }, [stage]);

  const handleDragOver = useCallback(e => e.preventDefault(), []);

  const handleEdit = useCallback(() => {
    (enabled || !stage.enabled) && onEdit(stage.id);
  }, [stage, enabled]);

  const handleDelete = useCallback(
    event => {
      event.stopPropagation();
      setShowConfirmationModal(true);
    },
    [stage]
  );

  const handleConfirm = useCallback(() => {
    onDelete(stage.id);
    setShowConfirmationModal(false);
  }, [stage]);

  const handleCancel = useCallback(() => {
    setShowConfirmationModal(false);
  }, []);

  const renderContent = useMemo(
    () => (
      <div className="stage-content">
        <AntText className="stage-name">{`${stageNo} - ${stage.name}`}</AntText>
        <AntText className="stage-target">{`Target Value - ${target_value}`}</AntText>
      </div>
    ),
    [stage, stageNo, stage.event?.params]
  );

  const renderActions = useMemo(
    () => (
      <div className="stage-actions">
        {stage.isFixedStage ? (
          <>
            <AntIcon type="edit" onClick={handleEdit} style={{ cursor: !enabled ? "default" : "pointer" }} />
            {!enabled && <AntIcon type="eye-invisible" />}
          </>
        ) : (
          <>
            <AntTooltip title={stage.description}>
              <AntIcon className="info-icon" type="info-circle" />
            </AntTooltip>
            <AntIcon type="edit" onClick={handleEdit} style={{ cursor: !enabled ? "default" : "pointer" }} />
            <AntIcon type="delete" onClick={handleDelete} />
          </>
        )}
      </div>
    ),
    [stage]
  );

  const confirmationModal = useMemo(() => {
    if (!showConfirmationModal) return null;
    return (
      <ConfirmationModal
        visiblity={showConfirmationModal}
        text={"Do you want to delete this stage?"}
        onCancel={handleCancel}
        onOk={handleConfirm}
      />
    );
  }, [stage, showConfirmationModal]);

  return (
    <>
      <div
        id={stage.id}
        className={cx(
          `velocity-config-stage ${stage.id}`,
          {
            "error-stage": !stage.isFixedStage && !stage.event?.values?.length
          },
          { "velocity-config-stage-disabled": !enabled },
          { "velocity-config-action-icons-disabled": !fixedStagesEnabled }
        )}
        draggable={!stage.isFixedStage}
        onClick={handleEdit}
        onDragOver={handleDragOver}
        onDragStart={onDrag}
        onDrop={onDrop}>
        {renderContent}
        {renderActions}
      </div>
      {confirmationModal}
    </>
  );
};

export default VelocityConfigStageItem;
