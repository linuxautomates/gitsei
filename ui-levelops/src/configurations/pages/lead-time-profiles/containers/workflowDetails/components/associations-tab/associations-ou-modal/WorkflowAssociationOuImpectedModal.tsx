import { Icon } from "antd";
import React, { useMemo } from "react";
import { AntButton, AntModal, AntText } from "shared-resources/components";
import { ORG_UNIT_IMPACTED, ORG_UNIT_IMPACTED_BUTTON } from "../../constant";
import "./WorkflowAssociationOuImpectedModal.styles.scss";

interface WorkflowAssociationOuImpectedModalProps {
    showWarningModal: boolean;
    handleCancel: () => void;
    handleProceed: () => void;
}

const WorkflowAssociationOuImpectedModalComponent: React.FC<WorkflowAssociationOuImpectedModalProps> = ({
    handleCancel,
    handleProceed,
    showWarningModal,
}) => {
    const renderFooter = useMemo(
        () => [
            <AntButton key="save" type="primary" onClick={handleProceed}>
                Ok
            </AntButton>
        ],
        [handleCancel, handleProceed]
    );

    return (
      <AntModal
        title="COLLECTIONS IMPACTED"
        centered={true}
        closable={true}
        onCancel={handleCancel}
        footer={renderFooter}
        visible={showWarningModal}>
        <div className="validation-warning">
          <span className="warning-text">{ORG_UNIT_IMPACTED}</span>
          <br />
          <br />
          <span className="warning-text">{ORG_UNIT_IMPACTED_BUTTON}</span>
        </div>
      </AntModal>
    );
};

export default WorkflowAssociationOuImpectedModalComponent;
