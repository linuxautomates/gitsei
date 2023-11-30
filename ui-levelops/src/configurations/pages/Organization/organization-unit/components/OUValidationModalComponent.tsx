import { Icon } from "antd";
import React, { useMemo } from "react";
import { AntButton, AntModal, AntText } from "shared-resources/components";
import "./ouValidationModel.styles.scss";

interface OrgUnitValidationModalProps {
  showValidationModal: boolean;
  warningMessage: string;
  handleCancel: () => void;
  handleProceed: () => void;
}

const OrgUnitValidationModalComponent: React.FC<OrgUnitValidationModalProps> = ({
  handleCancel,
  handleProceed,
  showValidationModal,
  warningMessage
}) => {
  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={handleCancel}>
        Cancel
      </AntButton>,

      <AntButton key="save" type="primary" onClick={handleProceed}>
        Update
      </AntButton>
    ],
    [handleCancel, handleProceed]
  );

  return (
    <AntModal
      title="UPDATE COLLECTION"
      centered={true}
      closable={true}
      onCancel={handleCancel}
      footer={renderFooter}
      visible={showValidationModal}>
      <div className="validation-warning">
        <div className="title">
          <Icon type="warning" />
          Warning
        </div>
        <AntText className="allow-new-line-in-text">{warningMessage}</AntText>
      </div>
    </AntModal>
  );
};

export default OrgUnitValidationModalComponent;
