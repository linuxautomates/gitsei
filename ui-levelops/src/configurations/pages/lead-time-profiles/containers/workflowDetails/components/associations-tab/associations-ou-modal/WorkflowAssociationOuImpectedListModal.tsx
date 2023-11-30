import { Divider, Icon } from "antd";
import React, { useMemo } from "react";
import { AntButton, AntModal, AntText } from "shared-resources/components";
import "./WorkflowAssociationOuImpectedModal.styles.scss";

interface WorkflowAssociationOuImpectedListModalProps {
    showWarningModal: boolean;
    handleCancel: () => void;
    impectedOuList: any[];
}

const WorkflowAssociationOuImpectedListModalComponent: React.FC<WorkflowAssociationOuImpectedListModalProps> = ({
    handleCancel,
    showWarningModal,
    impectedOuList
}) => {
    const renderFooter = useMemo(
        () => [
            <AntButton key="save" type="primary" onClick={handleCancel}>
                Close
            </AntButton>
        ],
        [handleCancel]
    );

    return (
      <AntModal
        title="COLLECTIONS"
        centered={true}
        closable={true}
        onCancel={handleCancel}
        className={"ou-list-modal"}
        footer={renderFooter}
        visible={showWarningModal}>
        <div className="content">
          <div className="innerbox">
            {impectedOuList.map((name: any) => (
              <>
                <p>
                  <span className="description">{name}</span>
                </p>
                <Divider />
              </>
            ))}
          </div>
        </div>
      </AntModal>
    );
};

export default WorkflowAssociationOuImpectedListModalComponent;
