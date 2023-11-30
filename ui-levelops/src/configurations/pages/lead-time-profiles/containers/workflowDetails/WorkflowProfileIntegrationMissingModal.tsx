import React, { useMemo } from "react";
import { AntButton, AntIcon, AntModal, AntText } from "shared-resources/components";
import { useHistory } from "react-router-dom";
import { getIntegrationPage } from "constants/routePaths";
import "./workflowProfileCreateEditNewPage.scss";

const WorkflowProfileIntegrationMissingModal: React.FC = () => {
  const history = useHistory();

  const handleCancel = () => {
    history.goBack();
  };

  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={handleCancel}>
        Cancel
      </AntButton>,

      <AntButton key="save" type="primary" onClick={() => history.push(getIntegrationPage())}>
        Add Integration
      </AntButton>
    ],
    []
  );

  return (
    <AntModal
      title={"NO INTEGRATION FOUND"}
      visible={true}
      centered={true}
      closable={true}
      onCancel={handleCancel}
      className="no-integration-modal"
      footer={renderFooter}>
      <div className="content">
        <AntIcon className="info-icon" type="info-circle" />
        <AntText strong>Alert</AntText>

        <p className="section-description">
          Looks like you do not have any integration in order to setup a DORA profile.
        </p>
        <p>Please add an integration in order to proceed.</p>
      </div>
    </AntModal>
  );
};

export default WorkflowProfileIntegrationMissingModal;
